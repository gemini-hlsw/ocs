package edu.gemini.pit.ui.action

import java.net.URI
import javax.swing.Action.ACCELERATOR_KEY
import javax.swing.{AbstractAction, KeyStroke}
import java.awt.event.ActionEvent

import edu.gemini.shared.gui.Browser

class BrowseAction(uri: URI, text: String, vkey:Int = 0, modifiers: Int = 0) extends AbstractAction(text) {
  if (vkey != 0) putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(vkey, modifiers))

  def this(uri: String, text: String) = this(new URI(uri), text)

  def actionPerformed(e: ActionEvent) {
    Browser.open(uri)
  }
}
