package edu.gemini.qv.plugin.chart.ui

import scala.swing._
import java.awt.Color
import scala.swing.Swing._
import scala.swing.GridBagPanel.Fill.{Horizontal, Both}
import scala.swing.Insets
import scala.swing.GridBagPanel.Anchor._
import scala.swing.event.ValueChanged
import edu.gemini.qv.plugin.ui.QvGui


/**
 * Base class for user element editors for editing axes, charts and tables.
 * It provides functionality to save, delete and replace elements, checks for already existing (taken) and
 * immutable (default) elements and contains some rules for enabling/disabling its save and delete buttons.
 * Axes editors are more complex than charts and tables editors and pass down an additional editor panel which
 * is added to the dialog in order to allow users to build custom axes.
 */
abstract class ElementEditor(label: String, initial: String, defaults: Set[String], taken: Set[String], editPanel: Option[Component] = None) extends Dialog {

  var isCancelled = false

  val description = new Label("Edit and store user defined " + label.toLowerCase + ".") {
    horizontalAlignment = Alignment.Left
  }
  val textField = new TextField(initial) {
  }
  val status = new Label() {
    foreground = Color.darkGray
    background = new Color(255, 255, 204)
    opaque = true
    horizontalAlignment = Alignment.Left
  }
  val deleteButton = Button("Delete") { delete(); close() }
  val saveButton = Button("Save") { save(); close() }
  val cancelButton = Button("Cancel") { isCancelled = true; close() }
  val buttons = new GridBagPanel {
    border = EmptyBorder(2, 2, 2, 2)
    layout(Swing.HGlue) = new Constraints() {
      gridx = 0; weightx = 1.0; fill = Horizontal
    }
    layout(cancelButton) = new Constraints() {
      gridx = 1; insets = new Insets(0,0, 0, 5)
    }
    layout(deleteButton) = new Constraints() {
      gridx  = 2; insets = new Insets(0,0, 0, 5)
    }
    layout(saveButton) = new Constraints() {
      gridx  = 3
    }
  }

  title = s"Edit ${label}"
  modal = true
  contents = new GridBagPanel {
    border = EmptyBorder(10, 10, 10, 10)
    var yPos = 0
    layout(description) = new Constraints() {
      gridx=0; gridy=yPos; weightx = 1.0; anchor = West; fill = Horizontal; insets = new Insets(0, 0, 5, 0)
    }
    yPos += 1
    layout(textField) = new Constraints() {
      gridx=0; gridy=yPos; weightx = 1.0; anchor = West; fill = Horizontal
    }
    yPos += 1
    layout(status) = new Constraints() {
      gridx=0; gridy=yPos; weightx = 1.0; anchor = West; fill = Horizontal
    }
    yPos += 1
    if (editPanel.isDefined) {
      layout(editPanel.get) = new Constraints() {
        gridx=0; gridy=yPos; weightx = 1.0; weighty = 1.0; anchor = West; fill = Both; insets = new Insets(5, 0, 0, 0)
      }
      yPos += 1
    }
    layout(buttons) = new Constraints() {
      gridx=0; gridy=yPos; weightx = 1.0; anchor = East; fill = Horizontal; insets = new Insets(5, 0, 0, 0)
    }
  }

  listenTo(textField)
  reactions += {
    case _: ValueChanged => updateUI()
  }
  updateUI()
  pack()

  def elementName = textField.text

  def delete()
  def save()

  def updateUI() {
    val isInvalid = textField.text.isEmpty             // true if name is invalid
    val isDefault = defaults.contains(textField.text)  // true if this is a name used by a default (immutable) element
    val isTaken = taken.contains(textField.text)       // true if this name is already taken by another user element

    deleteButton.enabled = !isDefault && isTaken
    saveButton.enabled = !isDefault && !isInvalid
    saveButton.text = if (isTaken) "Save" else "Save As"
    status.visible = true
    if (isInvalid) {
      status.text = "This is not a valid name."
      status.icon = QvGui.ErrorIcon
    }
    else if (isDefault) {
      status.text = "This name is used by the default elements and is forbidden for user elements."
      status.icon = QvGui.ErrorIcon
    }
    else if (isTaken) {
      status.text = "This name is already in use, saving this element will replace the existing one."
      status.icon = QvGui.InfoIcon
    }
    else {
      status.visible = false
    }
  }

}
