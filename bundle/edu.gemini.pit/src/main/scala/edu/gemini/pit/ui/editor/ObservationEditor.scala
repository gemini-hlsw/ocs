package edu.gemini.pit.ui.editor


import edu.gemini.model.p1.immutable._

import edu.gemini.pit.ui.util.SharedIcons._
import edu.gemini.pit.ui.util._

import java.awt.Color
import javax.swing.Icon

import scala.swing._
import event.ValueChanged
import scala.swing.Swing._

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
    addRow(new Label("Conditions"), new OptionLabel(obs.condition, ICON_CONDS))
    addRow(new Label("Resources"), new OptionLabel(obs.blueprint, ICON_DEVICE))
    addRow(new Label("Target"), new OptionLabel(obs.target, obs.target match {
      case Some(_:NonSiderealTarget) => ICON_NONSIDEREAL
      case _                         => ICON_SIDEREAL
    }))
    addSpacer()
    addRow(new Label("Time"), new BorderPanel {
      border = null
      add(Time, BorderPanel.Position.Center)
      add(Units, BorderPanel.Position.East)
    })
    preferredSize = (400, preferredSize.height) // force width
  }

  // Editable
  Contents.Footer.OkButton.enabled = canEdit

  // Validation
  override def editorValid = Time.valid
  Time.reactions += {
    case ValueChanged(_) => validateEditor()
  }

  type Named = {
    def name:String
  }

  class OptionLabel(a:Option[Named], icon0:Icon) extends Label() {
    horizontalAlignment = Alignment.Left
    icon = icon0
    text = a.map(_.name).getOrElse("none")
    if (a.isEmpty)
      foreground = Color.GRAY
  }

  object Time extends NumberField(obs.time.map(_.value).orElse(Some(1.0))) {
    enabled = canEdit
    override def valid(d:Double) = d > 0
  }

  object Units extends ComboBox(TimeUnit.values.toList) with ValueRenderer[TimeUnit] {
    selection.item = obs.time.getOrElse(TimeAmount.empty).units
  }

  // Construct our editor
  def editor = Editor

  // Construct a new value
  def value = obs.copy(
    time = Some(TimeAmount(Time.text.toDouble, Units.selection.item)),
    meta = None)

}

