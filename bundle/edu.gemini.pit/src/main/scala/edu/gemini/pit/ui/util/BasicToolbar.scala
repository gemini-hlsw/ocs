package edu.gemini.pit.ui.util
import scala.swing.Panel
import javax.swing.JPanel
import java.awt.FlowLayout
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.Icon
import scala.swing.Button
import java.awt.Insets
import scala.swing.Label
import scala.swing.Component

object BasicToolbar {

  class IconButton(enabledIcon: Icon, disabledIcon: Icon) extends Button {
    override lazy val peer = new JButton(enabledIcon) {
      setDisabledIcon(disabledIcon);
      setBorder(null);
      setOpaque(false);
      setFocusable(false);
      setMargin(new Insets(0, 0, 0, 0));
    }
  }

  class StaticText(text: String) extends Label(text) {
    foreground = Color.LIGHT_GRAY
  }
  
}

class BasicToolbar extends Panel {

  override lazy val peer = new JPanel(new FlowLayout(FlowLayout.LEFT)) {
    setBackground(new Color(255, 255, 224))
    setOpaque(true)
    setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY))
  }
  
  def add(c:Component) = peer.add(c.peer)

}