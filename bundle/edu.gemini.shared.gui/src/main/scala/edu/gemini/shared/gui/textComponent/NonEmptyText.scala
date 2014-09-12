package edu.gemini.shared.gui.textComponent

import java.awt.Color
import scala.swing.TextComponent
import scala.swing.event.ValueChanged

// Stolen from PIT

trait NonEmptyText { this: TextComponent =>

  private val pink  = new Color(255, 224, 224)
  private val white = background // N.B. this means we can't change the background to anything else

  def valid = !text.isEmpty

  reactions += {
    case ValueChanged(_) => handleValidation()
  }

  private def handleValidation(): Unit = {
    background = if (valid) white else pink
  }

  handleValidation()
}
