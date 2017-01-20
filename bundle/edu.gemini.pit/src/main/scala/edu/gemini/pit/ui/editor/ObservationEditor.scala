package edu.gemini.pit.ui.editor


import edu.gemini.model.p1.immutable._
import edu.gemini.pit.ui.util.SharedIcons._
import edu.gemini.pit.ui.util._
import java.awt.Color
import javax.swing.Icon

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


  class RightLabel(t: String) extends Label(t) {
    horizontalAlignment = Alignment.Right
  }

  // Editable
  Contents.Footer.OkButton.enabled = canEdit

  // Validation
  override def editorValid = IntegrationTime.valid
  IntegrationTime.reactions += {
    case ValueChanged(_) => validateEditor()
  }

  type Named = {
    def name: String
  }

  class OptionLabel(a:Option[Named], icon0:Icon) extends Label() {
    horizontalAlignment = Alignment.Left
    icon = icon0
    text = a.map(_.name).getOrElse("none")
    if (a.isEmpty)
      foreground = Color.GRAY
  }

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

  object ProgramTime extends NumberField(None, allowEmpty = false) {
    enabled = false
    def update(t: Double): Unit =
      text = t.toString
  }
  object ProgramTimeUnits extends UnitsLabel

  object PartTime extends NumberField(None, allowEmpty = false) {
    enabled = false
    def update(t: Double): Unit =
      text = t.toString
  }
  object PartTimeUnits extends UnitsLabel

  object TotalTime extends NumberField(None, allowEmpty = false) {
    enabled = false
    def update(t: Double): Unit =
      text = t.toString
  }
  object TotalTimeUnits extends UnitsLabel

  def updateTimeLabels(): Unit = {
    val t = \/.fromTryCatchNonFatal(IntegrationTime.text.toDouble).getOrElse(0.0)
    ProgramTime.update(t)
    PartTime.update(t)
    TotalTime.update(t)
  }
  IntegrationTime.reactions += {
    case ValueChanged(_) => updateTimeLabels()
  }
  updateTimeLabels()

  def updateUnitsLabels(): Unit = {
    val t = Units.selection.item.value()
    ProgramTimeUnits.update(t)
    PartTimeUnits.update(t)
    TotalTimeUnits.update(t)
  }
  Units.selection.reactions += {
    case SelectionChanged(_) => updateUnitsLabels()
  }
  updateUnitsLabels()

  // Construct our editor
  def editor = Editor

  // Construct a new value
  def value = obs.copy(
    time = Some(TimeAmount(IntegrationTime.text.toDouble, Units.selection.item)),
    meta = None)
}