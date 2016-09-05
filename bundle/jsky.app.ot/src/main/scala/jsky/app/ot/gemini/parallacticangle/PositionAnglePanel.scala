package jsky.app.ot.gemini.parallacticangle

import java.awt.CardLayout
import java.text.NumberFormat
import java.util.Locale

import edu.gemini.pot.sp.{ISPObsComponent, SPComponentType}
import edu.gemini.shared.gui.EnableDisableComboBox
import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.inst.ParallacticAngleSupport
import edu.gemini.spModel.obs.ObsClassService
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.telescope.{PosAngleConstraint, PosAngleConstraintAware}
import jsky.app.ot.editor.OtItemEditor
import jsky.app.ot.util.OtColor

import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing._
import scala.swing.event._
import scala.util.Try
import scalaz._
import Scalaz._

class PositionAnglePanel[I <: SPInstObsComp with PosAngleConstraintAware,
                         E <: OtItemEditor[ISPObsComponent, I]](instType: SPComponentType) extends GridBagPanel with Reactor {
  private var editor: Option[E] = None

  private val numberFormatter = NumberFormat.getInstance(Locale.US) <|
    (_.setMaximumFractionDigits(2)) <|
    (_.setMaximumIntegerDigits(3))

  private object ui {
    // We initialize the combo box will all possible items, and will modify this list as required.
    val positionAngleConstraintComboBox = new EnableDisableComboBox[PosAngleConstraint](PosAngleConstraint.values().toList)
    layout(positionAngleConstraintComboBox) = new Constraints() {
      anchor = Anchor.NorthWest
      insets = new Insets(0, 0, 0, 15)
    }
    listenTo(positionAngleConstraintComboBox.selection)
    reactions += {
      case SelectionChanged(`positionAngleConstraintComboBox`) =>
        positionAngleConstraintSelected()
    }


    object positionAngleTextField extends TextField {
      private val defaultBackground = background
      private val badBackground     = OtColor.LIGHT_SALMON

      peer.setColumns(6)
      minimumSize = preferredSize

      def angle: Option[Double] =
        Try { text.toDouble }.toOption

      override def validate(): Unit =
        background = angle.fold(if (positionAngleConstraintComboBox.selection.item == PosAngleConstraint.UNBOUNDED) background else badBackground)(_ => defaultBackground)
    }

    layout(positionAngleTextField) = new Constraints() {
      gridx  = 1
      anchor = Anchor.NorthWest
      insets = new Insets(0, 0, 0, 5)
    }

    // We want the parallactic angle controls to be notified every time the position angle changes.
    // A warning icon will be displayed if the two values are not the same according to the chosen formatter.
    listenTo(positionAngleTextField)
    listenTo(positionAngleTextField.keys)
    reactions += {
      case ValueChanged(`positionAngleTextField`) =>
        ui.parallacticAngleControlsOpt.foreach(_.positionAngleChanged(positionAngleTextField.text))
        ui.positionAngleTextField.validate()
        copyPosAngleToInstrument()

      case FocusGained(`positionAngleTextField`, _, _) | FocusLost(`positionAngleTextField`, _, _) =>
        copyPosAngleToTextField()

      case KeyPressed(`positionAngleTextField`, Key.Enter, _, _) =>
        copyPosAngleToTextField()
    }



    private object positionAngleTextFieldLabel extends Label("\u00baE of N") {
      horizontalAlignment = Alignment.Left
    }

    layout(positionAngleTextFieldLabel) = new Constraints() {
      anchor = Anchor.West
      gridx = 2
      weightx = 1.0
      fill = Fill.Horizontal
    }


    // Parallactic angle controls, if needed. This is an ugly hack because we only know if an instrument
    // supports parallactic angle by its type.
    val parallacticAngleControlsOpt = {
      val supportsParallacticAngle = Set(SPComponentType.INSTRUMENT_FLAMINGOS2,
                                         SPComponentType.INSTRUMENT_GMOS,
                                         SPComponentType.INSTRUMENT_GMOSSOUTH,
                                         SPComponentType.INSTRUMENT_GNIRS).contains(instType)

      if (supportsParallacticAngle) {
        val parallacticAngleControls = new ParallacticAngleControls(true)

        listenTo(parallacticAngleControls)
        reactions += {
          case ParallacticAngleControls.ParallacticAngleChangedEvent =>
            parallacticAngleChanged(parallacticAngleControls.parallacticAngle)
        }
        Some(parallacticAngleControls)
      }
      else None
    }

    // Position angle feedback label.
    object positionAngleFeedback extends Label

    // The lower panel, which contains the active controls depending on the choice of the position angle constraint.
    // This is ugly, but there is no equivalent to a CardLayout in Scala, so we need to work with underlying Java peers.
    object controlsPanel extends Panel {
      private val cardLayout = new CardLayout()
      peer.setLayout(cardLayout)

      private object parallacticAngleControlsPanel extends BorderPanel {
        val cardId = "ParallacticAngleControlsPanel"
        parallacticAngleControlsOpt.foreach(p => layout(p) = BorderPanel.Position.Center)
      }
      peer.add(parallacticAngleControlsPanel.peer, parallacticAngleControlsPanel.cardId)

      private object positionAngleFeedbackPanel extends Panel {
        val cardId = "PositionAngleFeedbackPanel"
        layout(positionAngleFeedback) = new Constraints() {
          anchor = Anchor.NorthWest
        }
      }
      peer.add(positionAngleFeedbackPanel.peer, positionAngleFeedbackPanel.cardId)

      def showParallacticAngleControls(): Unit =
        cardLayout.show(this.peer, parallacticAngleControlsPanel.cardId)

      def showPositionAngleFeedback(): Unit =
        cardLayout.show(this.peer, positionAngleFeedbackPanel.cardId)

      // Convenience method to set the appropriate card.
      def updatePanel(): Unit = editor.foreach { _.getDataObject.getPosAngleConstraint match {
          case PosAngleConstraint.PARALLACTIC_ANGLE => showParallacticAngleControls()
          case _                                    => showPositionAngleFeedback()
        }
      }
    }

    layout(controlsPanel) = new Constraints() {
      gridy = 1
      gridwidth = 3
      anchor = Anchor.NorthWest
      insets = new Insets(5, 0, 0, 0)
    }
  }


  /**
   * Initialization of the components.
   */
  def init(e: E, s: Site): Unit = {
    editor = Some(e)
    val instrument = e.getDataObject

    // Turn off the parallactic angle changing event handling as it triggers an AGS lookup.
    ui.parallacticAngleControlsOpt.foreach(p => {
      deafTo(p)
      p.init(e, s, numberFormatter)
    })

    // Reset the combo box so that all of the options are enabled by default.
    // TODO: When background AGS is implemented, we can remove the deafTo + listenTo lines, as well as the
    // disabling of the positionAngleTextField.
    deafTo(ui.positionAngleConstraintComboBox.selection)

    // TODO: Currently the UNBOUNDED PosAngleConstraint is disabled, so we remove it from any list of PACs.
    val availablePACsNoUnbounded = instrument.getSupportedPosAngleConstraints.asScalaList.diff(List(PosAngleConstraint.UNBOUNDED))
    ui.positionAngleConstraintComboBox.setItemsAndResetSelectedItem(availablePACsNoUnbounded)

    ui.positionAngleConstraintComboBox.resetEnabledItems()
    ui.positionAngleConstraintComboBox.selection.item = instrument.getPosAngleConstraint

    // Ignore changes to the position angle text field if it is being edited, i.e. has the focus.
    // This is to avoid BAGS changing the contents to +180 while editing is occurring.
    if (!ui.positionAngleTextField.hasFocus) {
      copyPosAngleToTextField()
    }
    ui.positionAngleTextField.enabled = instrument.getPosAngleConstraint != PosAngleConstraint.UNBOUNDED
    ui.controlsPanel.updatePanel()
    listenTo(ui.positionAngleConstraintComboBox.selection)

    // Turn on the parallactic angle changing event handling.
    ui.parallacticAngleControlsOpt.foreach(p => listenTo(p))

    // Determine if the parallactic angle and unbounded angle features should be enabled to begin.
    updateParallacticControls()
    updateUnboundedControls()
  }

  /** Sets the enabled state of contained widgets to match the provided value. */
  def updateEnabledState(enabled: Boolean): Unit = {
    ui.positionAngleConstraintComboBox.enabled = enabled
    ui.positionAngleTextField.enabled = enabled
    ui.parallacticAngleControlsOpt.foreach { p =>
      p.enabled = enabled
    }
  }

  /**
    * The actual copying of a given pos angle to the data object.
    * This is done explicitly to avoid overwriting based on minor differences in double precision and to avoid
    * adding 180 to the position angle for parallactic angle mode, which overrides BAGS flips.
    **/
  private def setInstPosAngle(newAngleDegrees: Double): Unit =
    editor.foreach(_.getDataObject.setPosAngleDegrees(newAngleDegrees))

  /**
   * Copies, if possible, the position angle text field contents to the data object.
   */
  private def copyPosAngleToInstrument(): Unit =
    ui.positionAngleTextField.angle.foreach(setInstPosAngle)

  /**
    * Copies the position angle in the data object to the position angle text field.
    */
  private def copyPosAngleToTextField(): Unit =
    editor.foreach { e =>
      val newAngleStr = numberFormatter.format(e.getDataObject.getPosAngleDegrees)
      val oldAngleStr = ui.positionAngleTextField.text
      if (!newAngleStr.equals(oldAngleStr)) {
        ui.positionAngleTextField.text = newAngleStr
      }
    }

  /**
   * Called whenever a selection is made in the position angle constraint combo box.
   * Sets the position angle constraint on the instrument, and sets up the lower controls to account for the new
   * selection.
   */
  private def positionAngleConstraintSelected(): Unit = {
    for {
      e <- editor
      p <- ui.parallacticAngleControlsOpt
    } {
      // Set the position angle constraint on the instrument.
      val posAngleConstraint = ui.positionAngleConstraintComboBox.selection.item
      e.getDataObject.setPosAngleConstraint(posAngleConstraint)

      // Set up the UI.
      ui.positionAngleTextField.enabled = posAngleConstraint != PosAngleConstraint.UNBOUNDED
      ui.controlsPanel.updatePanel()
      ui.positionAngleConstraintComboBox.selection.item match {
        case PosAngleConstraint.PARALLACTIC_ANGLE => p.resetComponents()
        case _                                    =>
      }
    }
  }


  /**
   * A listener method that is called whenever the parallactic angle changes.
   * We set the position angle to the parallactic angle.
   */
  private def parallacticAngleChanged(angleOpt: Option[Angle]): Unit =
    angleOpt.foreach(angle => {
      val degrees = angle.toDegrees
      ui.positionAngleTextField.text = numberFormatter.format(degrees)
      setInstPosAngle(degrees)
    })


  /**
   * Called to update the status of the parallactic angle controls. This allows / disallows the parallactic
   * angle to be selected in the position angle constraint combo box, and if it is selected but not allowed,
   * resets to FIXED.
   */
  def updateParallacticControls(): Unit =
    for {
      p <- ui.parallacticAngleControlsOpt
      i <- editor.map(_.getDataObject)
      o <- editor.map(_.getContextObservation)
    } {
      // Determine if the parallactic angle option can be selected.
      val isParInstAndOk = i.isInstanceOf[ParallacticAngleSupport] &&
                           i.asInstanceOf[ParallacticAngleSupport].isCompatibleWithMeanParallacticAngleMode
      val canUseAvgPar   = isParInstAndOk &&
                           !ObsClassService.lookupObsClass(o).equals(ObsClass.DAY_CAL)
      setOptionEnabled(PosAngleConstraint.PARALLACTIC_ANGLE, canUseAvgPar)

      // Now the parallactic angle is in use if it can be used and is selected.
      if (canUseAvgPar && i.getPosAngleConstraint.equals(PosAngleConstraint.PARALLACTIC_ANGLE))
        p.ui.relativeTimeMenu.rebuild()
    }


  def updateUnboundedControls(): Unit =
    editor.foreach(e => setOptionEnabled(PosAngleConstraint.UNBOUNDED, e.getDataObject.allowUnboundedPositionAngle()))


  private def setOptionEnabled(option: PosAngleConstraint, enabled: Boolean): Unit = {
    if (enabled)
      ui.positionAngleConstraintComboBox.enableItem(option)
    else {
      ui.positionAngleConstraintComboBox.disableItem(option)
    }
  }
}


object PositionAnglePanel {
  def apply[I <: SPInstObsComp with PosAngleConstraintAware with ParallacticAngleSupport,
            E <: OtItemEditor[ISPObsComponent, I]](instType: SPComponentType): PositionAnglePanel[I,E] =
    new PositionAnglePanel[I,E](instType)
}
