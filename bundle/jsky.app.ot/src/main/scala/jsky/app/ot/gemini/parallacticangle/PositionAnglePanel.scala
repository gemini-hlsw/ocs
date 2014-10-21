package jsky.app.ot.gemini.parallacticangle

import java.awt.CardLayout
import java.text.NumberFormat
import java.util.Locale

import edu.gemini.pot.sp.{SPComponentType, ISPObsComponent}
import edu.gemini.shared.gui.EnableDisableComboBox
import edu.gemini.skycalc.Angle
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.inst.{ParallacticAngleSupport, PositionAngleMode}
import edu.gemini.spModel.obs.ObsClassService
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.telescope.{PosAngleConstraintRegistrar, PosAngleConstraint, PosAngleConstraintAware}
import jsky.app.ot.editor.OtItemEditor
import jsky.app.ot.tpe.AgsClient
import jsky.app.ot.util.OtColor

import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing._
import scala.swing.event.{SelectionChanged, ValueChanged}
import scala.util.Try


class PositionAnglePanel[I <: SPInstObsComp with PosAngleConstraintAware,
                         E <: OtItemEditor[ISPObsComponent, I]](instType: SPComponentType) extends GridBagPanel with Reactor {
  private var editor: Option[E] = None

  private val numberFormatter = NumberFormat.getInstance(Locale.US)
  numberFormatter.setMaximumFractionDigits(2)
  numberFormatter.setMaximumIntegerDigits(3)

  // In order to avoid a lot of ugly hacks, we need the list of PosAngleConstraint options for the instrument at the
  // time of creation to avoid ugly UI hacks. Since Java doesn't allow overriding static methods, we have to work
  // around this.
  private val options = PosAngleConstraintRegistrar.validPosAngleConstraintsForInstrument(instType)


  private object ui {
    val positionAngleConstraintComboBox = new EnableDisableComboBox[PosAngleConstraint](options)
    // TODO: Remove this once the logic is fully implemented.
    if (options.contains(PosAngleConstraint.UNBOUNDED))
      positionAngleConstraintComboBox.disable(PosAngleConstraint.UNBOUNDED)
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

      def validate(): Unit =
        background = angle.fold(badBackground)(x => defaultBackground)
    }

    layout(positionAngleTextField) = new Constraints() {
      gridx  = 1
      anchor = Anchor.NorthWest
      insets = new Insets(0, 0, 0, 5)
    }

    // We want the parallactic angle controls to be notified every time the position angle changes.
    // A warning icon will be displayed if the two values are not the same according to the chosen formatter.
    listenTo(positionAngleTextField)
    reactions += {
      case ValueChanged(`positionAngleTextField`) =>
        ui.parallacticAngleControlsOpt.foreach(_.positionAngleChanged(positionAngleTextField.text))
        ui.positionAngleTextField.validate()
        copyPosAngleToInstrument()
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


    // Parallactic angle controls, if needed.
    val parallacticAngleControlsOpt = {
      if (options.contains(PosAngleConstraint.PARALLACTIC_ANGLE)) {
        val parallacticAngleControls = new ParallacticAngleControls

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
    }

    //controlsPanel.layout(positionAngleFeedback) = BorderPanel.Position.Center
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
      p.init(e.asInstanceOf[ParallacticAngleControls.Editor], s, numberFormatter)
    })

    // Reset the combo box so that all of the options are enabled by default.
    ui.positionAngleConstraintComboBox.reset()
    ui.positionAngleConstraintComboBox.selection.item = instrument.getPosAngleConstraint
    ui.positionAngleTextField.text                    = numberFormatter.format(instrument.getPosAngle)

    // Turn on the parallactic angle changing event handling.
    ui.parallacticAngleControlsOpt.foreach(p => listenTo(p))

    // Determine if the parallactic angle feature should be enabled to begin.
    updateParallacticControls()
  }


  /**
   * Copies, if possible, the position angle text field contents to the data object.
   */
  private def copyPosAngleToInstrument(): Unit =
    for {
      e <- editor
      a <- ui.positionAngleTextField.angle
    } yield {
      e.getDataObject.setPosAngle(a)
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
    } yield {
      val posAngleConstraint = ui.positionAngleConstraintComboBox.selection.item

      // Set the position angle constraint on the instrument.
      e.getDataObject.setPosAngleConstraint(posAngleConstraint)

      // Set up the UI.
      ui.positionAngleConstraintComboBox.selection.item match {
        case PosAngleConstraint.PARALLACTIC_ANGLE =>
          //ui.controlsPanel.layout.remove(ui.positionAngleFeedback)
          //ui.controlsPanel.layout(p) = BorderPanel.Position.Center
          ui.controlsPanel.showParallacticAngleControls()
          p.resetComponents()
        case _ =>
          //ui.controlsPanel.layout.remove(p)
          //ui.controlsPanel.layout(ui.positionAngleFeedback) = BorderPanel.Position.Center
          ui.controlsPanel.showPositionAngleFeedback()
      }
    }
  }


  /**
   * A listener method that is called whenever the parallactic angle changes.
   * We set the position angle to the parallactic angle.
   */
  private def parallacticAngleChanged(angleOpt: Option[Angle]): Unit = {
    for {
      angle <- angleOpt
      e     <- editor
      pa    <- ui.parallacticAngleControlsOpt
    } yield {
      val angleAsDouble = angle.toDegrees.toPositive.getMagnitude
      ui.positionAngleTextField.text = numberFormatter.format(angleAsDouble)
      pa.setPositionAngleMode(PositionAngleMode.MEAN_PARALLACTIC_ANGLE)
      e.getDataObject.setPosAngle(angleAsDouble)

      // This requires an AGS lookup.
      launchAGS()
    }
  }


  /**
   * Called to update the status of the parallactic angle controls. This allows / disallows the parallactic
   * angle to be selected in the position angle constraint combo box, and if it is selected but not allowed,
   * resets to FIXED.
   */
  def updateParallacticControls(): Unit = {
    for {
      p <- ui.parallacticAngleControlsOpt
      e <- editor
    } yield {
      val instrument = e.getDataObject

      // Determine if the parallactic angle option can be selected.
      val isParInstAndOk = instrument.isInstanceOf[ParallacticAngleSupport] &&
                           instrument.asInstanceOf[ParallacticAngleSupport].isCompatibleWithMeanParallacticAngleMode
      val canUseAvgPar   = isParInstAndOk &&
                           !ObsClassService.lookupObsClass(e.getContextObservation).equals(ObsClass.DAY_CAL)

      // Configure the combo box to allow / disallow it.
      // If parallactic angle is selected in the combox box and it is no longer admissible, then deselect it
      // and instead go to FIXED.
      if (canUseAvgPar)
        ui.positionAngleConstraintComboBox.enable(PosAngleConstraint.PARALLACTIC_ANGLE)
      else {
        ui.positionAngleConstraintComboBox.disable(PosAngleConstraint.PARALLACTIC_ANGLE)
        if (ui.positionAngleConstraintComboBox.selection.item.equals(PosAngleConstraint.PARALLACTIC_ANGLE))
          ui.positionAngleConstraintComboBox.selection.item = PosAngleConstraint.FIXED
      }

      // Now the parallactic angle is in use if it can be used and is selected.
      if (canUseAvgPar && instrument.getPosAngleConstraint.equals(PosAngleConstraint.PARALLACTIC_ANGLE))
        ui.parallacticAngleControlsOpt.foreach(_.ui.relativeTimeMenu.rebuild())
    }
  }


  /**
   * Launch the AGS lookup.
   */
  private def launchAGS(): Unit =
    editor.foreach(e => AgsClient.launch(e.getNode, e.getWindow))
}


object PositionAnglePanel {
  def apply[I <: SPInstObsComp with PosAngleConstraintAware with ParallacticAngleSupport,
            E <: OtItemEditor[ISPObsComponent, I]](instType: SPComponentType): PositionAnglePanel[I,E] =
    new PositionAnglePanel[I,E](instType)
}