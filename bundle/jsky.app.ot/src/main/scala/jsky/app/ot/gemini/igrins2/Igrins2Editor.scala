package jsky.app.ot.gemini.igrins2

import edu.gemini.pot.sp.{ISPObsComponent, SPComponentType}
import edu.gemini.shared.gui.bean.TextFieldPropertyCtrl
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.igrins2.{Igrins2, Igrins2Geometry}
import edu.gemini.spModel.telescope.IssPort
import jsky.app.ot.OTOptions
import jsky.app.ot.gemini.editor.ComponentEditor
import jsky.app.ot.gemini.ghost.GhostEditor.LabelPadding
import jsky.app.ot.gemini.parallacticangle.PositionAnglePanel
import squants.time.TimeConversions.TimeConversions

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import javax.swing.JPanel
import javax.swing.event.{DocumentEvent, DocumentListener}
import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.event.ButtonClicked
import scala.swing.{Alignment, ButtonGroup, Component, FlowPanel, GridBagPanel, Insets, Label, RadioButton, Separator, Swing}

class Igrins2Editor extends ComponentEditor[ISPObsComponent, Igrins2]{

  private object ui extends GridBagPanel {
    val updateParallacticAnglePCL: PropertyChangeListener = new PropertyChangeListener() {
      override def propertyChange(evt: PropertyChangeEvent): Unit = {
        posAnglePanel.updateParallacticControls()
      }
    }

    private var row = 0
    border = ComponentEditor.PANEL_BORDER

    layout(new Label("Science FOV: ")) = new Constraints() {
      anchor = Anchor.West
      gridx = 0
      gridy = row
      weightx = 1.0
    }

    layout(new Label("Wavelength Coverage: ")) = new Constraints() {
      anchor = Anchor.West
      gridx = 1
      gridy = row
      weightx = 1.0
    }
    row += 1

    layout(new Label(s"${Igrins2Geometry.ScienceFovHeight.toArcseconds} x ${Igrins2Geometry.ScienceFovWidth.toArcseconds} arcsec" )) = new Constraints() {
      anchor = Anchor.West
      gridx = 0
      gridy = row
      weightx = 1.0
    }

    layout(new Label(s"${Igrins2.WavelengthCoverageLowerBound.toMicrons} - ${Igrins2.WavelengthCoverageUpperBound.toMicrons} μm" )) = new Constraints() {
      anchor = Anchor.West
      gridx = 1
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
    layout(new Label("Exposure Time (1.63 - 13000s) ")) = new Constraints() {
      anchor = Anchor.West
      gridx = 0
      gridy = row
      weightx = 1.0
    }
    layout(new Label("Fowler Samples")) = new Constraints() {
      anchor = Anchor.West
      gridx = 1
      gridy = row
      weightx = 1.0
    }
    row += 1

    val expTimeCtrl: TextFieldPropertyCtrl[Igrins2, java.lang.Double] = TextFieldPropertyCtrl.createDoubleInstance(Igrins2.EXPOSURE_TIME_PROP, 1)
    expTimeCtrl.setColumns(10)

    val expTimeUnits = new Label("sec")
    expTimeUnits.horizontalAlignment = Alignment.Left

    private val expTimePanel = new FlowPanel(Component.wrap(expTimeCtrl.getComponent), expTimeUnits)
    layout(expTimePanel) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 0
      gridy = row
      insets = new Insets(0, 0, 0, LabelPadding)
    }

    val fowlerSamples = new Label("-")
    layout(fowlerSamples) = new Constraints() {
      anchor = Anchor.West
      gridx = 1
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
    val upLookingButton = new RadioButton("Up-looking")
    val sideLookingButton = new RadioButton("Sidelooking")
    listenTo(upLookingButton, sideLookingButton)
    reactions += {
      case ButtonClicked(button) =>
        if (button == upLookingButton) {
          changeIssPort(IssPort.UP_LOOKING)
        }
        if (button == sideLookingButton) {
          changeIssPort(IssPort.SIDE_LOOKING)
        }
    }
    new ButtonGroup(upLookingButton, sideLookingButton)
    private val portPanel = new FlowPanel(new Label("ISS Port:"), upLookingButton, sideLookingButton)
    layout(portPanel) = new Constraints() {
      anchor = Anchor.West
      fill = Fill.Horizontal
      gridx = 0
      gridy = row
      gridwidth = 2
      insets = new Insets(10, 0, 0, 0)
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

    val posAngleUnits: Label = new Label("deg E of N")
    posAngleUnits.horizontalAlignment = Alignment.Left
    val posAnglePanel: PositionAnglePanel[Igrins2, Igrins2Editor] = PositionAnglePanel.apply[Igrins2, Igrins2Editor](SPComponentType.INSTRUMENT_IGNRIS2)
    layout(posAnglePanel) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 2
      gridy = row
      insets = new Insets(3, 0, 0, 0)
    }
  }

  def updateFowlerSamples(): Unit =
    ui.fowlerSamples.text = Igrins2.fowlerSamples(ui.expTimeCtrl.getBean.getExposureTime.seconds).toString
    ui.expTimeCtrl.getTextField.getDocument.addDocumentListener(new DocumentListener {
    override def insertUpdate(e: DocumentEvent): Unit =
      updateFowlerSamples()

    override def removeUpdate(e: DocumentEvent): Unit =
      updateFowlerSamples()

    override def changedUpdate(e: DocumentEvent): Unit =
      updateFowlerSamples()
  })

  def changeIssPort(port: IssPort): Unit = Swing.onEDT {
    getDataObject.setIssPort(port)
  }

  // Display the current ISS port settings
  def updateIssPort(): Unit = {
    val port = getDataObject.getIssPort
    if (port == IssPort.SIDE_LOOKING) ui.sideLookingButton.selected = true
    else if (port == IssPort.UP_LOOKING) ui.upLookingButton.selected = true
  }

  /**
   * Return the window containing the editor.
   */
  override def getWindow: JPanel = ui.peer

  override def handlePostDataObjectUpdate(inst: Igrins2): Unit = Swing.onEDT {
    ui.posAnglePanel.init(this, Site.GN)
    val editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram, getContextObservation)
    ui.posAnglePanel.updateEnabledState(editable)

    inst.addPropertyChangeListener(Igrins2.POS_ANGLE_CONSTRAINT_PROP.getName, ui.updateParallacticAnglePCL)
    ui.expTimeCtrl.setBean(inst)
    updateFowlerSamples()
    updateIssPort()
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
