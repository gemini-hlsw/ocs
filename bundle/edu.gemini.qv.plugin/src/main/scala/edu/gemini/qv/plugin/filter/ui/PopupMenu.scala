package edu.gemini.qv.plugin.filter.ui

import scala.swing.{Point, MenuItem, Component}
import javax.swing.JPopupMenu

class PopupMenu extends Component {
  override lazy val peer = new JPopupMenu {
      override def setVisible(visible: Boolean) = {
        super.setVisible(visible)
        if (visible) lastLocation = locationOnScreen
      }
  }
  private var lastLocation = new Point(0, 0)

  def add(item: MenuItem) = peer.add(item.peer)

  def lastLocationOnScreen = lastLocation
}

