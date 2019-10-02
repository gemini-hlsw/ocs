package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsAnalysis.NoGuideStarForProbe
import edu.gemini.ags.api.{AgsAnalysis, AgsGuideQuality, AgsMagnitude, AgsStrategy, ProbeCandidates}
import edu.gemini.ags.api.AgsMagnitude.{ MagnitudeCalc, MagnitudeTable }
import edu.gemini.ags.api.AgsStrategy.{ Assignment, Selection }
import edu.gemini.ags.gems.{ GemsCandidates, GemsResultsAnalyzer, GemsVoTableCatalog }
import edu.gemini.catalog.api.{MagnitudeConstraints, CatalogName, CatalogQuery}
import edu.gemini.catalog.votable.VoTableBackend
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.ags.AgsStrategyKey.Ngs2Key
import edu.gemini.spModel.core.{Angle, BandsList, RBandsList, SiderealTarget}
import edu.gemini.spModel.gemini.gems.CanopusWfs
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.guide.{GuideSpeed, GuideProbe, ValidatableGuideProbe}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs1
import edu.gemini.spModel.telescope.{PosAngleConstraint, PosAngleConstraintAware}
import edu.gemini.pot.ModelConverters._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

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
        val (cwfs, sfs) =
          GemsCandidates.groupAndValidate(ctx, posAngles(ctx), ts)
            .foldLeft((List.empty[SiderealTarget], List.empty[SiderealTarget])) { case ((cwfs, sfs), gc) =>
              (gc.cwfsCandidates ++ cwfs, gc.slowFocusSensor :: sfs)
            }

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
