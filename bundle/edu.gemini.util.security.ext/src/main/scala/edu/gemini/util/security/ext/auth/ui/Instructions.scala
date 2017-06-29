package edu.gemini.util.security.ext.auth.ui

import swing.{TextArea, Label, BorderPanel}
import javax.swing.{Icon, BorderFactory, ImageIcon}
import java.awt.Color

abstract class Instructions extends BorderPanel { outer =>

  def instructions:String
  def icon:Icon

  add(new Label() {
    icon = outer.icon
  }, BorderPanel.Position.West)

  add(new TextArea(instructions) {
    border = BorderFactory.createEmptyBorder(0, 10, 0, 5)
    opaque = false
//    font = font.deriveFont(font.getSize2D - 1.0f)
    peer.setDisabledTextColor(Color.DARK_GRAY)
    enabled = false
  }, BorderPanel.Position.Center)

}