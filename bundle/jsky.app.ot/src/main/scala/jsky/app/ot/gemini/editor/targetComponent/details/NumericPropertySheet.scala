package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.{Insets, GridBagConstraints, GridBagLayout}
import javax.swing.{JLabel, JPanel}

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.Option
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.CoordinateParam
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.{TextBoxWidget, TextBoxWidgetWatcher, NumberBoxWidget}

import scalaz.std.list._
import scalaz.syntax.id._
import scalaz.syntax.functor._

// An editor for a list of doubles, with a titled border
case class NumericPropertySheet[A](title: String, f: SPTarget => A, props: NumericPropertySheet.Prop[A]*)
  extends JPanel with TelescopePosEditor with ReentrancyHack {

  private[this] var spt: SPTarget = new SPTarget // never null

  val pairs: List[(NumericPropertySheet.Prop[A], NumberBoxWidget)] =
    props.toList.fproduct { p =>
      new NumberBoxWidget {
        setColumns(15)
        setMinimumSize(getPreferredSize())
        addWatcher(new TextBoxWidgetWatcher {
          def textBoxKeyPress(tbwe: TextBoxWidget): Unit = textBoxAction(tbwe)
          def textBoxAction(tbwe: TextBoxWidget): Unit =
              try nonreentrant {
                p.g(f(spt), tbwe.getValue.toDouble)
                spt.notifyOfGenericUpdate()
              }
              catch { case _: NumberFormatException => }
        })
      }
    }

  setBorder(titleBorder(title));
  setLayout(new GridBagLayout)
  pairs.zipWithIndex.foreach { case ((p, w), row) =>
    val ins = new Insets(0, 2, 0, 2);
    add(new JLabel(p.leftCaption),new GridBagConstraints <| { c =>
      c.gridx = 0
      c.gridy = row
      c.fill = GridBagConstraints.HORIZONTAL
      c.insets = ins
    })
    add(w, new GridBagConstraints <| { c =>
      c.gridx = 1
      c.gridy = row
      c.insets = ins
    })
    add(new JLabel(p.rightCaption), new GridBagConstraints <| { c =>
      c.gridx = 2
      c.gridy = row
      c.fill = GridBagConstraints.HORIZONTAL
      c.weighty = 2
      c.insets = ins
    })
  }


  def edit(ctx: Option[ObsContext], target: SPTarget, node: ISPNode): Unit =
    nonreentrant {
      spt = target
      pairs.foreach { case (p, w) => w.setValue(p.f(f(target))) }
    }

}

object NumericPropertySheet {

  case class Prop[A](leftCaption: String, rightCaption: String, f: A => Double, g: (A, Double) => Unit)

  object Prop {
    def apply[A](leftCaption: String, rightCaption: String, f: A => CoordinateParam): Prop[A] =
      apply(leftCaption, rightCaption, a => f(a).getValue, (a, b) => f(a).setValue(b))
  }

}
