package jsky.app.ot.gemini.editor.targetComponent

import edu.gemini.ags.api._
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import jsky.app.ot.OT

import jsky.app.ot.ags.AgsContext

import scala.swing._

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

  val manualGuideStarButton = new Button("Manual GS")
  // Temporary button to go to the new manual search
  val newManualGuideStarButton = new Button("New Manual GS")
  newManualGuideStarButton.visible = false

  layout(manualGuideStarButton) = new Constraints {
    gridx  = 2
  }
  layout(newManualGuideStarButton) = new Constraints {
    gridx  = 3
  }

  def update(ctxOpt: edu.gemini.shared.util.immutable.Option[ObsContext]): Unit = {
    val analysis = (for {
      ctx <- ctxOpt.asScalaOpt
      str <- AgsRegistrar.currentStrategy(ctx)
    } yield str.analyze(ctx, OT.getMagnitudeTable)).getOrElse(List.empty)
    autoGuideStarGuiderSelector.setAgsOptions(AgsContext.create(ctxOpt))
  }

  def supportsAgs_=(supports: Boolean): Unit = {
    guiderLabel.visible = supports
    autoGuideStarGuiderSelector.getUi.setVisible(supports)
  }

  def supportsNewManualGS(supports: Boolean) {
    newManualGuideStarButton.visible = supports
  }
}
