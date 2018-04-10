package jsky.app.ot.gemini.ghost

import java.beans.PropertyDescriptor
import javax.swing.JPanel

import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.shared.gui.bean.TextFieldPropertyCtrl
import edu.gemini.spModel.gemini.ghost.Ghost
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GhostStandardResTargets._
import edu.gemini.spModel.gemini.ghost.AsterismConverters._
import edu.gemini.spModel.gemini.ghost.GhostAsterism.{HighResolution, StandardResolution}
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.target.obsComp.TargetObsComp
import jsky.app.ot.gemini.editor.ComponentEditor

import scala.collection.immutable.ListMap
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

    /** A map between GHOST asterism names and converters to those types. */
    val asterismMap: Map[String, GhostAsterismConverter] = ListMap(
      "Single target" -> GhostSingleTargetConverter,
      "Dual target" -> GhostDualTargetConverter,
      "SRIFU1 target, SRIFU2 sky position" -> GhostTargetPlusSkyConverter,
      "SRIFU1 sky position, SRIFU2 target" -> GhostSkyPlusTargetConverter,
      "High resolution" -> GhostHighResolutionConverter
    )

    val asterismComboBox: ComboBox[String] = new ComboBox[String](asterismMap.keys.toSeq)
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
        val converter = asterismMap(asterismComboBox.selection.item)
        convertAsterism(converter)
    }

    /** Convert the asterism to the new type, and set the new target environment. */
    def convertAsterism(converter: GhostAsterismConverter): Unit = Swing.onEDT {
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
      deafTo(asterismComboBox.selection)
      ui.asterismComboBox.enabled = true
      val selectionIdx = getContextObservation.findTargetObsComp.flatMap { env =>
        env.getAsterism match {
          case a: StandardResolution =>
            a.targets match {
              case SingleTarget(_)     => Some(0)
              case DualTarget(_, _)    => Some(1)
              case TargetPlusSky(_, _) => Some(2)
              case SkyPlusTarget(_, _) => Some(3)
            }
          case HighResolution(_, _, _) => Some(4)
          case _                       =>
            Dialog.showMessage(ui, "Illegal asterism type for GHOST observation.",
              "Asterism Error", Dialog.Message.Error)
            ui.asterismComboBox.enabled = false
            None
        }
      }
      asterismComboBox.selection.index = selectionIdx.getOrElse(0)
      listenTo(asterismComboBox.selection)
    }
  }

  override def getWindow: JPanel = ui.peer

  override def handlePostDataObjectUpdate(dataObj: Ghost): Unit = Swing.onEDT {
    ui.posAngleCtrl.setBean(dataObj)
    ui.initialize()
  }
}