package edu.gemini.ags.gems

import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.catalog.api.MagnitudeConstraints
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gems.CanopusWfs
import edu.gemini.spModel.guide.GuideStarValidation.VALID
import edu.gemini.spModel.guide.PatrolField
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs1

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

final case class GemsCandidates(
  posAngle:        Angle,
  cwfsCandidates:  List[SiderealTarget],
  slowFocusSensor: SiderealTarget
) {

  def cwfsCandidatesAsJava: java.util.List[SiderealTarget] =
    cwfsCandidates.asJava

}

object GemsCandidates {

  def fromJava(
    posAngle:        Angle,
    cwfsCandidates:  java.util.List[SiderealTarget],
    slowFocusSensor: SiderealTarget
  ): GemsCandidates =
    GemsCandidates(posAngle, cwfsCandidates.asScala.toList, slowFocusSensor)

  def groupAndValidate(
    obsContext:   ObsContext,
    posAngles:    Set[Angle],
    candidates:   List[SiderealTarget]
  ): List[GemsCandidates] = {

    val tt = groupAndValidateTiptilt(obsContext, posAngles, candidates)
    val sf = selectSlowFocus(obsContext, posAngles, candidates)

    // TODO-NGS: less convoluted way to do this?

    // For each set of CWFS candidates, group with the best SFS option (if any)
    // and the angle required to reach it.
    tt.toList.flatMap { case (as, cwfsCandidates) =>

      // For each angle that can be used for the CWFS candidates, find the
      // matching SFS option (if any)
      val slowFocusOptions: List[(Angle, SiderealTarget)] =
        as.toList.flatMap(a => sf.get(a).strengthL(a).toList)

      // Find the angle and target for the brightest valid option.
      val selectedSlowFocus: Option[(Angle, SiderealTarget)] =
        slowFocusOptions.reduceOption { (at0, at1) =>
          val ((a0, t0), (a1, t1)) = (at0, at1)
          if (ops.brighter(t0, t1)) (a0, t0) else (a1, t1)
        }

      selectedSlowFocus
        .map { case (a, s) => GemsCandidates(a, cwfsCandidates, s) }
        .toList
    }
  }

  private object ops {
    // Observation contexts at each of the position angles.  Position angles
    // matter even though the "range" is circular because the same star has to
    // be reachable by a probe at all offset positions.
    def posAngleContexts(
      obsContext: ObsContext,
      posAngles: Set[Angle]
    ): List[ObsContext] =
      posAngles.toList.map(obsContext.withPositionAngle)

    // Finds the target with the minimum R-band mag in a possibly empty list.
    // (want a minByOption(f))
    def findMinMag(ts: List[SiderealTarget]): Option[SiderealTarget] =
      ts match {
        case Nil => None
        case _ => Some(ts.minBy(RBandsList.extract)(Magnitude.MagnitudeOptionValueOrdering))
      }

    // Filter candidates by magnitude constraints.
    def magConstraintsFilter(c: MagnitudeConstraints): SiderealTarget => Boolean =
      t => c.searchBands.extract(t).exists(c.contains)

    def brighter(t0: SiderealTarget, t1: SiderealTarget): Boolean =
      Magnitude.MagnitudeOptionValueOrdering.lt(
        RBandsList.extract(t0),
        RBandsList.extract(t1)
      )

  }

  private def groupAndValidateTiptilt(
    obsContext: ObsContext,
    posAngles:  Set[Angle],
    candidates: List[SiderealTarget]
  ): Map[Set[Angle], List[SiderealTarget]] = {

    val magc  = GemsVoTableCatalog.cwfsMagnitudeConstraints(obsContext)
    val valid = candidates.filter(ops.magConstraintsFilter(magc))
    val when  = obsContext.getSchedulingBlockStart.asScalaOpt
    val ctxs  = ops.posAngleContexts(obsContext, posAngles)

    def rangeCheck(target: SiderealTarget, ctx: ObsContext): Boolean =
      when.flatMap(target.coords(_))
          .map(edu.gemini.skycalc.Coordinates.fromCoreCoordinates)
          .exists(CanopusWfs.areProbesInRange(_, ctx))

    // Which position angles work for candidate t?
    valid.foldRight(Map.empty[Set[Angle], List[SiderealTarget]]) { (t, m) =>

      val angles = ctxs.foldLeft(Set.empty[Angle]) { (s, ctx) =>
        if (rangeCheck(t, ctx)) s + ctx.getPositionAngle else s
      }

      if (angles.isEmpty) m // not reachable at all
      else m + ((angles, t :: m.getOrElse(angles, List.empty[SiderealTarget])))
    }
  }

  // Selects the best valid slow focus sensor option per position angle.
  private def selectSlowFocus(
    obsContext:   ObsContext,
    posAngles:    Set[Angle],
    candidates:   List[SiderealTarget]
  ): Map[Angle, SiderealTarget] =

    GemsVoTableCatalog
      .pwfsMagnitudeConstraints(obsContext, ProbeLimitsTable.loadOrThrow)
      .fold(Map.empty[Angle, SiderealTarget]) { magc =>

        // Filter to include only candidates that are in the magnitude range for the
        // observation's conditions.
        val magTargets = candidates.filter(ops.magConstraintsFilter(magc))

        // A PWFS1 range validator for each contex (i.e., position angle)
        val ctxs       = ops.posAngleContexts(obsContext, posAngles)
        val validators = ctxs.map { ctx =>
          val min = pwfs1.getVignettingClearance(ctx)
          val pf  = pwfs1.getCorrectedPatrolField(PatrolField.fromRadiusLimits(min, PwfsGuideProbe.PWFS_RADIUS), ctx)
          pf.validator(ctx)
        }

        ctxs.zip(validators).foldLeft(Map.empty[Angle, SiderealTarget]) { case (m, (c, v)) =>
          val valid = magTargets.filter(t => v.validate(new SPTarget(t), c) == VALID)
          ops.findMinMag(valid).fold(m) { t => m + ((c.getPositionAngle, t)) }
        }
      }


}
