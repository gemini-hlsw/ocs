package edu.gemini.ags.impl

import java.awt.geom.AffineTransform

import edu.gemini.ags.api.AgsAnalysis.NoGuideStarForProbe
import edu.gemini.ags.api.{AgsAnalysis, AgsGuideQuality, AgsMagnitude, AgsStrategy, ProbeCandidates}
import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.ags.api.AgsStrategy.{Assignment, Selection}
import edu.gemini.ags.gems.{GemsCandidates, GemsResultsAnalyzer, GemsVoTableCatalog}
import edu.gemini.catalog.api.{CatalogName, CatalogQuery, MagnitudeConstraints}
import edu.gemini.catalog.votable.VoTableBackend
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.ags.AgsStrategyKey.Ngs2Key
import edu.gemini.spModel.core.{Angle, BandsList, RBandsList, SiderealTarget}
import edu.gemini.spModel.gemini.gems.CanopusWfs
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.guide.{GuideProbe, GuideSpeed, PatrolField, ValidatableGuideProbe}
import edu.gemini.spModel.guide.GuideStarValidation.VALID
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs1
import edu.gemini.spModel.telescope.{PosAngleConstraint, PosAngleConstraintAware}
import edu.gemini.pot.ModelConverters._
import edu.gemini.skycalc.{Offset, Angle => JAngle}
import jsky.util

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._
import scalaz._
import Scalaz._
import edu.gemini.ags.gems.GemsCandidates.ops

/**
 * The new NGS2 strategy, which requires Canopus guide stars and a PWFS1 guide
 * star.
 */
final case class Ngs2Strategy(
  catalogName: CatalogName,
  backend:     Option[VoTableBackend]
) extends AgsStrategy {

  override def key: AgsStrategyKey = Ngs2Key

  override def magnitudes(
    ctx: ObsContext,
    mt:  MagnitudeTable
  ): List[(GuideProbe, MagnitudeCalc)] = {

    val cwfsCalc = new MagnitudeCalc {
      def apply(c: Conditions, gs: GuideSpeed): MagnitudeConstraints =
        GemsVoTableCatalog.cwfsMagnitudeConstraints(ctx.withConditions(c))
    }

    val pwfsCalc = new MagnitudeCalc {
      def apply(c: Conditions, gs: GuideSpeed): MagnitudeConstraints =
        GemsVoTableCatalog
          .pwfsMagnitudeConstraints(ctx.withConditions(c), mt)
          .getOrElse(MagnitudeConstraints.unbounded(RBandsList))
    }

    CanopusWfs.values.toList.strengthR(cwfsCalc) ++ List((pwfs1, pwfsCalc))

  }

  // Similar to toplevel AgsAnalysis.analysis but we want to call our version of
  // analyze and not AgsAnalysis.analysis ...
  private def contextAnalysis(
    ctx:        ObsContext,
    mt:         MagnitudeTable,
    guideProbe: ValidatableGuideProbe
  ): Option[AgsAnalysis] = {

    def selection(ctx: ObsContext, guideProbe: GuideProbe): Option[SPTarget] =
     for {
       gpt   <- ctx.getTargets.getPrimaryGuideProbeTargets(guideProbe).asScalaOpt
       gStar <- gpt.getPrimary.asScalaOpt
     } yield gStar

     selection(ctx, guideProbe).fold(Some(NoGuideStarForProbe(guideProbe)): Option[AgsAnalysis]) { guideStar =>
       analyze(ctx, mt, guideProbe, guideStar.toSiderealTarget(ctx.getSchedulingBlockStart))
     }

  }

  override def analyze(ctx: ObsContext, mt: MagnitudeTable): List[AgsAnalysis] = {
    val pwfs = contextAnalysis(ctx, mt, pwfs1).toList

    val cwfs =
      CanopusWfs
        .values
        .toList
        .flatMap(contextAnalysis(ctx, mt, _).toList)
        .filter {
          case AgsAnalysis.NoGuideStarForProbe(_) => false
          case _                                  => true
        }

    cwfs match {
      case Nil => AgsAnalysis.NoGuideStarForGroup(CanopusWfs.Group.instance) :: pwfs
      case _   => cwfs ++ pwfs
    }

  }

  override def analyze(
    ctx:        ObsContext,
    mt:         MagnitudeTable,
    guideProbe: ValidatableGuideProbe,
    guideStar:  SiderealTarget
  ): Option[AgsAnalysis] = {
    val boundsCheck: SiderealTarget => Boolean =
      if (guideProbe == pwfs1) GemsCandidates.pwfs1BoundsCheck(ctx)
      else GemsCandidates.cwfsBoundsCheck(ctx)

    if (boundsCheck(guideStar)) analyzeMagnitude(ctx, mt, guideProbe, guideStar)
    else Some(AgsAnalysis.NotReachable(guideProbe, guideStar))
  }

  override def analyzeMagnitude(
    ctx:        ObsContext,
    mt:         MagnitudeTable,
    guideProbe: ValidatableGuideProbe,
    guideStar:  SiderealTarget
  ): Option[AgsAnalysis] =
    Some(magAnalysis(ctx, mt, guideProbe, guideStar))

  private def magAnalysis(
    ctx:        ObsContext,
    mt:         MagnitudeTable,
    guideProbe: GuideProbe,
    guideStar:  SiderealTarget
  ): AgsAnalysis = {

    val magc: MagnitudeConstraints =
      if (guideProbe == pwfs1)
        GemsVoTableCatalog.pwfsMagnitudeConstraints(ctx, mt)
          .getOrElse(MagnitudeConstraints.unbounded(RBandsList))
      else
        GemsVoTableCatalog.cwfsMagnitudeConstraints(ctx)

    def usable: AgsAnalysis =
      AgsAnalysis.Usable(guideProbe, guideStar, None, AgsGuideQuality.DeliversRequestedIq)

    magc.searchBands
        .extract(guideStar)
        .fold(AgsAnalysis.NoMagnitudeForBand(guideProbe, guideStar): AgsAnalysis) { m =>
          if (magc.faintnessConstraint.contains(m.value)) {
            magc.saturationConstraint.fold(usable) { sat =>
              if (sat.contains(m.value)) usable
              else AgsAnalysis.MagnitudeTooBright(guideProbe, guideStar)
            }
          } else AgsAnalysis.MagnitudeTooFaint(guideProbe, guideStar, false)
        }
  }

  override def candidates(ctx: ObsContext, mt: MagnitudeTable)(ec: ExecutionContext): Future[List[ProbeCandidates]] =
    GemsVoTableCatalog(catalogName, backend)
      .search(ctx, mt)(ec)
      .map { ts =>

        // REL-3745: The code below handles its own range and magnitude filtering of CWFS candidates instead of
        // calling GemsCandidates.groupAndValidate. We make a new special-purpose validator using a
        // PatrolField.fromRadiusLimits(0, 1.3') so that candidates slightly outside of range will be shown alongside
        // strictly valid candidates. Regardless, the candidate pool is ultimately filtered down to valid targets
        // for selection. We do this in arcsecs so we can use the default value in CanopusWfs and simply grow the size.
        val pf                     = PatrolField.fromRadiusLimits(JAngle.ANGLE_0DEGREES,
                                           JAngle.arcsecs(15.5 + CanopusWfs.RADIUS_ARCSEC))
        assert(JAngle.arcsecs(15.5 + CanopusWfs.RADIUS_ARCSEC).toArcmins.compareToAngle(JAngle.arcmins(1.3)) == 0)
        val magc                   = GemsVoTableCatalog.cwfsMagnitudeConstraints(ctx)
        val candidatesWithValidMag = ts.filter(t => magc.searchBands.extract(t).exists(magc.contains))

        // A quick radius filter because the exact match done later is expensive
        // and in a crowded field it takes a while to exact match them all.
        val radiusFilter =
          for {
            rc <- RadiusLimitCalc.getAgsQueryRadiusLimits(Some(pf), ctx)
            c0 <- ctx.getBaseCoordinates.asScalaOpt
            c1 <- c0.toCoreCoordinates.asScalaOpt
          } yield rc.targetsFilter(c1)

        // Candidates with valid magnitude that are also within the correct distance,
        // ignoring position angle
        val cwfs = radiusFilter.fold(candidatesWithValidMag)(candidatesWithValidMag.filter)

        // REL-3747: here we use a special PWFS1 validator. GemsCandidates
        // returns just the "best" SFS option but we need to display all the
        // PWFS1 candidates.

        val sfsValidator = GemsCandidates.pwfs1Validator(ctx)
        val sfs          = ts.filter(t => sfsValidator.validate(new SPTarget(t), ctx) == VALID)

        ProbeCandidates(pwfs1, sfs) :: CanopusWfs.values.toList.map(ProbeCandidates(_, cwfs))
      }

  /**
   * Returns a list of catalog queries that would be used to search for guide stars with the given context
    */
  override def catalogQueries(ctx: ObsContext, mt: MagnitudeTable): List[CatalogQuery] =
    GemsVoTableCatalog.catalogQuery(ctx, mt, catalogName).toList

  private def posAngles(ctx: ObsContext): Set[Angle] = {
    val angles =
      ctx.getInstrument match {
        case pac: PosAngleConstraintAware if pac.getPosAngleConstraint == PosAngleConstraint.UNBOUNDED =>
          // TODO-NGS: this is what we've always done but it is poor as it
          // TODO-NGS: should depend on the distance of the center of the probe
          // TODO-NGS: range at all offset positions from the base
          Set(Angle.zero, Angle.fromDegrees(90.0), Angle.fromDegrees(180.0), Angle.fromDegrees(270.0))
        case _ =>
          Set.empty[Angle]
      }

    angles + ctx.getPositionAngle
  }

  private val canopusProbes: ISet[GuideProbe] =
    ISet.fromList(CanopusWfs.values.toList)

  override def estimate(ctx: ObsContext, mt: MagnitudeTable)(ec: ExecutionContext): Future[AgsStrategy.Estimate] =
    GemsVoTableCatalog(catalogName, backend)
      .search(ctx, mt)(ec)
      .map { ts =>
        val asterismSize =
          GemsResultsAnalyzer
            .analyzeGoodEnough(ctx, posAngles(ctx), ts, _.stars.size < 3)
            .map(_.guideGroup.grp.toManualGroup.targetMap.keySet.intersection(canopusProbes).size)
            .fold(0)(math.max)

        AgsStrategy.Estimate.toEstimate(asterismSize / 3.0) // sketchy, why is this the estimate?
      }

  override def select(ctx: ObsContext, mt: MagnitudeTable)(ec: ExecutionContext): Future[Option[AgsStrategy.Selection]] =
    // Run the catalog search, analyze the results, then pick the first one
    // (best strehl) and massage the result into an AGS selection.
    GemsVoTableCatalog(catalogName, backend)
      .search(ctx, mt)(ec)
      .map { ts =>
        GemsResultsAnalyzer.analyze(ctx, posAngles(ctx), ts, None).headOption.map { gs =>
          val assignments = gs.guideGroup.getAll.asScalaList.flatMap { gpt =>
            gpt.getTargets.asScalaList.map { t =>
              Assignment(gpt.getGuider, t.toSiderealTarget(ctx.getSchedulingBlockStart))
            }
          }
          Selection(gs.pa, assignments)
        }
      }

  override def guideProbes: List[GuideProbe] =
    CanopusWfs.values.toList ++ List(pwfs1)

  /**
   * Indicates the bands that will be used for a given probe
   */
  override def probeBands: BandsList = RBandsList

}
