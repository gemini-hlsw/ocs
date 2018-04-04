package jsky.app.ot.gemini.ghost

import java.beans.PropertyDescriptor

import javax.swing.JPanel
import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.spModel.gemini.ghost.{Ghost, GhostAsterism}
import edu.gemini.shared.gui.bean.TextFieldPropertyCtrl
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GhostStandardResTargets
import jsky.app.ot.gemini.editor.ComponentEditor

import scala.swing._
import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.event.ButtonClicked


final class GhostEditor extends ComponentEditor[ISPObsComponent, Ghost] {

  private object ui extends GridBagPanel {
    border = ComponentEditor.PANEL_BORDER

    /** Position angle components.
      */
    val posAngleProp: PropertyDescriptor = Ghost.POS_ANGLE_PROP
    val posAngleLabel: Label = new Label(posAngleProp.getDisplayName)
    posAngleLabel.horizontalAlignment = Alignment.Right
    val posAngleUnits: Label = new Label("deg E of N")
    posAngleUnits.horizontalAlignment = Alignment.Left
    val posAngleCtrl: TextFieldPropertyCtrl[Ghost, java.lang.Double] = TextFieldPropertyCtrl.createDoubleInstance(posAngleProp, 1)
    posAngleCtrl.setColumns(10)


    val tfComp: Component = Component.wrap(posAngleCtrl.getTextField)
    layout(posAngleLabel) = new Constraints() {
      anchor = Anchor.NorthWest
      insets = new Insets(3, 10, 0, 20)
    }
    layout(tfComp) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 1
      insets = new Insets(0, 0, 0, 20)
    }
    layout(posAngleUnits) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 2
      insets = new Insets(3, 0, 0, 20)
    }
    layout(new Label) = new Constraints() {
      anchor = Anchor.West
      gridx = 3
      weightx = 1.0
    }

    layout(new Separator()) = new Constraints() {
      anchor = Anchor.West
      fill = Fill.Horizontal
      gridy = 1
      gridwidth = 3
      insets = new Insets(20, 0, 20, 0)
    }

    /** Standard vs. high resolution components.
     */
    val standardResolution: RadioButton = new RadioButton("Standard Resolution")
    val highResolution:     RadioButton = new RadioButton("High Resolution")
    val buttonGroup:        ButtonGroup = new ButtonGroup()
    buttonGroup.buttons += standardResolution
    buttonGroup.buttons += highResolution

    layout(standardResolution) = new Constraints() {
      anchor = Anchor.NorthWest
      gridwidth = 2
      gridy = 2
      insets = new Insets(0, 10, 0, 20)
    }
    layout(highResolution) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 2
      gridy = 2
    }

    val asterismLabel: Label = new Label("Select configuration:")
    asterismLabel.horizontalAlignment = Alignment.Right
    layout(asterismLabel) = new Constraints() {
      anchor = Anchor.East
      gridwidth = 2
      gridy = 3
      insets = new Insets(22, 10, 0, 20)
    }

    // TODO: This will change. I'm going to make it call transformation methods between asterism types.
    // TODO: I still have a little thinking as to how this will be implemented and where these names
    // TODO: belong, so this is just a "placeholder" for now.
    val asterismMap: Map[String, () => GhostStandardResTargets] = Map(
      "Single target" -> (() => GhostStandardResTargets.emptySingleTarget),
      "Dual target"   -> (() => GhostStandardResTargets.emptyDualTarget),
      "SRIFU1 target, SRIFU2 sky position" -> (() => GhostStandardResTargets.emptyTargetPlusSky),
      "SRIFU1 sky position, SRIFU2 target" -> (() => GhostStandardResTargets.emptySkyPlusTarget)
    )

    val asterismComboBox: ComboBox[String] = new ComboBox[String](asterismMap.keys.toSeq)
    layout(asterismComboBox) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 2
      gridy = 3
      insets = new Insets(20, 0, 0, 20)
    }

    layout(new Label) = new Constraints() {
      anchor = Anchor.North
      gridy = 4
      weighty = 1.0
    }

    listenTo(standardResolution, highResolution, asterismComboBox)
    reactions += {
      case ButtonClicked(`standardResolution`) =>
        showStandardResolutionComponents(true)
      case ButtonClicked(`highResolution`) =>
        showStandardResolutionComponents(false)
    }

    def initialize(): Unit = {
      val isHighResolution = Option(getContextTargetEnv).exists(_.getAsterism.isInstanceOf[GhostAsterism.HighResolution])
      if (isHighResolution)
        highResolution.doClick()
      else
        standardResolution.doClick()
    }

    def showStandardResolutionComponents(viewable: Boolean): Unit = {
      asterismLabel.visible = viewable
      asterismComboBox.visible = viewable
    }
  }

  override def getWindow: JPanel = ui.peer

  override def handlePostDataObjectUpdate(dataObj: Ghost): Unit = {
    ui.posAngleCtrl.setBean(dataObj)
    ui.initialize()
  }
}