package jsky.app.ot.gemini.editor.targetComponent

import javax.swing.BorderFactory._
import javax.swing.border.Border

import jsky.util.gui.{TextBoxWidget, TextBoxWidgetWatcher}

package object details {

  /** Create a titled border with inner and outer padding. */
  def titleBorder(title: String): Border =
    createCompoundBorder(
      createEmptyBorder(2,2,2,2),
      createCompoundBorder(
        createTitledBorder(title),
        createEmptyBorder(2,2,2,2)))

  def watcher(f: String => Unit) = new TextBoxWidgetWatcher {
    override def textBoxKeyPress(tbwe: TextBoxWidget): Unit = textBoxAction(tbwe)
    override def textBoxAction(tbwe: TextBoxWidget): Unit = f(tbwe.getValue)
  }

}
