package edu.gemini.shared.gui.textComponent

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
  d.orElse(Some(0))foreach { d =>
    import NumberField.df
    text = df.format(d)
    commitEdit()
  }

  private val pink = new Color(255, 224, 224)
  private val white = background // N.B. this means we can't change the background to anything else

  var valid = true

  def valid(d:Double):Boolean = true

  override def enabled_=(b:Boolean) {
    super.enabled = b
    background = if (b && !valid) pink else white
  }

  reactions += {
    case ValueChanged(_) =>

      valid = try {
        val pp = new ParsePosition(0)
        valid(NumberField.df.parse(text, pp).doubleValue) && (pp.getIndex == text.length())
      } catch {
        case _:Exception => false
      }

      background = if (valid) white else pink

  }

  focusLostBehavior = FormattedTextField.FocusLostBehavior.Commit // ?

}
