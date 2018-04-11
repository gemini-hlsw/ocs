package jsky.app.ot.gemini.ghost

import java.beans.PropertyDescriptor
import javax.swing.JPanel

import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.shared.gui.bean.TextFieldPropertyCtrl
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.gemini.ghost.Ghost
import edu.gemini.spModel.gemini.ghost.AsterismConverters._
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.target.env.AsterismType
import edu.gemini.spModel.target.env.AsterismType._
import edu.gemini.spModel.target.obsComp.TargetObsComp
import jsky.app.ot.gemini.editor.ComponentEditor

import scala.swing._
import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.event.SelectionChanged


final class GhostEditor extends ComponentEditor[ISPObsComponent, Ghost] {

  private object ui extends GridBagPanel {
    border = ComponentEditor.PANEL_BORDER

    /** Position angle components. */
    val posAngleProp: PropertyDescriptor = Ghost.POS_ANGLE_PROP
    val posAngleLabel: Label = new Label(posAngleProp.getDisplayName)
    posAngleLabel.horizontalAlignment = Alignment.Right
    val posAngleUnits: Label = new Label("deg E of N")
    posAngleUnits.horizontalAlignment = Alignment.Left
    val posAngleCtrl: TextFieldPropertyCtrl[Ghost, java.lang.Double] = TextFieldPropertyCtrl.createDoubleInstance(posAngleProp, 1)
    posAngleCtrl.setColumns(10)


    val tfComp: Component = Component.wrap(posAngleCtrl.getTextField)
    layout(posAngleLabel) = new Constraints() {
      anchor = Anchor.NorthEast
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
      insets = new Insets(10, 0, 0, 0)
    }

    val asterismLabel: Label = new Label("Select configuration:")
    asterismLabel.horizontalAlignment = Alignment.Right
    layout(asterismLabel) = new Constraints() {
      anchor = Anchor.East
      gridy = 2
      insets = new Insets(12, 10, 0, 20)
    }

    /** A list of available asterism types. */
    val asterismList: List[AsterismType] = List(
      GhostStandardResolutionSingleTarget,
      GhostStandardResolutionDualTarget,
      GhostStandardResolutionTargetPlusSky,
      GhostStandardResolutionSkyPlusTarget,
      GhostHighResolution
    )

    val asterismComboBox: ComboBox[AsterismType] = new ComboBox[AsterismType](asterismList)
    layout(asterismComboBox) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 1
      gridwidth = 2
      gridy = 2
      insets = new Insets(10, 0, 0, 20)
    }

    layout(new Label) = new Constraints() {
      anchor = Anchor.North
      gridy = 3
      weighty = 1.0
    }

    listenTo(asterismComboBox.selection)
    reactions += {
      case SelectionChanged(`asterismComboBox`) =>
        val converter = asterismComboBox.selection.item.converter.asScalaOpt
        converter.foreach(convertAsterism)
    }

    /** Convert the asterism to the new type, and set the new target environment. */
    def convertAsterism(converter: AsterismConverter): Unit = Swing.onEDT {
      // Disable the combo box, and enable it only if conversion succeeds.
      // If the conversion fails, the combo box will stay disabled and a P2 error will be generated.
      asterismComboBox.enabled = false

      for {
        oc  <- Option(getContextTargetObsComp)
        toc <- Option(oc.getDataObject).collect { case t: TargetObsComp => t }
        env <- converter.convert(toc.getTargetEnvironment)
      } {
        toc.setTargetEnvironment(env)
        oc.setDataObject(toc)
        asterismComboBox.enabled = true
      }
    }

    def initialize(): Unit = Swing.onEDT {
      // Set the combo box to the appropriate asterism type.
      // If there is no allowable type, disable it.
      deafTo(asterismComboBox.selection)
      ui.asterismComboBox.enabled = false

      // We only allow asterism types in the asterism list populating the combo box.
      val selection = getContextObservation.findTargetObsComp
        .map(_.getAsterism.asterismType)
        .filter(asterismList.contains)

      selection.foreach { at =>
        ui.asterismComboBox.selection.item = at
        ui.asterismComboBox.enabled = true
      }
      listenTo(asterismComboBox.selection)
    }
  }

  override def getWindow: JPanel = ui.peer

  override def handlePostDataObjectUpdate(dataObj: Ghost): Unit = Swing.onEDT {
    ui.posAngleCtrl.setBean(dataObj)
    ui.initialize()
  }
}