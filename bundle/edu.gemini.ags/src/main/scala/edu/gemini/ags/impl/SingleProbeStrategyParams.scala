package edu.gemini.ags.impl

import edu.gemini.ags.api.{MagnitudeExtractor, AgsMagnitude, defaultProbeBands, magnitudeExtractor}
import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.catalog.api.{CatalogQuery, RadiusConstraint}
import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core.{MagnitudeBand, Angle, Site}
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider
import edu.gemini.spModel.gemini.flamingos2.Flamingos2OiwfsGuideProbe
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe
import edu.gemini.spModel.gemini.gnirs.GnirsOiwfsGuideProbe
import edu.gemini.spModel.gemini.nifs.NifsOiwfsGuideProbe
import edu.gemini.spModel.gemini.niri.NiriOiwfsGuideProbe
import edu.gemini.spModel.guide.{GuideStarValidator, PatrolField, ValidatableGuideProbe}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe

sealed trait SingleProbeStrategyParams {
  def site: Site
  def guideProbe: ValidatableGuideProbe
  def stepSize: Angle                        = Angle.fromDegrees(10)
  def minDistance: Option[Angle]             = Some(Angle.fromArcsecs(20))
  // Different instruments have a different reference band to decide what magnitude to use
  protected def referenceBand: MagnitudeBand = MagnitudeBand.R

  final def catalogQueries(ctx: ObsContext, mt: MagnitudeTable): Option[CatalogQuery] =
    for {
      mc <- magnitudeCalc(ctx, mt)
      rc <- radiusConstraint(ctx)
      ml =  AgsMagnitude.manualSearchLimits(mc)
    } yield CatalogQuery.catalogQueryRangeOnBand(ctx.getBaseCoordinates.toNewModel, rc, referenceMagnitude, Some(ml))

  def radiusConstraint(ctx: ObsContext): Option[RadiusConstraint] =
    RadiusLimitCalc.getAgsQueryRadiusLimits(guideProbe, ctx)

  def magnitudeCalc(ctx: ObsContext, mt: MagnitudeTable): Option[MagnitudeCalc] =
    mt(ctx, guideProbe)

  def validator(ctx: ObsContext): GuideStarValidator = guideProbe

  def probeBands = defaultProbeBands(referenceBand)

  // For a given target return a magnitude value that can be used to select a target
  def referenceMagnitude: MagnitudeExtractor = (st: SiderealTarget) => magnitudeExtractor(probeBands)(st)

  def brightest[A](lst: List[A])(toSiderealTarget: A => SiderealTarget):Option[A] = {
    def magnitude(t: SiderealTarget):Option[Double] = {
      val m = referenceMagnitude(t)
      m.map(_.value)
    }
    if (lst.isEmpty) None
    else Some(lst.minBy(t => magnitude(toSiderealTarget(t))))
  }

}

object SingleProbeStrategyParams {
  case object AltairAowfsParams extends SingleProbeStrategyParams {
    val guideProbe           = AltairAowfsGuider.instance
    val site                 = Site.GN
    override def stepSize    = Angle.fromDegrees(90)
    override def minDistance = Some(Angle.zero)
  }

  case object Flamingos2OiwfsParams extends SingleProbeStrategyParams {
    val guideProbe        = Flamingos2OiwfsGuideProbe.instance
    val site              = Site.GS
    override def stepSize = Angle.fromDegrees(90)
  }

  case class GmosOiwfsParams(site: Site) extends SingleProbeStrategyParams {
    val guideProbe = GmosOiwfsGuideProbe.instance
  }

  case object GnirsOiwfsParams extends SingleProbeStrategyParams {
    val guideProbe             = GnirsOiwfsGuideProbe.instance
    val site                   = Site.GN
    override val referenceBand = MagnitudeBand.K
  }

  case object NifsOiwfsParams extends SingleProbeStrategyParams {
    val guideProbe             = NifsOiwfsGuideProbe.instance
    val site                   = Site.GN
    override val referenceBand = MagnitudeBand.K

  }

  case object NiriOiwfsParams extends SingleProbeStrategyParams {
    val guideProbe             = NiriOiwfsGuideProbe.instance
    val site                   = Site.GN
    override val referenceBand = MagnitudeBand.K
  }

case class PwfsParams(site: Site, guideProbe: PwfsGuideProbe) extends SingleProbeStrategyParams {
    override def stepSize = Angle.fromDegrees(360)

    private def vignettingProofPatrolField(ctx: ObsContext): PatrolField = {
      val min = guideProbe.getVignettingClearance(ctx)
      guideProbe.getCorrectedPatrolField(PatrolField.fromRadiusLimits(min, PwfsGuideProbe.PWFS_RADIUS), ctx)
    }

    override def radiusConstraint(ctx: ObsContext): Option[RadiusConstraint] =
      RadiusLimitCalc.getAgsQueryRadiusLimits(Some(vignettingProofPatrolField(ctx)), ctx)

    // We have a special validator for Pwfs.
    override def validator(ctx: ObsContext): GuideStarValidator =
      vignettingProofPatrolField(ctx).validator(ctx)
  }
}