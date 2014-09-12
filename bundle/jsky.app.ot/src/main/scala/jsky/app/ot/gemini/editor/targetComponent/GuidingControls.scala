package jsky.app.ot.gemini.editor.targetComponent

import edu.gemini.ags.api._
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.spModel.rich.shared.immutable._

import java.awt.event.{ActionEvent, ActionListener, ItemEvent, ItemListener}
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

  val autoGuiderSelector          = new AgsStrategySelector
  val autoGuideStarGuiderSelector = new autoGuiderSelector.ComboBox

  layout(Component.wrap(autoGuideStarGuiderSelector)) = new Constraints {
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

      enabled = !analysis.isEmpty // if empty, no strategy so no search
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
      enabled = !analysis.isEmpty  // only empty if there is no strategy
    }
  }

  layout(manualGuideStarButton) = new Constraints {
    gridx  = 3
  }

  def init(obs: ISPObservation): Unit = {
    autoGuiderSelector.init(obs)
    update()
  }

  def resetSelectedAgsStrategy(): Unit =
    autoGuiderSelector.updateSelectedAgsStrategy(Option.empty[AgsStrategy].asGeminiOpt)

  def update(analysis: List[AgsAnalysis]): Unit = {
    autoGuideStarButton.update(analysis)
    manualGuideStarButton.update(analysis)
  }

  def update(): Unit = {
    val analysis = (for {
      ctx <- Option(autoGuiderSelector.getContext)
      str <- AgsRegistrar.selectedStrategy(ctx)
    } yield str.analyze(ctx, DefaultMagnitudeTable(ctx))).getOrElse(List.empty)
    update(analysis)
  }

  def toggleAgsGuiElements(visible: Boolean): Unit =
    this.visible = visible

  autoGuideStarGuiderSelector.addActionListener(new ActionListener {
    override def actionPerformed(e: ActionEvent): Unit =
      update()
  })
}

object GuidingControls {
  private val Search = "Search"
  private val Update = "Update"
}


