package edu.gemini.pit.ui.util

import swing.TextComponent
import java.awt.Color
import swing.event.ValueChanged

trait NonEmptyText { this: TextComponent =>

  private val pink = new Color(255, 224, 224)
  private var white = background // N.B. this means we can't change the background to anything else

  def valid = !text.isEmpty

  reactions += {
    case ValueChanged(_) => handleValidation()
  }

  private def handleValidation() {
    background = if (valid) white else pink
  }

  handleValidation()

}
