package edu.gemini.pit.ui.util

import javax.swing.BorderFactory
import java.awt.{Insets, Color}
import swing._

class StdToolbar extends GridBagPanel {
  background = new Color(255, 255, 224)
  opaque     = true
  border     = BorderFactory.createCompoundBorder(
    BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
    BorderFactory.createEmptyBorder(5, 5, 5, 5)
  )

  private def separator = new Label("|") {
    foreground = Color.LIGHT_GRAY
  }

  private def flexibleSpace = new FlowPanel() {
    opaque = false
  }

  private var compgap = 5

  def gap: Int = compgap
  def gap_=(g: Int) {
    compgap = g
  }

  private def compinsets = new Insets(0, compgap, 0, 0)

  private var col = 0

  private def addComp(c: Component, s: Constraints) {
    add(c, new Constraints(s.peer) {
      gridx = col
    })
    col = col + 1
  }

  protected def add(c: Component) {
    addComp(c, new Constraints { if (col > 0) insets = compinsets })
  }

  protected def addSeparator() {
    add(separator)
  }

  protected def addFlexibleSpace() {
    addComp(flexibleSpace, new Constraints {
      weightx = 1
      fill    = GridBagPanel.Fill.Horizontal
    })
  }

  protected def addLabel(s: String) {
    add(new Label(s) { foreground = Color.DARK_GRAY })
  }
}