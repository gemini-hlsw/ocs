package jsky.app.ot.gemini.editor.targetComponent

import edu.gemini.ags.api._
import edu.gemini.ags.api.AgsGuideQuality.{IqDegradation, PossiblyUnusable, PossibleIqDegradation, DeliversRequestedIq}
import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.catalog.api.MagnitudeLimits
import edu.gemini.spModel.guide.{GuideSpeed, GuideProbe}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import jsky.app.ot.OT
import jsky.app.ot.util.OtColor

import java.awt.Color
import java.text.DecimalFormat
import javax.swing.{Icon, BorderFactory}

import scala.swing._

import GuidingFeedback._

class GuidingFeedback extends GridBagPanel {
  border = BorderFactory.createEmptyBorder(2, 2, 2, 2)

  class Feedback(message: Message) extends GridBagPanel {
    object feedbackLabel extends Label {
      icon       = message.icon
      foreground = Color.DARK_GRAY
      background = message.color
      text       = message.text
      opaque     = true
      horizontalAlignment = Alignment.Left
    }

    object rangeLabel extends Label {
      foreground = Color.DARK_GRAY
      background = message.color
      text       = message.magRangeText.getOrElse("")
      opaque     = true
      horizontalAlignment = Alignment.Right

      if (message.fullMagRangeText.isDefined)
        tooltip = message.fullMagRangeText.get
    }

    layout(feedbackLabel) = new Constraints {
      weightx = 1.0
      fill = GridBagPanel.Fill.Horizontal
    }
    layout(rangeLabel) = new Constraints {
      gridx = 1
    }
  }


  def update(ctx: edu.gemini.shared.util.immutable.Option[ObsContext]): Unit =
    ctx.asScalaOpt.fold(reset())(update)

  def update(ctx: ObsContext): Unit = update(ctx, OT.getMagnitudeTable)

  def update(ctx: ObsContext, mt: MagnitudeTable): Unit = {
    val (calcTable, analysis) = AgsRegistrar.currentStrategy(ctx).fold((Map.empty[GuideProbe, MagnitudeCalc], List.empty[AgsAnalysis])) { strategy =>
      (strategy.magnitudes(ctx, mt).toMap, strategy.analyze(ctx, mt))
    }

    // Map of Guider -> (Map of GuideSpeed -> MagLimits), used to calculate the full magnitude range string
    // (e.g. 9.0 < FAST < 15.0 < MEDIUM < 15.8 < SLOW < 16.5)
    val probeSpeedLimits = calcTable.keys.map(gp => (gp, GuideSpeed.values.toList.map(gs => (gs, calcTable(gp).apply(ctx.getConditions, gs))).toMap)).toMap

    val limitsTable = calcTable.mapValues(mc => AgsMagnitude.autoSearchLimitsCalc(mc, ctx.getConditions))

    // Clear out the old messages.
    reset()

    val messages = analysis.map(h => Message(GuidingIcon.apply(h.qualityOption, enabled = true), toColor(h), feedbackText(h, analysis.size > 1),
      magRangeText(h, limitsTable), fullMagRangeText(h, probeSpeedLimits)))
    messages.zipWithIndex.foreach { case (msg, rowIndex) =>
      layout(new Feedback(msg)) = new Constraints {
        gridy = rowIndex
        weightx = 1.0
        fill = GridBagPanel.Fill.Horizontal
        insets = new Insets(0, 0, 1, 0)
      }
    }

    // Required when switching the primary guide star option.
    revalidate()
  }

  def reset(): Unit = {
    layout.clear()
  }
}

object GuidingFeedback {
  import OtColor._

  type ProbeSpeedLimits = Map[GuideProbe, Map[GuideSpeed, MagnitudeLimits]]

  case class Message(icon: Icon, color: Color, text: String, magRangeText: Option[String], fullMagRangeText: Option[String])

  def toColor(analysis: AgsAnalysis): Color = toColor(analysis.qualityOption)

  def toColor(quality: Option[AgsGuideQuality]): Color =
    quality.fold(LIGHT_SALMON) {
      case DeliversRequestedIq   => HONEY_DEW
      case PossibleIqDegradation => BANANA
      case IqDegradation         => CANTALOUPE
      case PossiblyUnusable      => TANGERINE
    }

  // Number formatter for formatting decimals.
  private val nf = new DecimalFormat("#.##") {{
    setMinimumFractionDigits(0)
    setMaximumFractionDigits(2)
  }}

  def fullMagRangeText(analysis: AgsAnalysis, probeSpeedLimits: ProbeSpeedLimits): Option[String] = {
    def toRangeText(guideProbe: GuideProbe, probeSpeedLimits: ProbeSpeedLimits): String = {
      def toText(guideSpeed: GuideSpeed, magLimits: MagnitudeLimits): String =
        s"${guideSpeed.name} \u2264 ${nf.format(magLimits.getFaintnessLimit.getBrightness)}"

      probeSpeedLimits.get(guideProbe).fold("")(_.map {
        case (GuideSpeed.FAST, fastLimits) =>
          fastLimits.getSaturationLimit.asScalaOpt.fold("") { sl => s"${nf.format(sl.getBrightness)} \u2264 "} + toText(GuideSpeed.FAST, fastLimits)
        case (guideSpeed, magLimits) => s"< ${toText(guideSpeed, magLimits)}"
      }.mkString(" "))
    }

    analysis match {
      case a: AgsAnalysisWithGuideProbe => Some(toRangeText(a.guideProbe, probeSpeedLimits))
      case _ => None
    }
  }

  def magRangeText(analysis: AgsAnalysis, limitsTable: Map[GuideProbe, MagnitudeLimits]): Option[String] = {
    def toRangeText(guideProbe: GuideProbe, limitsTable: Map[GuideProbe, MagnitudeLimits]): String =
      limitsTable.get(guideProbe).fold("")(magLimits => {
        val sl = magLimits.getSaturationLimit.asScalaOpt.fold("") { sl => s"${nf.format(sl.getBrightness)} \u2264 "}
        val fl = s" \u2264 ${nf.format(magLimits.getFaintnessLimit.getBrightness)}"
        s"$sl${magLimits.getBand}$fl"
      })
    analysis match {
      case a: AgsAnalysisWithGuideProbe => Some(toRangeText(a.guideProbe, limitsTable))
      case _ => None
    }
  }

  def feedbackText(analysis: AgsAnalysis, showGuideProbeName: Boolean): String =
    (if (showGuideProbeName)
      analysis match {
        case a: AgsAnalysisWithGuideProbe => s"${a.guideProbe.getKey}  "
        case _ => ""
      }
    else "") + analysis.feedbackMessage
}
