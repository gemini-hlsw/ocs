package edu.gemini.util.security.ext.auth.ui

import swing.{Separator, GridBagPanel, Component, Label}

trait Rows { this: GridBagPanel =>

//  border = DLU4_BORDER
  private var row = 0
  def addRow(a: Component, b: Component, f:GridBagPanel.Fill.Value = GridBagPanel.Fill.Horizontal, wy:Int = 0 ) {
    add(a, new Constraints { gridx = 0; gridy = row; ipadx = 10; ipady = 4; anchor = GridBagPanel.Anchor.NorthEast })
    add(b, new Constraints { gridx = 1; gridy = row; fill = f; weightx = 1; weighty = wy })
    row = row + 1
  }
  def addRow(a: Component, b: Component, c: Component) {
    add(a, new Constraints { gridx = 0; gridy = row; ipadx = 10; ipady = 4; anchor = GridBagPanel.Anchor.West })
    add(b, new Constraints { gridx = 1; gridy = row; fill = GridBagPanel.Fill.Horizontal; weightx = 2 })
    add(c, new Constraints { gridx = 2; gridy = row; weightx = 1 })
    row = row + 1
  }
  def addRow(a: Component) {
    add(a, new Constraints { gridx = 0; gridwidth = 2; gridy = row; ipadx = 10; ipady = 4; anchor = GridBagPanel.Anchor.West })
    row = row + 1
  }
  def addRow(a: Component, cols:Int) {
    add(a, new Constraints { gridx = 0; gridwidth = cols; gridy = row; ipadx = 10; ipady = 4; anchor = GridBagPanel.Anchor.West })
    row = row + 1
  }
  def addCentered(a: Component) {
    add(a, new Constraints { gridx = 0; gridwidth = 3; gridy = row; ipadx = 10; ipady = 4 })
    row = row + 1
  }
  def addSpacer() {
    addRow(new Label(""), new Label("")) // :-/
  }
  def addSeparator() {
    add(new Separator(), new Constraints { gridx = 0; gridwidth = 3; gridy = row; ipadx = 10; ipady = 4; fill = GridBagPanel.Fill.Horizontal ; weightx = 1.0})
    row = row + 1
  }
}