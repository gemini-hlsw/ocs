package edu.gemini.pit.ui.util

import edu.gemini.pit.ui.util.MultiFormatTextField.Formatter

import java.text.ParseException

import edu.gemini.shared.gui.textComponent.SelectOnFocus
import edu.gemini.spModel.core.{RightAscension, Declination}

import scala.swing._
import scala.swing.BorderPanel.Position._
import scala.swing.Swing.pair2Dimension
import scala.swing.event._

import javax.swing.JFormattedTextField.AbstractFormatter
import javax.swing.text.DefaultFormatterFactory
import java.awt.Color

/**
 * A formatted text field that contains a value of type A and allows the user to select among
 * one or more possible input/display formats. Note that there is no notion of "empty" for this
 * control; it always has a value.
 */
class MultiFormatTextField[A](initialValue: A, f: Formatter[A], fs: Formatter[A]*) extends BorderPanel { mftf =>
  override def enabled_=(b:Boolean) {
    super.enabled = b
    Text.enabled = b
  }
  
  /** Retrieve the field's value. */
  def value = Text.peer.getValue.asInstanceOf[A] // no real choice; Java iface returns Object

  /** Set a new value for this field. */
  def value_=(a: A) {
    Text.peer.setValue(a)
  }

  var valid = true // always, initially

  // We have two controls, defined below
  add(Text, Center)
  add(Button, East)

  // Initialize
  Button.doClick(1)
  value = initialValue

  // Text is just a formatted text field
  object Text extends FormattedTextField(null) with SelectOnFocus {

    private val pink = new Color(255, 224, 224)
    private var white = background // N.B. this means we can't change the background to anything else

    override def enabled_=(b:Boolean) {
      super.enabled = b
      background = if (b && !valid) pink else white
    }

    reactions += {
      case ValueChanged(_) => peer.getFormatter match {
        case f:Formatter[A] => validate(f)          
      }
    }

    def validate(f:Formatter[A]) {
      valid = f.fromString(text).isDefined
      background = if (valid) white else pink
      Button.enabled = valid
      mftf.publish(new ValueChanged(mftf))
    }

    focusLostBehavior = FormattedTextField.FocusLostBehavior.Commit // ?

  }

  // The button does the real work
  object Button extends Button {

    // We take f and fs to guarantee at least one, but we fold them together here
    private val config = f :: fs.toList
    private val loop = config.cycle // an infinite cycle of formatters

    // Shrink our font a little bit
    font = font.deriveFont(font.getSize - 3f)

    // We don't want this button to accept focus
    peer.setFocusable(false)

    // Calculate the minimum width based on the captions
    minimumSize = (config.map { f => text = f.caption; peer.getPreferredSize.width }.max, 0)
    preferredSize = minimumSize

    // On click, swap out our caption and formatter
    reactions += {
      case ButtonClicked(_) =>
        val f = loop.next()
        text = f.caption
        Text.peer.setFormatterFactory(new DefaultFormatterFactory(f))
        Swing.onEDT {
          Text.peer.selectAll()
        }
    }

  }

  // TODO: this is ugly
  def selectFormat(caption:String) {
    if ((f :: fs.toList).exists(_.caption == caption)) {
      while (Button.text != caption)
        Button.doClick(1)
    }
  }
  
}

object MultiFormatTextField {

  /** A swing AbstractFormatter that's more Scala-like */
  abstract class Formatter[A](val caption: String) extends AbstractFormatter {
    final def stringToValue(s: String) = {
      val x = fromString(s).map(_.asInstanceOf[AnyRef])
      // println("%s: parsing %s => %s".format(caption, s, x))
      x.getOrElse(throw new ParseException(s, 0))
    }
    final def valueToString(a: Any) = {
      // println("%s: unparsing %s".format(caption, a))
      toString(a.asInstanceOf[A])
    }
    def toString(a: A): String
    def fromString(s: String): Option[A]
  }

}


object MFTFTest extends SwingApplication {
  
  def startup(args:Array[String]) {
    frame.visible = true
  }

  object frame extends MainFrame {

    contents = new BorderPanel {

      add(new RATextField(RightAscension.zero) {
        reactions += {
          case ValueChanged(_) =>
            println("Valid? " + valid)
        }
      }, North)

      add(new DecTextField(Declination.zero) {
        reactions += {
          case ValueChanged(_) =>
            println("Valid? " + valid)
        }
      }, South)

    }
  }

}
