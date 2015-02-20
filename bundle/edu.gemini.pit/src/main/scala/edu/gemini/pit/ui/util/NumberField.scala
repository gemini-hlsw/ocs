package edu.gemini.pit.ui.util

import scala.swing.FormattedTextField

import java.awt.Color
import swing.event.ValueChanged
import java.text.{ParsePosition, DecimalFormat}

// A class for numeric fields.
object NumberField {
  private lazy val df = {
    val df = new DecimalFormat
    df.setMinimumFractionDigits(1)
    df.setMinimumIntegerDigits(1)
    df.setGroupingUsed(false)
    df
  }
}

class NumberField(d: Option[Double]) extends FormattedTextField(NumberField.df) with SelectOnFocus {
  d.orElse(Some(0)).foreach { d =>
    text = NumberField.df.format(d)
    commitEdit()
  }

  private val pink = new Color(255, 224, 224)
  private var white = background // N.B. this means we can't change the background to anything else

  var valid = true

  def valid(d:Double):Boolean = true

  override def enabled_=(b:Boolean) {
    super.enabled = b
    background = if (b && !valid) pink else white
  }

  reactions += {
    case ValueChanged(_) =>

      // REL-2253 For some reason on OSX you get sometimes an extra value change event with an empty text.
      // This creates a null parsed value and the validation fails
      // This is not very noticeable as there is a second event with the valid text
      // However, it means that sometimes you need to press Ok twice
      if (text.nonEmpty) {
        valid = try {
          val pp = new ParsePosition(0)
          val parsed = NumberField.df.parse(text, pp)
          (pp.getIndex == text.length()) && valid(parsed.doubleValue)
        } catch {
          case _: Exception => false
        }
        
        background = if (valid) white else pink
      }
  }

  focusLostBehavior = FormattedTextField.FocusLostBehavior.Commit // ?
}