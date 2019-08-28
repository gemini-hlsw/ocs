package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsMagnitude
import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.ags.conf.{FaintnessKey, ProbeLimitsCalc}
import edu.gemini.catalog.api._
import edu.gemini.catalog.api.CatalogName.UCAC4
import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider
import edu.gemini.spModel.gemini.flamingos2.Flamingos2OiwfsGuideProbe
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe
import edu.gemini.spModel.gemini.gnirs.GnirsOiwfsGuideProbe
import edu.gemini.spModel.gemini.nifs.NifsOiwfsGuideProbe
import edu.gemini.spModel.gemini.niri.NiriOiwfsGuideProbe
import edu.gemini.spModel.guide.{GuideSpeed, GuideStarValidator, PatrolField, ValidatableGuideProbe}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality

import scalaz._
import Scalaz._

sealed trait SingleProbeStrategyParams {
  def site: Site
  def guideProbe: ValidatableGuideProbe
  def stepSize: Angle                        = Angle.fromDegrees(10)
  def minDistance: Option[Angle]             = Some(Angle.fromArcsecs(20))
  def catalogName: CatalogName               = UCAC4

  final def catalogQueries(ctx: ObsContext, mt: MagnitudeTable): Option[CatalogQuery] =
    for {
      base <- ctx.getBaseCoordinates.asScalaOpt
      mc   <- magnitudeCalc(ctx, mt)
      rc   <- radiusConstraint(ctx)
      ml   <- AgsMagnitude.manualSearchConstraints(mc)
    } yield CatalogQuery(base.toNewModel, rc, ml, catalogName)

  def radiusConstraint(ctx: ObsContext): Option[RadiusConstraint] =
    RadiusLimitCalc.getAgsQueryRadiusLimits(guideProbe, ctx)

  def magnitudeCalc(ctx: ObsContext, mt: MagnitudeTable): Option[MagnitudeCalc] =
    mt(ctx, guideProbe)

  def validator(ctx: ObsContext): GuideStarValidator = guideProbe

  def probeBands: BandsList = RBandsList

  // For a given target return a magnitude value that can be used to select a target
  def referenceMagnitude(st: SiderealTarget):Option[Magnitude] = probeBands.extract(st)

  def brightest[A](lst: List[A])(toSiderealTarget: A => SiderealTarget):Option[A] = {
    def magnitude(t: SiderealTarget):Option[Double] = {
      val m = referenceMagnitude(t)
      m.map(_.value)
    }
    if (lst.isEmpty) None
    else Some(lst.minBy(t => magnitude(toSiderealTarget(t))))
  }

  def hasGuideSpeed: Boolean = true
}

object SingleProbeStrategyParams {
  case object AltairAowfsParams extends SingleProbeStrategyParams {
    override val guideProbe  = AltairAowfsGuider.instance
    override val site        = Site.GN
    override def stepSize    = Angle.fromDegrees(90)
    override def minDistance = Some(Angle.zero)
  }

  case object Flamingos2OiwfsParams extends SingleProbeStrategyParams {
    override val guideProbe = Flamingos2OiwfsGuideProbe.instance
    override val site       = Site.GS
  }

  case class GmosOiwfsParams(site: Site) extends SingleProbeStrategyParams {
    override val guideProbe = GmosOiwfsGuideProbe.instance
  }

  case object GnirsOiwfsParams extends SingleProbeStrategyParams {
    override val guideProbe = GnirsOiwfsGuideProbe.instance
    override val site       = Site.GN
    override val probeBands = SingleBand(MagnitudeBand.K)
  }

  case object NifsOiwfsParams extends SingleProbeStrategyParams {
    override val guideProbe = NifsOiwfsGuideProbe.instance
    override val site       = Site.GN
    override val probeBands = SingleBand(MagnitudeBand.K)
  }

  case object NiriOiwfsParams extends SingleProbeStrategyParams {
    override val guideProbe = NiriOiwfsGuideProbe.instance
    override val site       = Site.GN
    override val probeBands = SingleBand(MagnitudeBand.K)
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

case object Pwfs1NGS2Params extends SingleProbeStrategyParams {
  override val guideProbe: PwfsGuideProbe = PwfsGuideProbe.pwfs1
  override val site: Site                 = Site.GS
  override def stepSize: Angle            = Angle.fromDegrees(360)
  override val catalogName: CatalogName   = PPMXL

  private def vignettingProofPatrolField(ctx: ObsContext): PatrolField = {
    val min = guideProbe.getVignettingClearance(ctx)
    guideProbe.getCorrectedPatrolField(PatrolField.fromRadiusLimits(min, PwfsGuideProbe.PWFS_RADIUS), ctx)
  }

  override def radiusConstraint(ctx: ObsContext): Option[RadiusConstraint] =
    RadiusLimitCalc.getAgsQueryRadiusLimits(Some(vignettingProofPatrolField(ctx)), ctx)

  // We have a special validator for Pwfs.
  override def validator(ctx: ObsContext): GuideStarValidator =
    vignettingProofPatrolField(ctx).validator(ctx)

  // TODO-NGS2: Check this.
  // TODO-NGS2: The magic number 2.5 should be moved somewhere, but where?
  /**
    * Since PWFS1 is used only for slow focus sensing in NGS2 and not for guiding, we can use stars up to 2.5 mag
    * fainter than with regular PWFS1.
    * This is an ugly copy-paste hack, but I'm not sure how else to get the additional 2.5 mag faintness in the
    * magnitude calculation short of adding another PWFS guide probe, which would be a nightmare.
    */
  override def magnitudeCalc(ctx: ObsContext, mt: MagnitudeTable): Option[MagnitudeCalc] = {
    mt(ctx, guideProbe) match {
      case Some(ProbeLimitsCalc(band, saturationAdjustment, faintnessTable)) =>
        new MagnitudeCalc {
          override def apply(c: SPSiteQuality.Conditions, gs: GuideSpeed): MagnitudeConstraints = {
            val unadjustedFaint = faintnessTable(FaintnessKey(c.iq, c.sb, gs))
            val bright = unadjustedFaint - saturationAdjustment
            val faint  = unadjustedFaint + 2.5
            c.cc.adjust(MagnitudeConstraints(BandsList.bandList(band), FaintnessConstraint(faint), Some(SaturationConstraint(bright))))
          }
        }.some
      case _ => None
    }
  }

  override val hasGuideSpeed: Boolean = false
}
