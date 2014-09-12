package edu.gemini.qv.plugin.ui

import edu.gemini.qv.plugin.ui.QvGui._
import edu.gemini.shared.gui.RotatedButtonUI
import javax.swing.BorderFactory
import scala.swing.GridBagPanel.Fill._
import scala.swing.ScrollPane.BarPolicy
import scala.swing._
import scala.swing.event.ButtonClicked

/**
 */
class SideBarPanel(label: String, openedAtStart: Boolean = false) extends GridBagPanel {

  border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  object barButton extends ToggleButton(label) {
    selected = openedAtStart
    peer.setUI(new RotatedButtonUI(RotatedButtonUI.Orientation.topToBottom))

    reactions += {
      case ButtonClicked(_) => update()
    }

    update()

    private def update() {
      foreground = DarkGreen
      background = if (selected) LightOrange else VeryLightGray
      SideBarPanel.this.visible = selected
    }
  }

}

object SideBarPanel {
  def apply(label: String, buttons: Button*) = {
    val panel = new SideBarPanel(label) {
      buttons.zipWithIndex.foreach({case (b, y) => {
        layout(b) = new Constraints() {
          gridx = 0
          gridy = y
          weightx = 1
          fill = Horizontal
        }
      }})
      layout(Swing.VGlue) = new Constraints() {
        gridx = 0
        gridy = buttons.size
        weighty = 1
        fill = Vertical
      }
    }
    panel
  }
}

class SideBar(panels: SideBarPanel*) extends GridBagPanel {

  private object sideBarButtons extends BoxPanel(Orientation.Vertical) {
    panels.foreach { p => contents += p.barButton }
  }

  private object sideBarPanels extends GridBagPanel {
    panels.zipWithIndex.foreach { case (p, x) =>
      layout(p) = new Constraints {
        gridx = x
        gridy = 0
        weightx = 1
        weighty = 1
        fill = GridBagPanel.Fill.Both
      }
    }
  }

  private object panelsScrollPane extends ScrollPane {
    visible = false
    minimumSize = new Dimension(200, 100)
    verticalScrollBarPolicy = BarPolicy.AsNeeded
    horizontalScrollBarPolicy = BarPolicy.AsNeeded
    contents = sideBarPanels
  }

  layout(panelsScrollPane) = new Constraints() {
    gridx = 0
    gridy = 0
    weightx = 1
    weighty = 1
    fill = GridBagPanel.Fill.Both
  }
  layout(sideBarButtons) = new Constraints() {
    gridx = 1
    gridy = 0
    weighty = 1
    fill = GridBagPanel.Fill.Vertical
  }

  buttons.foreach(listenTo(_))
  reactions += {
    case ButtonClicked(b) =>
      buttons.foreach(deafTo(_))
      buttons.foreach { pb =>
        if (b != pb && pb.selected) pb.doClick()
      }
      panelsScrollPane.visible = buttons.exists(_.selected)
      buttons.foreach(listenTo(_))
      revalidate()
  }

  private lazy val buttons = panels.map(_.barButton)

}
