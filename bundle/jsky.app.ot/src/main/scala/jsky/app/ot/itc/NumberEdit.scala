package jsky.app.ot.itc

import jsky.util.gui.{NumberBoxWidget, TextBoxWidget, TextBoxWidgetWatcher}

import scala.swing.{Component, Label}
import scala.swing.event.ValueChanged

import scalaz._
import Scalaz._

/**
  * A light weight wrapper to turn NumberBoxWidget into a Scala swing component
  */
class NumberEdit(label: Label, units: Label, default: Double = 0) extends Component {
  override lazy val peer = new NumberBoxWidget {
    setColumns(6)
    setValue(default)
    setMinimumSize(getPreferredSize)
    addWatcher(new TextBoxWidgetWatcher {
      override def textBoxKeyPress(tbwe: TextBoxWidget): Unit = textBoxAction(tbwe)
      override def textBoxAction(tbwe: TextBoxWidget): Unit =
        try {
          publish(new ValueChanged(NumberEdit.this))
          tbwe.requestFocus()
        } catch {
          case _: NumberFormatException =>
        }
    })
  }

  override def enabled_=(e: Boolean) = {
    label.enabled = e
    peer.setEnabled(e)
    units.enabled = e
  }

  def value: Option[Double] =
    try {
      Some(peer.getValue.toDouble)
    } catch {
      case _: NumberFormatException => None
    }
}
