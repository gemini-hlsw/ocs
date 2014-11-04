package edu.gemini.ags.api

import edu.gemini.ags.api.AgsMagnitude._
import edu.gemini.ags.impl.Strategy
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.ImageQuality
import edu.gemini.spModel.guide.{ValidatableGuideProbe, GuideProbe, GuideProbeGroup, GuideSpeed}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.SPTarget


import scalaz._
import Scalaz._

sealed trait AgsGuideQuality extends Ordered[AgsGuideQuality] {
  def ord: Int
  def compare(that: AgsGuideQuality): Int = ord.compareTo(that.ord)
  def message: String = ""
}

object AgsGuideQuality {
  case object DeliversRequestedIq extends AgsGuideQuality   {
    val ord = 0
  }
  case object PossibleIqDegradation extends AgsGuideQuality {
    val ord = 1
    override val message = "Slower guiding required; may not deliver requested IQ."
  }
  case object IqDegradation extends AgsGuideQuality         {
    val ord = 2
    override val message = "Slower guiding required; will not deliver requested IQ."
  }
  case object PossiblyUnusable extends AgsGuideQuality      {
    val ord = 3
    override val message = "May not be able to guide."
  }

  val All: List[AgsGuideQuality] =
    List(DeliversRequestedIq, PossibleIqDegradation, IqDegradation, PossiblyUnusable)
}

sealed trait AgsAnalysis {
  def qualityOption: Option[AgsGuideQuality] = None
  def message: String
}

sealed trait AgsAnalysisWithGuideProbe extends AgsAnalysis {
  def guideProbe: GuideProbe
}

object AgsAnalysis {
  case object UnknownError extends AgsAnalysis {
    override val message = "Unknown error."
  }

  case class NoGuideStarForProbe(guideProbe: GuideProbe) extends AgsAnalysisWithGuideProbe {
    override val message = s"No ${guideProbe.getKey} guide star selected."
  }

  case class NoGuideStarForGroup(guideGroup: GuideProbeGroup) extends AgsAnalysis {
    override val message = s"No ${guideGroup.getKey} guide star selected."
  }

  case class MagnitudeTooDim(guideProbe: GuideProbe, target: SPTarget) extends AgsAnalysisWithGuideProbe {
    override val message = "Cannot guide with the star in these conditions, even using the slowest guide speed."
  }

  case class MagnitudeTooBright(guideProbe: GuideProbe, target: SPTarget) extends AgsAnalysisWithGuideProbe {
    override val message = "Guide star is too bright to guide."
  }

  case class NoMagnitudeForBand(guideProbe: GuideProbe, target: SPTarget, band: Magnitude.Band) extends AgsAnalysisWithGuideProbe {
    override val message = s"Guide star ${band.name}-band magnitude is missing."
  }

  case class NotReachable(guideProbe: GuideProbe, target: SPTarget) extends AgsAnalysisWithGuideProbe {
    override val message = "The star is not reachable at all positions."
  }

  case class Usable(guideProbe: GuideProbe, target: SPTarget, guideSpeed: GuideSpeed, quality: AgsGuideQuality) extends AgsAnalysisWithGuideProbe {
    override def qualityOption = Some(quality)

    override def message: String = quality.message + (quality match {
      case AgsGuideQuality.DeliversRequestedIq => ""
      case _ => " "
    }) + s"Guide Speed: ${guideSpeed.name}."
  }

  def worstCaseGuideQuality(analysis: List[AgsAnalysis]): Option[AgsGuideQuality] =
    analysis.map(_.qualityOption).sequence.flatMap {
      case Nil => None
      case lst => Some(lst.max)
    }

  /**
   * Analysis of the selected guide star (if any) in the given context.
   */
  def analysis(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe): AgsAnalysis = {
    def selection(ctx: ObsContext, guideProbe: GuideProbe): Option[SPTarget] =
      for {
        gpt   <- ctx.getTargets.getPrimaryGuideProbeTargets(guideProbe).asScalaOpt
        gStar <- gpt.getPrimary.asScalaOpt
      } yield gStar

    selection(ctx, guideProbe).fold(NoGuideStarForProbe(guideProbe): AgsAnalysis) { guideStar =>
      AgsAnalysis.analysis(ctx, mt, guideProbe, guideStar)
    }
  }

  /**
   * Analysis of the given guide star in the given context, regardless of which
   * guide star is actually selected in the target environment.
   */
  def analysis(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SPTarget): AgsAnalysis =
    if (!guideProbe.validate(guideStar, ctx)) NotReachable(guideProbe, guideStar)
    else magnitudeAnalysis(ctx, mt, guideProbe, guideStar)

  private def magnitudeAnalysis(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SPTarget): AgsAnalysis = {
    import GuideSpeed._
    import AgsGuideQuality._

    val conds = ctx.getConditions

    // Handles the case where the magnitude falls outside of the acceptable ranges for any guide speed.
    // This handles Andy's 0.5 rule where we might possibly be able to guide if the star is only 0.5 too dim, and
    // otherwise returns the appropriate analysis indicating too dim or too bright.
    def outsideLimits(magCalc: MagnitudeCalc, mag: Magnitude): AgsAnalysis = {
      val adj             = 0.5
      val saturationLimit = magCalc(conds, FAST).getSaturationLimit.asScalaOpt
      val faintnessLimit  = magCalc(conds, SLOW).getFaintnessLimit.getBrightness
      val saturated       = saturationLimit.fold(false)(_.getBrightness > mag.getBrightness)

      def almostTooDim: Boolean = !saturated && mag.getBrightness <= faintnessLimit + adj
      def tooDim:       Boolean = mag.getBrightness > faintnessLimit + adj

      if (almostTooDim)         Usable(guideProbe, guideStar, SLOW, PossiblyUnusable)
      else if (tooDim)          MagnitudeTooDim(guideProbe, guideStar)
      else                      MagnitudeTooBright(guideProbe, guideStar)
    }

    // Called when we know that a valid guide speed can be chosen for the given guide star.
    // Determine the quality and return an analysis indicating that the star is usable.
    def usable(guideSpeed: GuideSpeed): AgsAnalysis = {
      def worseOrEqual(iq: ImageQuality) = conds.iq.compareTo(iq) >= 0

      val quality = guideSpeed match {
        case FAST =>
          DeliversRequestedIq
        case MEDIUM =>
          if (worseOrEqual(ImageQuality.PERCENT_70)) DeliversRequestedIq
          else PossibleIqDegradation
        case SLOW =>
          if (worseOrEqual(ImageQuality.PERCENT_85)) DeliversRequestedIq
          else if (worseOrEqual(ImageQuality.PERCENT_70)) PossibleIqDegradation
          else IqDegradation
      }

      Usable(guideProbe, guideStar, guideSpeed, quality)
    }

    val magAnalysis = for {
      site      <- Strategy.site(ctx)
      mc        <- mt(site, guideProbe)
      probeBand = band(mc)
    } yield {
      val magOpt      = guideStar.getMagnitude(probeBand).asScalaOpt
      val analysisOpt = magOpt.map(mag => fastestGuideSpeed(mc, mag, conds).fold(outsideLimits(mc, mag))(usable))
      analysisOpt.getOrElse(NoMagnitudeForBand(guideProbe, guideStar, probeBand))
    }

    // This should theoretically never happen, but if it does, something has gone wrong with the site or magnitude table.
    magAnalysis.getOrElse(UnknownError)
  }
}