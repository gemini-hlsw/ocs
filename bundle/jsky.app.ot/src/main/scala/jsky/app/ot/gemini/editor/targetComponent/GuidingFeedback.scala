package jsky.app.ot.gemini.editor.targetComponent

import edu.gemini.ags.api._
import edu.gemini.ags.api.AgsGuideQuality._
import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.spModel.guide.{GuideSpeed, GuideProbe}
import edu.gemini.spModel.guide.GuideSpeed._
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import jsky.app.ot.OT
import jsky.app.ot.util.OtColor

import java.awt.Color.DARK_GRAY
import javax.swing.BorderFactory

import scala.swing._
import scala.swing.GridBagPanel.Fill

import GuidingFeedback._

class GuidingFeedback extends GridBagPanel {
  border = BorderFactory.createEmptyBorder(2, 2, 2, 2)

  def update(ctx: edu.gemini.shared.util.immutable.Option[ObsContext]): Unit =
    ctx.asScalaOpt.fold(reset())(update)

  def update(ctx: ObsContext): Unit = update(ctx, OT.getMagnitudeTable)

  def update(ctx: ObsContext, mt: MagnitudeTable): Unit = {
    val (calcTable, analysis) = AgsRegistrar.currentStrategy(ctx).fold((Map.empty[GuideProbe, MagnitudeCalc], List.empty[AgsAnalysis])) { strategy =>
      (strategy.magnitudes(ctx, mt).toMap, strategy.analyze(ctx, mt))
    }

    val probeLimitsMap = calcTable.mapValues(ProbeLimits(ctx, _))

    // Clear out the old messages, create new messages for each analysis, and add them to the feedback.
    reset()

    analysis.map { a =>
      val limits = for {
        p  <- AgsAnalysis.guideProbe(a)
        lo <- probeLimitsMap.get(p)
        l  <- lo
      } yield l

      new Row(a, limits, includeProbeName = true)
    }.zipWithIndex.foreach { case (row, rowIndex) =>
      layout(row) = new Constraints {
        gridy   = rowIndex
        weightx = 1.0
        fill    = Fill.Horizontal
        insets  = new Insets(0, 0, 1, 0)
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

  object ProbeLimits {
    def apply(ctx: ObsContext, mc: MagnitudeCalc): Option[ProbeLimits] = {
      val cnds = ctx.getConditions
      val fast = mc.apply(cnds, FAST)

      def faint(gs: GuideSpeed) = mc.apply(cnds, gs).getFaintnessLimit.getBrightness

      fast.getSaturationLimit.asScalaOpt.map { sat =>
        ProbeLimits(fast.getBand, sat.getBrightness, faint(FAST), faint(MEDIUM), faint(SLOW))
      }
    }

    val le = '\u2264'
    def lim(d: Double): String = f"$d%.1f"
  }

  case class ProbeLimits(band: Magnitude.Band, sat: Double, fast: Double, medium: Double, slow: Double) {
    import ProbeLimits.{le, lim}

    def searchRange: String =
      s"${lim(sat)} $le $band $le ${lim(slow)}"

    def detailRange: String =
      s"${lim(sat)} $le FAST $le ${lim(fast)} < MEDIUM $le ${lim(medium)} < SLOW $le ${lim(slow)}"
  }

  class Row(analysis: AgsAnalysis, probeLimits: Option[ProbeLimits], includeProbeName: Boolean) extends GridBagPanel {
    val bg = analysis.quality match {
      case DeliversRequestedIq   => HONEY_DEW
      case PossibleIqDegradation => BANANA
      case IqDegradation         => CANTALOUPE
      case PossiblyUnusable      => TANGERINE
      case Unusable              => LIGHT_SALMON
    }

    object feedbackLabel extends Label {
      icon       = GuidingIcon(analysis.quality, enabled = true)
      foreground = DARK_GRAY
      background = bg
      text       = analysis.message(withProbe = includeProbeName)
      opaque     = true
      horizontalAlignment = Alignment.Left
    }

    object rangeLabel extends Label {
      foreground = DARK_GRAY
      background = bg
      text       = probeLimits.map(_.searchRange).getOrElse("")
      tooltip    = probeLimits.map(_.detailRange).orNull
      opaque     = true
      horizontalAlignment = Alignment.Right
    }

    override def opaque_=(o: Boolean): Unit =
      if (o != opaque) {
        feedbackLabel.opaque = o
        rangeLabel.opaque    = o
      }

    layout(feedbackLabel) = new Constraints {
      weightx = 1.0
      fill    = Fill.Horizontal
    }

    layout(rangeLabel) = new Constraints {
      gridx = 1
    }
  }
}
