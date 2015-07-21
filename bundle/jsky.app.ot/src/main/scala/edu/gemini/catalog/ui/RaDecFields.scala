package edu.gemini.catalog.ui

import java.awt.Color
import java.text.ParseException
import javax.swing.JFormattedTextField
import javax.swing.JFormattedTextField.AbstractFormatter

import edu.gemini.spModel.core.{Angle, Declination, RightAscension}

import scala.swing.BorderPanel.Position._
import scala.swing.FormattedTextField.FocusLostBehavior
import scala.swing._
import scala.swing.event.{ButtonClicked, ValueChanged}
import scalaz.Scalaz._
import scalaz._

/** A swing AbstractFormatter that's more Scala-like */
abstract class Formatter[A] extends AbstractFormatter {
  final def stringToValue(s: String) = {
    val x = fromString(s).map(_.asInstanceOf[AnyRef])
    x.getOrElse(throw new ParseException(s, 0))
  }
  final def valueToString(a: Any) = {
    Option(a).map(v => toString(v.asInstanceOf[A])).getOrElse("")
  }
  def toString(a: A): String
  def fromString(s: String): NumberFormatException \/ A
}

class AngleTextField[A](initialValue: A, format : Formatter[A]) extends scala.swing.TextComponent {
  override lazy val peer : javax.swing.JFormattedTextField = new JFormattedTextField(format) with SuperMixin
  var valid = true
  peer.setValue(initialValue)

  /** Retrieve the field's value. */
  def value = peer.getValue.asInstanceOf[A] // no real choice; Java iface returns Object

  /** Set a new value for this field. */
  def value_=(a: A) {
    peer.setValue(a)
  }

  private val pink = new Color(255, 224, 224)
  private val white = background

  override def enabled_=(b:Boolean) {
    super.enabled = b
    background = if (b && !valid) pink else white
  }

  reactions += {
    case ValueChanged(_) => peer.getFormatter match {
      case f:Formatter[A] =>
        if (!text.isEmpty)
          validate(f)
    }
  }

  def validate(f:Formatter[A]) {
    valid = f.fromString(text).isRight
    background = if (valid) white else pink
  }

  focusLostBehavior = FormattedTextField.FocusLostBehavior.Commit

  def commitEdit(): Unit = {
    peer.commitEdit()
  }
  def editValid: Boolean = peer.isEditValid

  def focusLostBehavior: FocusLostBehavior.Value = FocusLostBehavior(peer.getFocusLostBehavior)
  def focusLostBehavior_=(b: FocusLostBehavior.Value) { peer.setFocusLostBehavior(b.id) }
}

case object HMSFormatter extends Formatter[Angle] {
  def toString(d: Angle) = d.formatHMS
  def fromString(s: String) = Angle.parseSexigesimal(s)
}

case object DMSFormatter extends Formatter[Angle] {
  def toString(d: Angle) = d.formatDMS
  def fromString(s: String) = Angle.parseDMS(s)
}

case object RAFormatter extends Formatter[RightAscension] {
  override def toString(a: RightAscension) = {
    a.toAngle.formatHMS
  }

  override def fromString(s: String) =
    for {
      v <- Angle.parseHMS(s)
    } yield RightAscension.fromAngle(v)
}

case object DecFormatter extends Formatter[Declination] {
  override def toString(a: Declination) = a.formatDMS

  override def fromString(s: String) =
    for {
      a <- Angle.parseDMS(s)
      d <- Declination.fromAngle(a) \/> new NumberFormatException(s"Cannot convert $a to declination")
    } yield d
}

class RATextField(initialValue: RightAscension) extends AngleTextField(initialValue, RAFormatter)
class DecTextField(initialValue: Declination) extends AngleTextField(initialValue, DecFormatter)

object FieldsDemo extends SwingApplication {

    def startup(args:Array[String]) {
      frame.visible = true
    }

    object frame extends MainFrame {
      val raField = new RATextField(RightAscension.zero) {
          reactions += {
            case ValueChanged(_) =>
              println("Valid RA ? " + value + " " + valid)
          }
        }
      val decField = new DecTextField(Declination.zero) {
          reactions += {
            case ValueChanged(_) =>
              println("Valid Dec? " + value + " " + valid)
          }
        }

      contents = new BorderPanel {

        add(raField, North)
        add(decField, Center)
        add(new Button("Print") {
          reactions += {
            case ButtonClicked(_) =>
              println("RA/Dec " + raField.value + "/" + decField.value)
          }
        }, South)

      }
    }

}