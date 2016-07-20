package edu.gemini.shared.gui

import javax.swing.{JEditorPane, JLabel, JOptionPane}
import javax.swing.event.{HyperlinkEvent, HyperlinkListener}

object ErrorBoxWithHyperlink {

  def showErrorBoxWithLink(msg: String): Unit = {
    // for copying style
    val label = new JLabel()
    val font = label.getFont

    // create some css from the label's font
    val style = s"font-family:${font.getFamily};font-weight:${if (font.isBold) "bold" else "normal"};font-size:${font.getSize}pt;"

    // JEditor pane to display text with a hyperlink
    val ep = new JEditorPane("text/html", s"""<html><body style="$style">$msg</body></html>""")
    // Open de hiperlink when clicked
    ep.addHyperlinkListener(new HyperlinkListener() {
      override def hyperlinkUpdate(e: HyperlinkEvent): Unit = {
        if (e.getEventType == HyperlinkEvent.EventType.ACTIVATED) {
          Browser.open(e.getURL.toURI)
        }
      }
    })
    // No editing...
    ep.setEditable(false)
    ep.setBackground(label.getBackground)
    // Display
    JOptionPane.showMessageDialog(null, ep, "Error", JOptionPane.ERROR_MESSAGE)
  }
}
