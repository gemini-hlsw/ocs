package jsky.app.ot.gemini.editor.targetComponent

import edu.gemini.ags.api.AgsAnalysis.{NoGuideStarForProbe, NoGuideStarForGroup}
import edu.gemini.ags.api._
import edu.gemini.ags.api.AgsGuideQuality._
import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.core.BandsList
import edu.gemini.spModel.guide.{ValidatableGuideProbe, GuideSpeed}
import edu.gemini.spModel.guide.GuideSpeed._
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import jsky.app.ot.ags.BagsStatus
import jsky.app.ot.util.{Resources, OtColor}

import java.awt.Color._
import javax.swing.BorderFactory

import scala.collection.JavaConverters._
import scala.swing._
import scala.swing.GridBagPanel.Fill

object GuidingFeedback {
  import OtColor._

  object ProbeLimits {
    def apply(probeBands: BandsList, ctx: ObsContext, mc: MagnitudeCalc): Option[ProbeLimits] = {
      val conditions = ctx.getConditions
      val fast = mc.apply(conditions, FAST)

      def faint(gs: GuideSpeed) = mc.apply(conditions, gs).faintnessConstraint.brightness

      fast.saturationConstraint.map { sat =>
        ProbeLimits(probeBands, sat.brightness, faint(FAST), faint(MEDIUM), faint(SLOW))
      }
    }

    val le = '\u2264'
    def lim(d: Double): String = f"$d%.1f"
  }

  case class ProbeLimits(bands: BandsList, sat: Double, fast: Double, medium: Double, slow: Double) {
    import ProbeLimits.{le, lim}

    def searchRange: String =
      s"${lim(sat)} $le ${bands.bands.map(_.name).mkString(", ")} $le ${lim(slow)}"

    def detailRange: String =
      s"${lim(sat)} $le FAST $le ${lim(fast)} < MEDIUM $le ${lim(medium)} < SLOW $le ${lim(slow)}"
  }

  sealed trait Row extends GridBagPanel {
    protected val labelBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  }

  case class BagsStatusRow(bagsStatus: BagsStatus) extends Row {
    private val bgColor = bagsStatus match {
      case BagsStatus.Pending(None) | BagsStatus.Running(None) => BANANA
      case _                                                   => LIGHT_SALMON
    }

    private val statusIcon = Resources.getIcon("spinner16.gif")

    object feedbackLabel extends Label {
      border              = labelBorder
      foreground          = DARK_GRAY
      background          = bgColor
      text                = bagsStatus.message
      icon                = statusIcon
      opaque              = true
      horizontalAlignment = Alignment.Left
    }

    layout(feedbackLabel) = new Constraints {
      weightx = 1.0
      fill    = Fill.Both
    }
  }

  case class AnalysisRow(analysis: AgsAnalysis, probeLimits: Option[ProbeLimits], includeProbeName: Boolean) extends Row {
    val bg = analysis.quality match {
      case DeliversRequestedIq   => HONEY_DEW
      case PossibleIqDegradation => BANANA
      case IqDegradation         => CANTALOUPE
      case PossiblyUnusable      => TANGERINE
      case Unusable              => LIGHT_SALMON
    }

    object feedbackLabel extends Label {
      border     = labelBorder
      icon       = GuidingIcon(analysis.quality, enabled = true)
      foreground = DARK_GRAY
      background = bg
      text       = analysis.message(withProbe = includeProbeName)
      opaque     = true
      horizontalAlignment = Alignment.Left
    }

    object rangeLabel extends Label {
      border     = labelBorder
      foreground = DARK_GRAY
      background = bg
      text       = probeLimits.map(_.searchRange).getOrElse("")
      tooltip    = probeLimits.collect{case pl if pl.fast < pl.slow => pl.detailRange}.orNull
      opaque     = true
      horizontalAlignment = Alignment.Right
    }

    override def opaque_=(o: Boolean): Unit =
      if (o != opaque) {
        super.opaque = o
        feedbackLabel.opaque = o
        rangeLabel.opaque    = o
      }

    override def enabled_=(e: Boolean): Unit =
      if (e != enabled) {
        super.enabled = enabled
        feedbackLabel.icon = GuidingIcon(analysis.quality, enabled = false)
      }

    layout(feedbackLabel) = new Constraints {
      weightx = 1.0
      fill    = Fill.Both
    }

    layout(rangeLabel) = new Constraints {
      gridx = 1
      fill  = Fill.Both
    }
  }

  class Table extends GridBagPanel {
    def clear(): Unit = showRows(Nil)

    def showRow(row: Row): Unit = showRows(List(row))

    def showRows(rows: List[Row]): Unit = {
      layout.clear()

      rows.zipWithIndex.foreach { case (row, rowIndex) =>
        layout(row) = new Constraints {
          gridy   = rowIndex
          weightx = 1.0
          fill    = Fill.Horizontal
          insets  = new Insets(0, 0, 1, 0)
        }
      }

      revalidate()
    }
  }

  // GuidingFeedback.Rows corresponding to the observation as a whole.
  def obsAnalysis(ctx: ObsContext, mt: MagnitudeTable): List[AnalysisRow] = {
    AgsRegistrar.currentStrategy(ctx).map { strategy =>
      val (calcTable, analysis, probeBands) = (strategy.magnitudes(ctx, mt).toMap, strategy.analyze(ctx, mt), strategy.probeBands)
      val probeLimitsMap = calcTable.mapValues(ProbeLimits(probeBands, ctx, _))

      analysis.map { a =>
        val plo = for {
          gp <- AgsAnalysis.guideProbe(a)
          pl <- probeLimitsMap.get(gp).flatten
        } yield pl
        AnalysisRow(a, plo, includeProbeName = true)
      }
    }.getOrElse(Nil)
  }

  // Ugh, search through to figure out what the guide probe is, if any.
  private def guideProbe(env: TargetEnvironment, target: SPTarget): Option[ValidatableGuideProbe] = {
    val gpts = env.getGuideEnvironment.iterator().asScala.flatMap(_.iterator().asScala)
    gpts.find(_.containsTarget(target)).map(_.getGuider).collect {
      case v: ValidatableGuideProbe => v
    }
  }

  // GuidingFeedback.Rows corresponding to the given target.  If the base
  // position, show any global messages about missing guide stars. If a guide
  // star, show information particular to the guide star itself.
  def targetAnalysis(ctx: ObsContext, mt: MagnitudeTable, target: SPTarget): List[Row] = {
    val env = ctx.getTargets
    if (target == env.getBase) baseAnalysis(ctx, mt)
    else guideProbe(env, target).fold(List.empty[AnalysisRow]) { vgp =>
      guideStarAnalysis(ctx, mt, vgp, target).toList
    }
  }

  // GuidingFeedback.Rows corresponding to global errors like missing guide
  // stars.
  def baseAnalysis(ctx: ObsContext, mt: MagnitudeTable): List[AnalysisRow] =
    AgsRegistrar.currentStrategy(ctx).fold(List.empty[AnalysisRow]) { s =>
      s.analyze(ctx, mt).filter {
        case NoGuideStarForGroup(_, _) => true
        case NoGuideStarForProbe(_, _) => true
        case _                         => false
      }.map { a => AnalysisRow(a, None, includeProbeName = true) }
    }

  // GuidingFeedback.Row related to the given guide star itself.
  def guideStarAnalysis(ctx: ObsContext, mt: MagnitudeTable, gp: ValidatableGuideProbe, target: SPTarget): Option[AnalysisRow] =
    AgsRegistrar.currentStrategy(ctx).flatMap(_.analyze(ctx, mt, gp, target.toNewModel).map { a =>
      val plo = mt(ctx, gp).flatMap(ProbeLimits(a.probeBands, ctx, _))
      AnalysisRow(a, plo, includeProbeName = false)
    })
}