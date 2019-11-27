package jsky.app.ot.gemini.ghost

import java.beans.PropertyDescriptor

import javax.swing.{DefaultComboBoxModel, JPanel}
import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.shared.gui.bean.{CheckboxPropertyCtrl, ComboPropertyCtrl, RadioPropertyCtrl, TextFieldPropertyCtrl}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.gemini.ghost.{AsterismTypeConverters, Ghost}
import edu.gemini.spModel.gemini.ghost.AsterismConverters._
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.target.env.{AsterismType, ResolutionMode}
import edu.gemini.spModel.target.env.AsterismType._
import edu.gemini.spModel.target.env.ResolutionMode._
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.telescope.IssPort
import jsky.app.ot.gemini.editor.ComponentEditor

import scala.collection.JavaConverters._
import scala.swing._
import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.TabbedPane.Page
import scala.swing.event.SelectionChanged


final class GhostEditor extends ComponentEditor[ISPObsComponent, Ghost] {

  private object ui extends GridBagPanel {
    private var row = 0
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
      gridx = 0
      gridy = row
      insets = new Insets(3, 10, 0, 20)
    }
    layout(tfComp) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 1
      gridy = row
      insets = new Insets(0, 0, 0, 20)
    }
    layout(posAngleUnits) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 2
      gridy = row
      insets = new Insets(3, 0, 0, 20)
    }
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

    /**
     * RESOLUTION MODE
     */
    val resolutionModeLabel: Label = new Label("Resolution Mode:")
    resolutionModeLabel.horizontalAlignment = Alignment.Right
    layout(resolutionModeLabel) = new Constraints() {
      anchor = Anchor.East
      gridx = 0
      gridy = row
      insets = new Insets(12, 10, 0, 20)
    }

    /** A list of available resolution modes. */
    val resolutionModes: List[ResolutionMode] = List(
      GhostStandard,
      GhostHigh,
      GhostPRV
    )

    val resolutionModeComboBox: ComboBox[ResolutionMode] = new ComboBox[ResolutionMode](resolutionModes)
    layout(resolutionModeComboBox) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 1
      gridy = row
      gridwidth = 2
      fill = Fill.Horizontal
      insets = new Insets(10, 0, 0, 20)
    }
    row += 1

    /**
     * Target mode
     */
    val targetModeLabel: Label = new Label("Target Mode:")
    targetModeLabel.horizontalAlignment = Alignment.Right
    layout(targetModeLabel) = new Constraints() {
      anchor = Anchor.East
      gridx = 0
      gridy = row
      insets = new Insets(12, 10, 0, 20)
    }

    /** A list of available asterism types. */
    val asterismList: List[AsterismType] = List(
      GhostSingleTarget,
      GhostDualTarget,
      GhostTargetPlusSky,
      GhostSkyPlusTarget,
      GhostHighResolutionTarget,
      GhostHighResolutionTargetPlusSky
    )

    val asterismComboBox: ComboBox[AsterismType] = new ComboBox[AsterismType](resolutionModeComboBox.selection.item.asterismTypes.asScala)
    layout(asterismComboBox) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 1
      gridwidth = 2
      gridy = row
      fill = Fill.Horizontal
      insets = new Insets(10, 0, 0, 20)
    }
    row += 1


    val tabPane = new TabbedPane

    /**
     * The tabbed pane containing:
     * 1. Target / guiding / binning information.
     */
    object targetPane extends GridBagPanel {
      var row = 0
      /**
       * Enable fiber agitator
       */
      val enableFiberAgitatorCtrl: CheckboxPropertyCtrl[Ghost] = new CheckboxPropertyCtrl[Ghost](Ghost.ENABLE_FIBER_AGITATOR_PROP)
      layout(Component.wrap(enableFiberAgitatorCtrl.getComponent)) = new Constraints() {
        anchor = Anchor.NorthWest
        gridx = 0
        gridy = row
        insets = new Insets(10, 10, 0, 20)
      }
      row += 1

      layout(new Separator()) = new Constraints() {
        anchor = Anchor.West
        fill = Fill.Horizontal
        gridx = 0
        gridy = row
        gridwidth = 2
        insets = new Insets(10, 10, 0, 0)
      }
      row += 1

      /**
       * IFU1 information.
       */
      val ifu1TargetName: Label = new Label("IFU1 Target")
      val ifu2TargetName: Label = new Label("IFU2 Target")

      //val ifu1SpectralBinning: ComboPropertyCtrl[Ghost, G]
      def createIFUPane(ifuNum: Int, ifuTargetName: Label): Panel = {
        val panel = new GridBagPanel
        var row = 0
        val ifuLabel: Label = new Label(f"IFU$ifuNum:")
        panel.layout(ifuLabel) = new panel.Constraints() {
          anchor = Anchor.NorthEast
          gridx = 0
          gridy = row
          insets = new Insets(3, 0, 0, 20)
        }

        /** Placeholder for name for target. **/
        panel.layout(ifuTargetName) = new panel.Constraints() {
          anchor = Anchor.NorthWest
          gridx = 1
          gridy = row
          insets = new Insets(3, 0, 0, 20)
        }
        row += 1

        /** Spectral binning. **/
        val spectralBinningLabel: Label = new Label("Spectral Binning:")
        panel.layout(spectralBinningLabel) = new panel.Constraints() {
          anchor = Anchor.NorthEast
          gridx = 0
          gridy = row
          insets = new Insets(3, 0, 0, 20)
        }

        /** Spatial binning. **/
        val spatialBinningLabel: Label = new Label("Spatial Binning:")
        panel.layout(spatialBinningLabel) = new panel.Constraints() {
          anchor = Anchor.NorthEast
          gridx = 2
          gridy = row
          insets = new Insets(3, 0, 0, 20)
        }
        row += 1

        /** Separator. */
        panel.layout(new Separator()) = new panel.Constraints() {
          anchor = Anchor.West
          fill = Fill.Horizontal
          gridx = 0
          gridy = row
          gridwidth = 4
          insets = new Insets(10, 10, 0, 0)
        }
        panel
      }
      row += 1

      val ifu1Pane = createIFUPane(1, ifu1TargetName)
      layout(ifu1Pane) = new Constraints() {
        anchor = Anchor.NorthWest
        gridx = 0
        gridy = row
        gridwidth = 4
        //gridheight = 3
        insets = new Insets(0, 10, 0, 0)
      }
      row += 1

      val ifu2Pane = createIFUPane(2, ifu2TargetName)
      layout(ifu2Pane) = new Constraints() {
        anchor = Anchor.NorthWest
        gridx = 0
        gridy = row
        gridwidth = 4
        //gridheight = 3
        insets = new Insets(0, 10, 0, 0)
      }

      /** Eat up the remaining horizontal space. **/
      layout(new Label) = new Constraints() {
        anchor = Anchor.West
        gridx = 2
        gridy = row
        weightx = 1.0
      }
    }
    tabPane.pages += new Page("Read Mode", targetPane)


    /**
     * The tabbed pane containing:
     * 2. Up / side looking selection.
     */
    val portCtrl: RadioPropertyCtrl[Ghost, IssPort] = new RadioPropertyCtrl[Ghost, IssPort](Ghost.PORT_PROP, true)
    tabPane.pages += new Page("ISS Port", makeTabPane(Component.wrap(portCtrl.getComponent)))

    layout(tabPane) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 0
      gridy = row
      gridwidth = 4
      gridheight = 1
      weightx = 1.0
      weighty = 0
      fill = Fill.Horizontal
      insets = new Insets(10, 0, 0, 0)
    }

    /**
     * Eats up the blank space at the bottom of the form.
     */

    layout(new Label) = new Constraints() {
      anchor = Anchor.North
      gridx = 0
      gridy = row
      weighty = 1.0
    }

    /**
     * A panel to house one component in a tab.
     */
    private def makeTabPane(component: Component, addGlue: Boolean = true): Panel = {
      var panel = new BoxPanel(Orientation.Vertical)
      panel.border = ComponentEditor.TAB_PANEL_BORDER
      panel.contents += component
      if (addGlue)
        panel.contents += Swing.VGlue
      panel
    }

    /**
     * When the resolution mode changes, change the target modes.
     * We try to change the target mode to the closest mode in the newly selected resolution to maintain
     * as much of the target version as possible.
     */
    listenTo(resolutionModeComboBox.selection)
    reactions += {
      case SelectionChanged(`resolutionModeComboBox`) =>
        // The old resolution mode.
        deafTo(resolutionModeComboBox.selection)
        deafTo(asterismComboBox.selection)

        // The old resolution mode.
        val originalResolutionMode = resolutionMode

        // The new resolution mode.
        val newResolutionMode = resolutionModeComboBox.selection.item

        // The asterism type.
        val asterismType = asterismComboBox.selection.item

        // If they are not the same, convert the asterism type from the old resolution mode
        // to the new resolution mode and store.
        if (originalResolutionMode != newResolutionMode) {
          val newAsterismType = AsterismTypeConverters.asterismTypeConverters((originalResolutionMode, asterismType, newResolutionMode))
          newAsterismType.converter.asScalaOpt.foreach(convertAsterism)

          // Update the contents of the asterism combo box to only allow asterisms of this resolution mode
          // and make sure we are at the selected mode.
          // We need to set a new model: we cannot modify the model directly as per Scala Swing.
          asterismComboBox.peer.setModel(new DefaultComboBoxModel[AsterismType]())
          newResolutionMode.asterismTypes.asScala.foreach(asterismComboBox.peer.addItem)
          asterismComboBox.selection.item = newAsterismType
        }

        listenTo(asterismComboBox.selection)
        listenTo(resolutionModeComboBox.selection)
    }

    /**
     * Now when the asterism type changes, change the target environment, trying to maintain as
     * much of the original target environment as we can.
     */
    listenTo(asterismComboBox.selection)
    reactions += {
      case SelectionChanged(`asterismComboBox`) =>
        val converter = asterismComboBox.selection.item.converter.asScalaOpt
        converter.foreach(convertAsterism)
    }

    def resolutionMode: ResolutionMode =
      getContextObservation.findTargetObsComp.map(_.getAsterism.resolutionMode).getOrElse(ResolutionMode.GhostStandard)

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
      deafTo(resolutionModeComboBox.selection)
      deafTo(asterismComboBox.selection)
      ui.asterismComboBox.enabled = false
      ui.resolutionModeComboBox.enabled = false

      // We need the resolution mode to set the combo box.
      ui.resolutionModeComboBox.selection.item = resolutionMode

      // We only allow asterism types in the asterism list populating the combo box.
      val selection = getContextObservation.findTargetObsComp
        .map(_.getAsterism.asterismType)
        .filter(asterismList.contains)

      // Set the asterism types to match those available to the resolution mode.
      // We need to set a new model: we cannot modify the model directly as per Scala Swing.
      asterismComboBox.peer.setModel(new DefaultComboBoxModel[AsterismType]())
      resolutionMode.asterismTypes.asScala.foreach(asterismComboBox.peer.addItem)
      ui.asterismComboBox.selection.item = selection.getOrElse(resolutionMode.asterismTypes.asScala.head)

      ui.resolutionModeComboBox.enabled = true
      ui.asterismComboBox.enabled = true
      listenTo(asterismComboBox.selection)
      listenTo(resolutionModeComboBox.selection)
    }
  }

  override def getWindow: JPanel = ui.peer

  override def handlePostDataObjectUpdate(dataObj: Ghost): Unit = Swing.onEDT {
    ui.posAngleCtrl.setBean(dataObj)
    ui.portCtrl.setBean(dataObj)
    ui.targetPane.enableFiberAgitatorCtrl.setBean(dataObj)
    ui.initialize()
  }
}