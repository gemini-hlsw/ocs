package edu.gemini.pit.ui.editor


import edu.gemini.model.p1.immutable._
import edu.gemini.pit.ui.util.SharedIcons._
import edu.gemini.pit.ui.util._
import java.awt.Color
import javax.swing.Icon

import edu.gemini.pit.overheads.Overheads
import edu.gemini.shared.gui.textComponent.NumberField

import scala.swing._
import event.{SelectionChanged, ValueChanged}
import scala.swing.Swing._
import scalaz._
import Scalaz._


object ObservationEditor {

  def open(c:Option[Observation], editable:Boolean, parent:UIElement) =
    new ObservationEditor(c.getOrElse(Observation.empty.copy(time = Some(TimeAmount.empty))), editable).open(parent)

}

/**
 * Modal editor for an Observation.
 */
class ObservationEditor private (obs:Observation, canEdit:Boolean) extends StdModalEditor[Observation]("Edit Observation Time") {

  // Editor component
  object Editor extends GridBagPanel with Rows {
    private class RightLabel(t: String) extends Label(t) {
      horizontalAlignment = Alignment.Right
    }

    private type Named = {
      def name: String
    }

    private class OptionLabel(a:Option[Named], icon0:Icon) extends Label() {
      horizontalAlignment = Alignment.Left
      icon = icon0
      text = a.map(_.name).getOrElse("none")
      if (a.isEmpty)
        foreground = Color.GRAY
    }

    addRow(new Label("Conditions"), new OptionLabel(obs.condition, ICON_CONDS), gw=2)
    addRow(new Label("Resources"), new OptionLabel(obs.blueprint, ICON_DEVICE), gw=2)
    addRow(new Label("Target"), new OptionLabel(obs.target, obs.target match {
      case Some(_:NonSiderealTarget) => ICON_NONSIDEREAL
      case _                         => ICON_SIDEREAL
    }), gw=2)
    addSpacer()
    addLabeledRow(new RightLabel("Integration Time"), IntegrationTime, Units)
    addLabeledRow(new RightLabel("Program Time"), ProgramTime, ProgramTimeUnits)
    addLabeledRow(new RightLabel("Night Basecal Time"), PartTime, PartTimeUnits)
    addLabeledRow(new RightLabel("Total Time"), TotalTime, TotalTimeUnits)
    preferredSize = (500, preferredSize.height) // force width
  }

  // Editable
  Contents.Footer.OkButton.enabled = canEdit

  // Validation
  override def editorValid = IntegrationTime.valid
  IntegrationTime.reactions += {
    case ValueChanged(_) => validateEditor()
  }

  // Time calculator
  val calculator = obs.blueprint.flatMap(Overheads)

  object IntegrationTime extends NumberField(obs.time.map(_.value).orElse(Some(1.0)), allowEmpty = false) {
    enabled = canEdit
    override def valid(d:Double) = d > 0
  }

  object Units extends ComboBox(TimeUnit.values.toList) with ValueRenderer[TimeUnit] {
    enabled = canEdit
    selection.item = obs.time.getOrElse(TimeAmount.empty).units
  }

  class UnitsLabel extends Label {
    def update(t: String): Unit =
      text = t
  }

  class CalculatedTimeAmountField extends NumberField(None, allowEmpty = false) {
    enabled = false

    def update(t: TimeAmount): Unit =
      value = (Units.selection.item match {
        case TimeUnit.HR    => t.toHours
        case TimeUnit.NIGHT => t.toNights
      }).value
  }

  object ProgramTime extends CalculatedTimeAmountField
  object ProgramTimeUnits extends UnitsLabel

  object PartTime extends CalculatedTimeAmountField
  object PartTimeUnits extends UnitsLabel

  object TotalTime extends CalculatedTimeAmountField
  object TotalTimeUnits extends UnitsLabel

  def updateTimeLabels(): Unit = {
    val intTime = TimeAmount(\/.fromTryCatchNonFatal(IntegrationTime.text.toDouble).getOrElse(0.0), Units.selection.item)
    val obsTimes = calculator.map(_.calculate(intTime))
    ProgramTime.update(obsTimes.map(_.progTime).getOrElse(TimeAmount.empty))
    PartTime.update(obsTimes.map(_.partTime).getOrElse(TimeAmount.empty))
    TotalTime.update(obsTimes.map(_.totalTime).getOrElse(TimeAmount.empty))
  }
  IntegrationTime.reactions += {
    case ValueChanged(_) => updateTimeLabels()
  }

  def updateUnitsLabels(): Unit = {
    val t = Units.selection.item.value()
    ProgramTimeUnits.update(t)
    PartTimeUnits.update(t)
    TotalTimeUnits.update(t)
  }
  Units.selection.reactions += {
    case SelectionChanged(_) =>
      // A units change requires the labels to change and will change the calculated time amounts, so must update both.
      updateUnitsLabels()
      updateTimeLabels()
  }

  // Initialize the dependent UI components.
  updateTimeLabels()
  updateUnitsLabels()

  // Construct our editor
  def editor = Editor

  // Construct a new value
  def value = obs.copy(
    time = Some(TimeAmount(IntegrationTime.text.toDouble, Units.selection.item)),
    meta = None)
}