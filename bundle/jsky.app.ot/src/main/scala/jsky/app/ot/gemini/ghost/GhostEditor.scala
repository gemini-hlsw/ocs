package jsky.app.ot.gemini.ghost

import java.awt.Font
import java.util.logging.Logger

import com.jgoodies.forms.factories.DefaultComponentFactory
import javax.swing.{DefaultComboBoxModel, JPanel, JSpinner}
import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.shared.gui.bean.{CheckboxPropertyCtrl, ComboPropertyCtrl, RadioPropertyCtrl, TextFieldPropertyCtrl}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.gemini.ghost.{AsterismTypeConverters, Ghost, GhostAsterism, GhostBinning, GhostReadNoiseGain}
import edu.gemini.spModel.gemini.ghost.AsterismConverters._
import edu.gemini.spModel.gemini.ghost.GhostAsterism._
import edu.gemini.spModel.gemini.ghost.GhostAsterism.HighResolution._
import edu.gemini.spModel.gemini.ghost.GhostAsterism.StandardResolution._
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.target.SPCoordinates
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
import scala.swing.event.{ButtonClicked, SelectionChanged}
import scalaz._
import Scalaz._
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GuideFiberState.Enabled
import jsky.app.ot.editor.SpinnerEditor


final class GhostEditor extends ComponentEditor[ISPObsComponent, Ghost] {
  val LOG: Logger = Logger.getLogger(getClass.getName)

  private object ui extends GridBagPanel {
    import GhostEditor._

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

    val posAngleCtrl: TextFieldPropertyCtrl[Ghost, java.lang.Double] = TextFieldPropertyCtrl.createDoubleInstance(Ghost.POS_ANGLE_PROP, 1)
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

    /**
     * Resolution Mode.
     */
    val resolutionModeLabel: Label = new Label("Resolution Mode:")
    resolutionModeLabel.horizontalAlignment = Alignment.Right
    layout(resolutionModeLabel) = new Constraints() {
      anchor = Anchor.East
      gridx = 0
      gridy = row
      insets = new Insets(12, 10, 0, LabelPadding)
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
      insets = new Insets(10, 0, 0, 0)
    }
    row += 1

    /**
     * Target mode.
     */
    val targetModeLabel: Label = new Label("Target Mode:")
    targetModeLabel.horizontalAlignment = Alignment.Right
    layout(targetModeLabel) = new Constraints() {
      anchor = Anchor.East
      gridx = 0
      gridy = row
      insets = new Insets(12, 10, 0, LabelPadding)
    }

    /** A list of available asterism types. */
    val asterismList: List[AsterismType] = List(
      GhostSingleTarget,
      GhostDualTarget,
      GhostTargetPlusSky,
      GhostSkyPlusTarget,
      GhostHighResolutionTargetPlusSky
    )

    val asterismComboBox: ComboBox[AsterismType] = new ComboBox[AsterismType](resolutionModeComboBox.selection.item.asterismTypes.asScala)
    layout(asterismComboBox) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 1
      gridy = row
      gridwidth = 2
      fill = Fill.Horizontal
      insets = new Insets(10, 0, 0, 0)
    }
    row += 1


    /*****************
     *** Detectors ***
     *****************/
    object detectorUI extends GridBagPanel {
      var row = 0
      layout(Component.wrap(DefaultComponentFactory.getInstance.createSeparator("Red Camera Detector"))) = new Constraints() {
        gridx = 0
        gridy = row
        gridwidth = 7
        anchor = Anchor.West
        fill = Fill.Horizontal
        insets = new Insets(15, 0, 10, 0)
      }
      row += 1

      val redExpTimeLabel = new Label("Red Exposure Time:")
      redExpTimeLabel.horizontalAlignment = Alignment.Right
      layout(redExpTimeLabel) = new Constraints() {
        anchor = Anchor.NorthEast
        gridx = 0
        gridy = row
        insets = new Insets(3, 30, 0, LabelPadding)
      }

      val redExpTimeCtrl: TextFieldPropertyCtrl[Ghost, java.lang.Double] = TextFieldPropertyCtrl.createDoubleInstance(Ghost.RED_EXPOSURE_TIME_PROP, 1)
      redExpTimeCtrl.setColumns(10)
      layout(Component.wrap(redExpTimeCtrl.getTextField)) = new Constraints() {
        anchor = Anchor.NorthWest
        gridx = 1
        gridy = row
        insets = new Insets(0, 0, 0, LabelPadding)
      }

      val redExpTimeUnits = new Label("sec")
      redExpTimeUnits.horizontalAlignment = Alignment.Left
      layout(redExpTimeUnits) = new Constraints() {
        anchor = Anchor.NorthWest
        gridx = 2
        gridy = row
        insets = new Insets(3, 0, 0, 20)
      }

      val redBinningLabel = new Label("Spectral / Spatial Binning:")
      redBinningLabel.horizontalAlignment = Alignment.Right
      layout(redBinningLabel) = new Constraints() {
        anchor = Anchor.NorthEast
        gridx = 3
        gridy = row
        insets = new Insets(3, 10, 0, LabelPadding)
      }

      val redBinning: ComboPropertyCtrl[Ghost, GhostBinning] = ComboPropertyCtrl.enumInstance(Ghost.RED_BINNING_PROP)
      layout(Component.wrap(redBinning.getComponent)) = new Constraints() {
        anchor = Anchor.NorthWest
        gridx = 4
        gridy = row
        fill = Fill.Horizontal
        insets = new Insets(0, 0, 0, 0)
      }
      row += 1

      val redCountLabel = new Label("Red Exposure Count:")
      redCountLabel.horizontalAlignment = Alignment.Right
      layout(redCountLabel) = new Constraints() {
        anchor = Anchor.NorthEast
        gridx = 0
        gridy = row
        fill = Fill.Horizontal
        insets = new Insets(3, 30, 0, LabelPadding)
      }
      val redCountSpinner: JSpinner = new JSpinner()
      layout(Component.wrap(redCountSpinner)) = new Constraints() {
        anchor = Anchor.NorthWest
        gridx = 1
        gridy = row
        insets = new Insets(0, 0, 0, LabelPadding)
      }
      redCountSpinner.setPreferredSize(redExpTimeCtrl.getComponent.getPreferredSize)
      redCountSpinner.setMinimumSize(redExpTimeCtrl.getComponent.getMinimumSize)
      val redCountSpinnerEditor: SpinnerEditor = new SpinnerEditor(redCountSpinner, new SpinnerEditor.Functions() {
        override def getValue: Int = getDataObject.getRedExposureCount
        override def setValue(newValue: Int): Unit = {
          getDataObject.setRedExposureCount(newValue)
        }
      })
      val redCountUnits = new Label("X")
      redCountUnits.horizontalAlignment = Alignment.Left
      layout(redCountUnits) = new Constraints() {
        anchor = Anchor.NorthWest
        gridx = 2
        gridy = row
        insets = new Insets(3, 0, 0, 20)
      }
      row += 1

      val redReadNoiseGain: RadioPropertyCtrl[Ghost, GhostReadNoiseGain] = new RadioPropertyCtrl[Ghost, GhostReadNoiseGain](Ghost.RED_READ_NOISE_GAIN_PROP)
      layout(Component.wrap(redReadNoiseGain.getComponent)) = new Constraints() {
        anchor = Anchor.NorthWest
        gridx = 0
        gridy = row
        gridwidth = 7
        fill = Fill.Horizontal
        insets = new Insets(15, 30, 0, 0)
      }
      row += 1

      layout(Component.wrap(DefaultComponentFactory.getInstance.createSeparator("Blue Camera Detector"))) = new Constraints() {
        gridx = 0
        gridy = row
        gridwidth = 7
        anchor = Anchor.West
        fill = Fill.Horizontal
        insets = new Insets(15, 0, 10, 0)
      }
      row += 1

      val blueExpTimeLabel = new Label("Blue Exposure Time:")
      blueExpTimeLabel.horizontalAlignment = Alignment.Right
      layout(blueExpTimeLabel) = new Constraints() {
        anchor = Anchor.NorthEast
        gridx = 0
        gridy = row
        insets = new Insets(3, 30, 0, LabelPadding)
      }
      val blueExpTimeCtrl: TextFieldPropertyCtrl[Ghost, java.lang.Double] = TextFieldPropertyCtrl.createDoubleInstance(Ghost.BLUE_EXPOSURE_TIME_PROP, 1)
      blueExpTimeCtrl.setColumns(10)
      layout(Component.wrap(blueExpTimeCtrl.getTextField)) = new Constraints() {
        anchor = Anchor.NorthWest
        gridx = 1
        gridy = row
        insets = new Insets(0, 0, 0, LabelPadding)
      }
      val blueExpTimeUnits = new Label("sec")
      blueExpTimeUnits.horizontalAlignment = Alignment.Left
      layout(blueExpTimeUnits) = new Constraints() {
        anchor = Anchor.NorthWest
        gridx = 2
        gridy = row
        insets = new Insets(3, 0, 0, 20)
      }

      val blueBinningLabel = new Label("Spectral / Spatial Binning:")
      blueBinningLabel.horizontalAlignment = Alignment.Right
      layout(blueBinningLabel) = new Constraints() {
        anchor = Anchor.NorthEast
        gridx = 3
        gridy = row
        insets = new Insets(3, 10, 0, LabelPadding)
      }

      val blueBinning: ComboPropertyCtrl[Ghost, GhostBinning] = ComboPropertyCtrl.enumInstance(Ghost.BLUE_BINNING_PROP)
      layout(Component.wrap(blueBinning.getComponent)) = new Constraints() {
        anchor = Anchor.NorthWest
        gridx = 4
        gridy = row
        fill = Fill.Horizontal
        insets = new Insets(2, 0, 0, 0)
      }
      row += 1

      val blueCountLabel = new Label("Blue Exposure Count:")
      blueCountLabel.horizontalAlignment = Alignment.Right
      layout(blueCountLabel) = new Constraints() {
        anchor = Anchor.NorthEast
        gridx = 0
        gridy = row
        fill = Fill.Horizontal
        insets = new Insets(3, 30, 0, LabelPadding)
      }
      val blueCountSpinner: JSpinner = new JSpinner()
      layout(Component.wrap(blueCountSpinner)) = new Constraints() {
        anchor = Anchor.NorthWest
        gridx = 1
        gridy = row
        insets = new Insets(0, 0, 0, LabelPadding)
      }
      blueCountSpinner.setPreferredSize(blueExpTimeCtrl.getComponent.getPreferredSize)
      blueCountSpinner.setMinimumSize(blueExpTimeCtrl.getComponent.getMinimumSize)
      val blueCountSpinnerEditor: SpinnerEditor = new SpinnerEditor(blueCountSpinner, new SpinnerEditor.Functions() {
        override def getValue: Int = getDataObject.getBlueExposureCount
        override def setValue(newValue: Int): Unit = {
          getDataObject.setBlueExposureCount(newValue)
        }
      })
      val blueCountUnits = new Label("X")
      blueCountUnits.horizontalAlignment = Alignment.Left
      layout(blueCountUnits) = new Constraints() {
        anchor = Anchor.NorthWest
        gridx = 2
        gridy = row
        insets = new Insets(3, 0, 0, 20)
      }
      row += 1

      val blueReadNoiseGain: RadioPropertyCtrl[Ghost, GhostReadNoiseGain] = new RadioPropertyCtrl[Ghost, GhostReadNoiseGain](Ghost.BLUE_READ_NOISE_GAIN_PROP)
      layout(Component.wrap(blueReadNoiseGain.getComponent)) = new Constraints() {
        anchor = Anchor.NorthWest
        gridx = 0
        gridy = row
        gridwidth = 7
        fill = Fill.Horizontal
        insets = new Insets(15, 30, 0, 0)
      }
      row += 1
    }

    layout(detectorUI) = new Constraints() {
      anchor = Anchor.NorthWest
      gridx = 0
      gridy = row
      gridwidth = 4
      insets = new Insets(10, 0, 0, 20)
    }
    row += 1

    /**
     * TABS.
     */
    val tabPane = new TabbedPane

    /**
     * The tabbed pane containing:
     * 1. Target / guiding / binning information.
     */
    object targetPane extends GridBagPanel {
      var row = 0
      /**
       * Enable fiber agitators
       */
      val fiberAgitatorPanel = new GridBagPanel
      val enableFiberAgitator1Ctrl: CheckboxPropertyCtrl[Ghost] = new CheckboxPropertyCtrl[Ghost](Ghost.ENABLE_FIBER_AGITATOR_1_PROP)
      fiberAgitatorPanel.layout(Component.wrap(enableFiberAgitator1Ctrl.getComponent)) = new fiberAgitatorPanel.Constraints() {
        anchor = Anchor.NorthWest
        gridx = 0
        gridy = row
        insets = new Insets(10, 10, 0, 0)
      }
      val enableFiberAgitator2Ctrl: CheckboxPropertyCtrl[Ghost] = new CheckboxPropertyCtrl[Ghost](Ghost.ENABLE_FIBER_AGITATOR_2_PROP)
      fiberAgitatorPanel.layout(Component.wrap(enableFiberAgitator2Ctrl.getComponent)) = new fiberAgitatorPanel.Constraints() {
        anchor = Anchor.NorthWest
        gridx = 1
        gridy = row
        insets = new Insets(10, 40, 0, 0)
      }
      fiberAgitatorPanel.layout(new Label) = new fiberAgitatorPanel.Constraints() {
        fill = Fill.Horizontal
        anchor = Anchor.West
        gridx = 2
        gridy = row
        weightx = 1.0
      }
      layout(fiberAgitatorPanel) = new Constraints() {
        fill = Fill.Horizontal
        anchor = Anchor.NorthWest
        gridx = 0
        gridy = row
        gridwidth = 3
        weightx = 1.0
        insets = new Insets(0, 10, 0, 0)
      }
      row += 1

      /**
       * IFU1 information.
       */
      val ifu1TargetName: Label = new Label("IFU1 Target")
      val ifu2TargetName: Label = new Label("IFU2 Target")
      val ifu1GuideFiberCheckBox: CheckBox = new CheckBox()
      val ifu2GuideFiberCheckBox: CheckBox = new CheckBox()

      def createIFUPane(ifuNum: Int, ifuTargetName: Label, ifuGuideFiberCheckBox: CheckBox): Panel = {
        val panel = new GridBagPanel
        var row = 0

        /** Separator. */
        panel.layout(new Separator()) = new panel.Constraints() {
          anchor = Anchor.West
          fill = Fill.Horizontal
          gridx = 0
          gridy = row
          gridwidth = 3
          weightx = 1.0
          insets = new Insets(10, 10, 0, 0)
        }
        row += 1

        val ifuLabel: Label = new Label(f"IFU$ifuNum Target:")
        val font: Font = ifuLabel.font
        ifuLabel.font = font.deriveFont(font.getStyle | Font.BOLD)

        panel.layout(ifuLabel) = new panel.Constraints() {
          anchor = Anchor.NorthEast
          gridx = 0
          gridy = row
          insets = new Insets(3, 0, 0, LabelPadding)
        }

        /** Placeholder for name for target. **/
        panel.layout(ifuTargetName) = new panel.Constraints() {
          anchor = Anchor.NorthWest
          gridx = 1
          gridy = row
          insets = new Insets(3, 0, 0, 0)
        }

        /** Eat up all remaining horizontal space in the form. **/
        layout(new Label) = new Constraints() {
          fill = Fill.Horizontal
          anchor = Anchor.West
          gridx = 2
          gridy = row
          weightx = 1.0
        }
        row += 1

        /** OIWFS guide star. */
        panel.layout(new Label("Enable Guide Fibers:")) = new panel.Constraints() {
          anchor = Anchor.NorthEast
          gridx = 0
          gridy = row
          insets = new Insets(3, 0, 0, LabelPadding)
        }

        panel.layout(ifuGuideFiberCheckBox) = new panel.Constraints() {
          anchor = Anchor.West
          gridx = 1
          gridy = row
          insets = new Insets(3, 0, 0, 0)
        }
        row += 1
        panel
      }
      row += 1

      val ifu1Pane: Panel = createIFUPane(1, ifu1TargetName, ifu1GuideFiberCheckBox)
      layout(ifu1Pane) = new Constraints() {
        fill = Fill.Horizontal
        anchor = Anchor.NorthWest
        gridx = 0
        gridy = row
        gridwidth = 3
        weightx = 1.0
        insets = new Insets(0, 10, 0, 0)
      }
      row += 1

      val ifu2Pane: Panel = createIFUPane(2, ifu2TargetName, ifu2GuideFiberCheckBox)
      layout(ifu2Pane) = new Constraints() {
        fill = Fill.Horizontal
        anchor = Anchor.NorthWest
        gridx = 0
        gridy = row
        gridwidth = 3
        weightx = 1.0
        insets = new Insets(0, 10, 0, 0)
      }

      /** Eat up the remaining horizontal space. **/
      layout(new Label) = new Constraints() {
        anchor = Anchor.West
        gridx = 0
        gridy = row
        weightx = 1.0
      }
    }
    tabPane.pages += new Page("IFUs", targetPane)

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
      fill = Fill.Horizontal
      insets = new Insets(30, 0, 0, 0)
    }
    row += 1

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
      case SelectionChanged(`resolutionModeComboBox`) => Swing.onEDT {
        // The old resolution mode.
        deafTo(resolutionModeComboBox.selection)
        deafTo(asterismComboBox.selection)

        // The old resolution mode.
        val originalResolutionMode = resolutionMode

        // The new resolution mode.
        val newResolutionMode = resolutionModeComboBox.selection.item

        // The asterism type.
        val asterismType = asterismComboBox.selection.item

        // Update the contents of the asterism combo box to only allow asterisms of this resolution mode
        // and make sure we are at the selected mode.
        // We need to set a new model: we cannot modify the model directly as per Scala Swing.
        asterismComboBox.peer.setModel(new DefaultComboBoxModel[AsterismType]())
        newResolutionMode.asterismTypes.asScala.foreach(asterismComboBox.peer.addItem)

        // If they are not the same, convert the asterism type from the old resolution mode
        // to the new resolution mode and store.
        if (originalResolutionMode != newResolutionMode) {
          val newAsterismType = AsterismTypeConverters.asterismTypeConverters((originalResolutionMode, asterismType, newResolutionMode))

          // If we find ourselves in an inconsistent state with regards to resolution mode and asterism type, default back to standard mode, ghost single
          // target to try to preserve as much information as possible and log a severe error message.
          if (newAsterismType.isEmpty) {
            LOG.severe(s"GHOST observation has incompatible resolution type ${originalResolutionMode.displayName} and asterism type ${asterismType.displayName}." +
              "\n\tResetting to standard mode with single target.")
            resolutionModeComboBox.selection.item = GhostStandard
          }
          val nat = newAsterismType.getOrElse(GhostSingleTarget)
          nat.converter.asScalaOpt.foreach(convertAsterism)
          asterismComboBox.selection.item = nat
        }
        listenTo(asterismComboBox.selection)
        listenTo(resolutionModeComboBox.selection)
      }
    }

    /**
     * Now when the asterism type changes, change the target environment, trying to maintain as
     * much of the original target environment as we can.
     */
    listenTo(asterismComboBox.selection)
    reactions += {
      case SelectionChanged(`asterismComboBox`) => Swing.onEDT {
        val converter = asterismComboBox.selection.item.converter.asScalaOpt
        converter.foreach(convertAsterism)
      }
    }

    def resolutionMode: ResolutionMode =
      getContextObservation.findTargetObsComp.map(_.getAsterism.resolutionMode).getOrElse(ResolutionMode.GhostStandard)

    /** Convert the asterism to the new type, and set the new target environment. */
    def convertAsterism(converter: AsterismConverter): Unit = Swing.onEDT {
      // Disable the combo box, and enable it only if conversion succeeds.
      // If the conversion fails, the combo box will stay disabled and a P2 error will be generated.
      asterismComboBox.enabled = false

      for {
        oc <- Option(getContextTargetObsComp)
        toc <- Option(oc.getDataObject).collect { case t: TargetObsComp => t }
        env <- converter.convert(toc.getTargetEnvironment)
      } {
        toc.setTargetEnvironment(env)
        oc.setDataObject(toc)
        asterismComboBox.enabled = true
        initIFUs()
      }
    }

    reactions += {
      case ButtonClicked(targetPane.ifu1GuideFiberCheckBox) =>
        changeIFU1GuideFiberState(gfs(targetPane.ifu1GuideFiberCheckBox))
      case ButtonClicked(targetPane.ifu2GuideFiberCheckBox) =>
        changeIFU2GuideFiberState(gfs(targetPane.ifu2GuideFiberCheckBox))
    }

    /**
     * When the guide fiber state changes, we must change the asterism type to reflect this.
     */
    // Get the state from the checkbox.
    def gfs(box: CheckBox): GuideFiberState =
      if (box.selected) GuideFiberState.Enabled
      else GuideFiberState.Disabled

    /**
     * Change the guide fiber type of the IFU1s.
     */
    def changeIFU1GuideFiberState(state: GuideFiberState): Unit = Swing.onEDT {
      for {
        oc <- Option(getContextTargetObsComp)
        toc <- Option(oc.getDataObject).collect { case t: TargetObsComp => t }
      } {
        val env = toc.getTargetEnvironment
        toc.getTargetEnvironment.getAsterism match {
          case a: SingleTarget => toc.setTargetEnvironment(env.setAsterism((SingleTargetIFU1 >=> GhostTarget.guideFiberState).set(a, state)))
          case a: DualTarget => toc.setTargetEnvironment(env.setAsterism((DualTargetIFU1 >=> GhostTarget.guideFiberState).set(a, state)))
          case a: TargetPlusSky => toc.setTargetEnvironment(env.setAsterism((TargetPlusSkyIFU1 >=> GhostTarget.guideFiberState).set(a, state)))
          case a: HighResolutionTargetPlusSky => toc.setTargetEnvironment(env.setAsterism((HRTargetPlusSkyIFU1 >=> GhostTarget.guideFiberState).set(a, state)))
        }
        oc.setDataObject(toc)
      }
    }

    def changeIFU2GuideFiberState(state: GuideFiberState): Unit = Swing.onEDT {
      for {
        oc <- Option(getContextTargetObsComp)
        toc <- Option(oc.getDataObject).collect { case t: TargetObsComp => t }
      } {
        val env = toc.getTargetEnvironment
        toc.getTargetEnvironment.getAsterism match {
          case a: DualTarget => toc.setTargetEnvironment(env.setAsterism((DualTargetIFU2 >=> GhostTarget.guideFiberState).set(a, state)))
          case a: SkyPlusTarget => toc.setTargetEnvironment(env.setAsterism((SkyPlusTargetIFU2 >=> GhostTarget.guideFiberState).set(a, state)))
        }
        oc.setDataObject(toc)
      }
    }

    reactions += {
      case ButtonClicked(targetPane.ifu1GuideFiberCheckBox) =>
        changeIFU1GuideFiberState(gfs(targetPane.ifu1GuideFiberCheckBox))
      case ButtonClicked(targetPane.ifu2GuideFiberCheckBox) =>
        changeIFU2GuideFiberState(gfs(targetPane.ifu2GuideFiberCheckBox))
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

    def initIFUs(): Unit = Swing.onEDT {
      val Sky = SPCoordinates.Name
      Option(getContextTargetEnv).foreach(env => {
        val asterism = env.getAsterism
        val (name1: String, name2Opt: Option[String]) = asterism match {
          case GhostAsterism.SingleTarget(gt, _)                   => (gt.spTarget.getName, None)
          case GhostAsterism.DualTarget(gt1, gt2, _)               => (gt1.spTarget.getName, Some(gt2.spTarget.getName))
          case GhostAsterism.TargetPlusSky(gt1, _, _)              => (gt1.spTarget.getName, Some(Sky))
          case GhostAsterism.SkyPlusTarget(_, gt2, _)              => (Sky, Some(gt2.spTarget.getName))
          case GhostAsterism.HighResolutionTargetPlusSky(gt, _, _) => (gt.spTarget.getName, Some(Sky))
          case _                                                   => sys.error("illegal asterism type")
        }
        targetPane.ifu1TargetName.text = name1
        name2Opt.foreach(name2 => {
          targetPane.ifu2TargetName.text = name2
        })
        targetPane.ifu2Pane.visible = name2Opt.isDefined

        val (guideFibers1: Boolean, editableGuideFibers1: Boolean, guideFibers2: Boolean, editableGuideFibers2: Boolean) = asterism match {
          case SingleTarget(gt, _)                                 => (gt.guideFiberState === Enabled, true, false, false)
          case GhostAsterism.DualTarget(gt1, gt2, _)               => (gt1.guideFiberState == Enabled, true, gt2.guideFiberState == Enabled, true)
          case GhostAsterism.TargetPlusSky(gt, _, _)               => (gt.guideFiberState === Enabled, true, false, false)
          case GhostAsterism.SkyPlusTarget(_, gt2, _)              => (false, false, gt2.guideFiberState === Enabled, true)
          case GhostAsterism.HighResolutionTargetPlusSky(gt, _, _) => (gt.guideFiberState === Enabled, true, false, false)
          case _                                                   => sys.error("illegal asterism type")
        }

        deafTo(targetPane.ifu1GuideFiberCheckBox)
        deafTo(targetPane.ifu2GuideFiberCheckBox)
        targetPane.ifu1GuideFiberCheckBox.selected = guideFibers1
        targetPane.ifu1GuideFiberCheckBox.enabled = editableGuideFibers1
        targetPane.ifu2GuideFiberCheckBox.selected = guideFibers2
        targetPane.ifu2GuideFiberCheckBox.enabled = editableGuideFibers2
        listenTo(targetPane.ifu1GuideFiberCheckBox)
        listenTo(targetPane.ifu2GuideFiberCheckBox)
      })
    }
  }

  override def getWindow: JPanel = ui.peer

  override def handlePostDataObjectUpdate(dataObj: Ghost): Unit = Swing.onEDT {
    ui.posAngleCtrl.setBean(dataObj)
    ui.targetPane.enableFiberAgitator1Ctrl.setBean(dataObj)
    ui.targetPane.enableFiberAgitator2Ctrl.setBean(dataObj)
    ui.detectorUI.redExpTimeCtrl.setBean(dataObj)
    ui.detectorUI.redCountSpinnerEditor.init()
    ui.detectorUI.redBinning.setBean(dataObj)
    ui.detectorUI.redReadNoiseGain.setBean(dataObj)
    ui.detectorUI.blueExpTimeCtrl.setBean(dataObj)
    ui.detectorUI.blueCountSpinnerEditor.init()
    ui.detectorUI.blueBinning.setBean(dataObj)
    ui.detectorUI.blueReadNoiseGain.setBean(dataObj)
    ui.portCtrl.setBean(dataObj)
    ui.initialize()
    ui.initIFUs()
  }

  override def cleanup(): Unit = {
    super.cleanup()
    ui.detectorUI.redCountSpinnerEditor.cleanup()
    ui.detectorUI.blueCountSpinnerEditor.cleanup()
  }
}

object GhostEditor {
  val LabelPadding = 15
}