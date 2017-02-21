package edu.gemini.shared.gui.textComponent

import scala.swing.FormattedTextField

import java.awt.Color
import scala.swing.event.ValueChanged
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

  object TimeFormatter extends DecimalFormat {
    setMaximumFractionDigits(2)
    setMinimumFractionDigits(2)
    setMinimumIntegerDigits(1)
    setGroupingUsed(false)
  }
}

class NumberField(d: Option[Double], allowEmpty: Boolean, format: java.text.Format = NumberField.df) extends FormattedTextField(format) with SelectOnFocus {
  d.orElse(Some(0)).foreach { d =>
    text = format.format(d)
    commitEdit()
  }

  private val pink = new Color(255, 224, 224)
  private val white = background // N.B. this means we can't change the background to anything else

  var valid = true

  def valid(d:Double):Boolean = true

  def value: Option[Double] = Option(peer.getValue).map(_.asInstanceOf[Double])
  def value_=(v: Double): Unit = peer.setValue(v)

  override def enabled_=(b:Boolean) {
    super.enabled = b
    background = if (b && !valid) pink else white
  }

  reactions += {
    case ValueChanged(e) =>
      // REL-2253 Due to the way scala swing handles a text component you get an extra
      // ValueChange event with an empty text when the event has focus
      // This creates a null parsed value and the validation fails
      // This is not very noticeable as there is a second event with the valid text
      // However, it means that sometimes you need to press Ok twice
      // We can workaround this bug by validating only if the field has focus
      if (this.hasFocus) {
        if (text.nonEmpty) {
          valid = try {
            val pp = new ParsePosition(0)
            val parsed = NumberField.df.parse(text, pp)
            (pp.getIndex == text.length()) && valid(parsed.doubleValue)
          } catch {
            case _: Exception => false
          }

          background = if (valid) white else pink
        } else {
          valid = allowEmpty
        }
      }
  }

  focusLostBehavior = FormattedTextField.FocusLostBehavior.Commit // ?

}
