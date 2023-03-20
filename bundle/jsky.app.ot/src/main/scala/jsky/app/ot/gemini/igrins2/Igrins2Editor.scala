package jsky.app.ot.gemini.igrins2

import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.shared.gui.bean.TextFieldPropertyCtrl
import edu.gemini.spModel.gemini.igrins2.Igrins2
import jsky.app.ot.gemini.editor.ComponentEditor
import jsky.app.ot.gemini.ghost.GhostEditor.LabelPadding

import javax.swing.JPanel
import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.{Alignment, Component, GridBagPanel, Insets, Label, Separator, Swing}

class Igrins2Editor extends ComponentEditor[ISPObsComponent, Igrins2]{
  private object ui extends GridBagPanel {

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
      insets = new Insets(3, 10, 0, LabelPadding)
    }

    val posAngleCtrl: TextFieldPropertyCtrl[Igrins2, java.lang.Double] = TextFieldPropertyCtrl.createDoubleInstance(Igrins2.POS_ANGLE_PROP, 1)
    posAngleCtrl.setColumns(10)
    layout(Component.wrap(posAngleCtrl.getTextField)) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 1
      gridy = row
      insets = new Insets(0, 0, 0, LabelPadding)
    }

    val posAngleUnits: Label = new Label("deg E of N")
    posAngleUnits.horizontalAlignment = Alignment.Left
    layout(posAngleUnits) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 2
      gridy = row
      insets = new Insets(3, 0, 0, 0)
    }

    /** Eat up all remaining horizontal space in the form. **/
    layout(new Label) = new Constraints() {
      anchor = Anchor.West
      gridx = 3
      gridy = row
      weightx = 1.0
    }
    row += 1

    layout(new Separator()) = new Constraints() {
      anchor = Anchor.West
      fill = Fill.Horizontal
      gridx = 0
      gridy = row
      gridwidth = 3
      insets = new Insets(10, 0, 0, 0)
    }
    row += 1
  }

  /**
   * Return the window containing the editor.
   */
  override def getWindow: JPanel = ui.peer

  override def handlePostDataObjectUpdate(dataObj: Igrins2): Unit = Swing.onEDT {
    ui.posAngleCtrl.setBean(dataObj)
  }
}
