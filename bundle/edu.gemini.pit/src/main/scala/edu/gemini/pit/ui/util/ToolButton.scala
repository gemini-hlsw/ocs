package edu.gemini.pit.ui.util

import edu.gemini.pit.ui.util.SimpleToolbar.IconButton
import javax.swing.Icon
import scala.swing.Button
import scala.swing.event.ButtonClicked

object ToolButton {
  def apply(icon: Icon, disabledIcon: Icon, tooltipText: String)(f: => Unit) =
    new ToolButton(icon, disabledIcon, tooltipText) {
      def apply = f
    }
}

// A generic tool button
abstract class ToolButton(icon: Icon, disabledIcon: Icon, tooltipText: String) extends Button {
  override lazy val peer = new IconButton(icon, disabledIcon)
  tooltip = tooltipText
  reactions += {
    case ButtonClicked(_) => apply()
  }
  def apply()
}

