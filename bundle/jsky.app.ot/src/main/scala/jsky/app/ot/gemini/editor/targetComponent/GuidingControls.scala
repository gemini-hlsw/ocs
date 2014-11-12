package jsky.app.ot.gemini.editor.targetComponent

import edu.gemini.ags.api._
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._

import jsky.app.ot.ags.AgsContext

import scala.swing._
import edu.gemini.ags.api.DefaultMagnitudeTable


class GuidingControls extends GridBagPanel {
  opaque = false

  private object guiderLabel extends Label {
    text                = "Guide with:"
    horizontalAlignment = Alignment.Right
    opaque              = false
  }

  layout(guiderLabel) = new Constraints {
    gridx  = 0
    insets = new Insets(0, 0, 0, 0)
  }

  val autoGuideStarGuiderSelector = new AgsStrategyCombo

  layout(Component.wrap(autoGuideStarGuiderSelector.getUi)) = new Constraints {
    gridx  = 1
    insets = new Insets(0, 5, 0, 10)
  }

  val autoGuideStarButton = new Button {
    import GuidingControls.{Search, Update}
    text = Search

    def update(analysis: List[AgsAnalysis]): Unit = {
      def noGuideStar(a: AgsAnalysis): Boolean = a match {
        case AgsAnalysis.NoGuideStarForGroup(_) => true
        case AgsAnalysis.NoGuideStarForProbe(_) => true
        case _ => false
      }

      enabled = analysis.nonEmpty // if empty, no strategy so no search
      text    = if (analysis.exists(noGuideStar)) Search else Update
    }
  }

  layout(autoGuideStarButton) = new Constraints {
    gridx  = 2
    insets = new Insets(0, 0, 0, 5)
  }

  val manualGuideStarButton = new Button {
    text = "Plot..."

    def update(analysis: List[AgsAnalysis]): Unit = {
      enabled = analysis.nonEmpty  // only empty if there is no strategy
    }
  }

  layout(manualGuideStarButton) = new Constraints {
    gridx  = 3
  }

  def update(ctxOpt: edu.gemini.shared.util.immutable.Option[ObsContext]): Unit = {
    val analysis = (for {
      ctx <- ctxOpt.asScalaOpt
      str <- AgsRegistrar.currentStrategy(ctx)
    } yield str.analyze(ctx, DefaultMagnitudeTable(ctx))).getOrElse(List.empty)
    autoGuideStarButton.update(analysis)
    manualGuideStarButton.update(analysis)
    autoGuideStarGuiderSelector.setAgsOptions(AgsContext.create(ctxOpt))
  }
}

object GuidingControls {
  private val Search = "Search"
  private val Update = "Update"
}


