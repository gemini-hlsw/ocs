package jsky.app.ot.gemini.igrins2

import edu.gemini.pot.sp.{ISPObsComponent, SPComponentType}
import edu.gemini.shared.gui.bean.TextFieldPropertyCtrl
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.igrins2.Igrins2
import jsky.app.ot.OTOptions
import jsky.app.ot.gemini.editor.ComponentEditor
import jsky.app.ot.gemini.ghost.GhostEditor.LabelPadding
import jsky.app.ot.gemini.parallacticangle.PositionAnglePanel

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import javax.swing.JPanel
import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.{Alignment, GridBagPanel, Insets, Label, Separator, Swing}

class Igrins2Editor extends ComponentEditor[ISPObsComponent, Igrins2]{

  private object ui extends GridBagPanel {
    val updateParallacticAnglePCL: PropertyChangeListener = new PropertyChangeListener() {
      override def propertyChange(evt: PropertyChangeEvent): Unit = {
        posAnglePanel.updateParallacticControls()
      }
    }

    private var row = 0
    border = ComponentEditor.PANEL_BORDER

    /**
     * Position angle components.
     **/
    val posAngleLabel: Label = new Label("Position Angle:")
    posAngleLabel.horizontalAlignment = Alignment.Right
    layout(posAngleLabel) = new Constraints() {
      anchor = Anchor.NorthEast
      gridx = 0
      gridy = row
      insets = new Insets(3, 10, 0, Igrins2Editor.LabelPadding)
    }

//    val posAngleCtrl: TextFieldPropertyCtrl[Igrins2, java.lang.Double] = TextFieldPropertyCtrl.createDoubleInstance(Igrins2.POS_ANGLE_PROP, 1)
//    posAngleCtrl.setColumns(10)
//    layout(Component.wrap(posAngleCtrl.getTextField)) = new Constraints() {
//      anchor = Anchor.NorthWest
//      gridx = 1
//      gridy = row
//      insets = new Insets(0, 0, 0, LabelPadding)
//    }

    val posAngleUnits: Label = new Label("deg E of N")
    posAngleUnits.horizontalAlignment = Alignment.Left
    val posAnglePanel: PositionAnglePanel[Igrins2, Igrins2Editor] = PositionAnglePanel.apply[Igrins2, Igrins2Editor](SPComponentType.INSTRUMENT_IGNRIS2)
    layout(posAnglePanel) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 2
      gridy = row
      insets = new Insets(3, 0, 0, 0)
    }

    /** Eat up all remaining horizontal space in the form. **/
//    layout(new Label) = new Constraints() {
//      anchor = Anchor.West
//      gridx = 3
//      gridy = row
//      weightx = 1.0
//    }
//    row += 1
//
//    layout(new Separator()) = new Constraints() {
//      anchor = Anchor.West
//      fill = Fill.Horizontal
//      gridx = 0
//      gridy = row
//      gridwidth = 3
//      insets = new Insets(10, 0, 0, 0)
//    }
//    row += 1
  }

  /**
   * Return the window containing the editor.
   */
  override def getWindow: JPanel = ui.peer

  override def handlePostDataObjectUpdate(inst: Igrins2): Unit = Swing.onEDT {
//    ui.posAngleCtrl.setBean(inst)
    ui.posAnglePanel.init(this, Site.GN)
    val editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram, getContextObservation)
    ui.posAnglePanel.updateEnabledState(editable)

    inst.addPropertyChangeListener(Igrins2.POS_ANGLE_CONSTRAINT_PROP.getName, ui.updateParallacticAnglePCL)
  }

  override def handlePreDataObjectUpdate (inst: Igrins2): Unit = {
    Option(inst).foreach {inst =>

      inst.removePropertyChangeListener(Igrins2.POS_ANGLE_CONSTRAINT_PROP.getName, ui.updateParallacticAnglePCL)
    }
  }
}

object Igrins2Editor {
  val LabelPadding = 15
}
