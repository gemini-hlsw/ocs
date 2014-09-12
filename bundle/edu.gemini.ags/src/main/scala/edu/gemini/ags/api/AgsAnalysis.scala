package edu.gemini.ags.api

import edu.gemini.ags.api.AgsGuideQuality.{PossibleIqDegradation, DeliversRequestedIq}
import edu.gemini.ags.api.AgsMagnitude._
import edu.gemini.ags.impl.Strategy
import edu.gemini.catalog.api.MagnitudeLimits
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.spModel.core.Site
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
  case class NoGuideStarForProbe(guideProbe: GuideProbe) extends AgsAnalysisWithGuideProbe {
    override val message = "No guide star selected."
  }

  case class NoGuideStarForGroup(guideGroup: GuideProbeGroup) extends AgsAnalysis {
    override val message = s"No ${guideGroup.getDisplayName} guide star selected."
  }

  case class MagnitudeOutOfRange(guideProbe: GuideProbe, target: SPTarget) extends AgsAnalysisWithGuideProbe {
    override val message = "Cannot guide with the star in these conditions, even using the slowest guide speed."
  }

  case class NotReachable(guideProbe: GuideProbe, target: SPTarget) extends AgsAnalysisWithGuideProbe {
    override val message = "The star is not reachable at all positions."
  }

  case class Usable(guideProbe: GuideProbe, target: SPTarget, guideSpeed: GuideSpeed, quality: AgsGuideQuality) extends AgsAnalysisWithGuideProbe {
    override def qualityOption = Some(quality)

    override def message: String = quality.message + (quality match {
      case DeliversRequestedIq => ""
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

    // Andy's 0.5 fainter than the limit rule ...  Even if the guide star falls
    // out of the faintness limit it may still be usable.
    def tooFaint(magCalc: MagnitudeCalc, mag: Magnitude): AgsAnalysis = {
      def almost(lim: MagnitudeLimits): Boolean =
        Math.abs(mag.getBrightness - lim.getFaintnessLimit.getBrightness) <= 0.5

      if (almost(magCalc(conds, SLOW))) Usable(guideProbe, guideStar, SLOW, PossiblyUnusable)
      else MagnitudeOutOfRange(guideProbe, guideStar)
    }

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
      site <- Strategy.site(ctx)
      mc   <- mt(site, guideProbe)
      mag  <- guideStar.getMagnitude(band(mc)).asScalaOpt
    } yield fastestGuideSpeed(mc, mag, conds).fold(tooFaint(mc, mag))(usable)

    magAnalysis.getOrElse(MagnitudeOutOfRange(guideProbe, guideStar))
  }
}
