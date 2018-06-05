package jsky.app.ot.gemini.editor.targetComponent.details2

import java.awt.{GridBagConstraints, GridBagLayout, Insets}
import java.text.{DecimalFormat, NumberFormat, ParseException}
import java.util.Locale
import javax.swing.JPanel

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => JOption}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.app.ot.gemini.editor.targetComponent.details2.NumericPropertySheet.Prop
import jsky.util.gui.{NumberBoxWidget, TextBoxWidget, TextBoxWidgetWatcher}
import squants.{Quantity, UnitOfMeasure}

import scala.swing.ListView.Renderer
import scala.swing._
import scala.swing.event.SelectionChanged
import scalaz.std.list._
import scalaz.syntax.functor._
import scalaz.syntax.id._

// An editor for a list of doubles, with a titled border
case class NumericPropertySheet[A](title: Option[String], f: SPTarget => A, props: NumericPropertySheet.Prop[A, _]*)
  extends JPanel with TelescopePosEditor[SPTarget] with Publisher with ReentrancyHack {

  private[this] var spt: SPTarget = new SPTarget // never null

  val pairs: List[(NumericPropertySheet.Prop[A, _], NumberBoxWidget)] = props.toList.fproduct(editField)

  title.foreach(t => setBorder(titleBorder(t)))
  setLayout(new GridBagLayout)
  pairs.zipWithIndex.foreach { case ((p, w), row) =>
    val ins = new Insets(0, 2, 0, 2)
    add(p.leftComponent.peer, new GridBagConstraints <| { c =>
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
    add(p.rightComponent.peer, new GridBagConstraints <| { c =>
      c.gridx = 2
      c.gridy = row
      c.weightx = 1
      c.fill = GridBagConstraints.HORIZONTAL
      c.insets = ins
    })

    // if the right component is a unit selector we need to
    // update the value every time the unit is changed
    Option(p.rightComponent).foreach {
      case c: ComboBox[_] =>
        listenTo(c.selection)
        reactions += {
          case SelectionChanged(`c`) =>
            updateField(p, w)
        }
      case _ =>
    }
    // if the left side component is a selector we need to support
    // transforming the value on the text without writing it back to the
    // model until the user edits it
    Option(p.leftComponent).foreach {
      case c: ComboBox[_] =>
        listenTo(c.selection)
        reactions += {
          case SelectionChanged(`c`) =>
            transformField(p, w)
        }
      case _ =>
    }

  }


  def edit(ctx: JOption[ObsContext], target: SPTarget, node: ISPNode): Unit =
    nonreentrant {
      spt = target
      pairs.foreach { case (p, w) => p.init(w, f(target)) }
    }

  private def editField(p: Prop[A, _]) =
    new NumberBoxWidget {
      setColumns(15)
      setMinimumSize(getPreferredSize)
      addWatcher(new TextBoxWidgetWatcher {
        def textBoxKeyPress(tbwe: TextBoxWidget): Unit = updateField(p, tbwe)
        def textBoxAction(tbwe: TextBoxWidget): Unit   = updateField(p, tbwe)

      })
    }

  private def updateField(p: Prop[A, _], tbwe: TextBoxWidget) =
    try
      nonreentrant {
        p.edit(f(spt), parse(tbwe.getValue))
      }
    catch { case _: ParseException => }

  private def transformField(p: Prop[A, _], tbwe: TextBoxWidget) =
    try {
      pairs.find(_._1 == p).foreach { w =>
        p.transform(w._2, f(spt), parse(tbwe.getValue))
      }
    } catch { case _: ParseException => }

  private def parse(s: String) =
    NumericPropertySheet.ParseFormatter.
      parse(s.toUpperCase). // turn 1.23e-3 into upper case 1.23E-3; NumericFormatter only allows capital E!
      doubleValue()         // formatter deals with empty string and incomplete exp numbers like 1e 1e- etc

}

object NumericPropertySheet {

  private val MaxFractionDigits = 12
  private val ExpLimit          = 6
  private val SmallLimit        = Math.pow(10, -ExpLimit)
  private val BigLimit          = Math.pow(10, ExpLimit)

  private val ExpNumbersFormatter: NumberFormat =
    new DecimalFormat("0.#E0") <| { f =>
      f.setMaximumFractionDigits(MaxFractionDigits)
    }

  private val NumbersFormatter: NumberFormat =
    NumberFormat.getInstance(Locale.US) <| { f =>
      f.setMaximumFractionDigits(MaxFractionDigits)
      f.setGroupingUsed(false)
    }

  private val ParseFormatter = NumbersFormatter

  sealed trait Prop[A, B] {
    def leftComponent:  Component
    def rightComponent: Component
    def edit: (A, Double)                       => Unit
    def transform: (NumberBoxWidget, A, Double) => Unit = (_, _, _) => {}
    def init: (NumberBoxWidget, A)              => Unit
    def format(v: Double) = formatter(v).format(v)
    def formatter(v: Double) = v match {
      case d if Math.abs(d) < SmallLimit && d != 0 => ExpNumbersFormatter // use exponential notation for small numbers
      case d if Math.abs(d) > BigLimit             => ExpNumbersFormatter // use exponential notation for big numbers
      case d                                       => NumbersFormatter    // use standard notation for all others
    }
  }

  case class DoubleProp[A](
    leftCaption: String,
    rightCaption: String,
    get: A => Double,
    edit: (A, Double) => Unit
  ) extends Prop[A, Double] {

    val leftComponent =
      new Label(leftCaption) {
        horizontalAlignment = Alignment.Left
      }

    val rightComponent =
      new Label(rightCaption) {
        horizontalAlignment = Alignment.Left
      }

    def init: (NumberBoxWidget, A) => Unit = { (w,a) =>
      w.setValue(format(get(a)))
    }

  }

  case class TransformableProp[A, B](
    leftOptions: List[B],
    rightCaptions: Map[B, String],
    initial: B,
    render: B => String,
    get: (A, B) => Double,
    set: (A, B, Double) => Unit,
    format: B => NumberFormat
  ) extends Prop[A, Double] {

    val rightComponent =
      new Label(rightCaptions.getOrElse(initial, "")) {
        horizontalAlignment = Alignment.Left
      }

    val leftComponent  =
      new ComboBox[B](leftOptions) {
        selection.item = initial
        renderer = Renderer(render)
        listenTo(selection)
        reactions += {
          case SelectionChanged(_) =>
            rightComponent.text = rightCaptions.getOrElse(selection.item, "")
        }
      }

    override def formatter(v: Double) =
      format(leftComponent.selection.item)

    def edit: (A, Double) => Unit = { (a, d) =>
      set(a, leftComponent.selection.item, d)
    }

    override def transform: (NumberBoxWidget, A, Double) => Unit =  { (w, a, d) =>
      w.setValue(format(get(a, leftComponent.selection.item)))
    }

    def init: (NumberBoxWidget, A) => Unit  = { (w, a) =>
      w.setValue(format(get(a, leftComponent.selection.item)))
    }

  }

  case class SingleUnitQuantity[A, B <: Quantity[B]](
    leftCaption: String,
    unit: UnitOfMeasure[B],
    get: A => B,
    set: (A, B) => Unit
  ) extends Prop[A, B] {

    val leftComponent =
      new Label(leftCaption) {
        horizontalAlignment = Alignment.Left
      }

    val rightComponent =
      new Label(unit.symbol)  {
        horizontalAlignment = Alignment.Left
      }

    def edit: (A, Double) => Unit = { (a, d) =>
      set(a, unit(d))
    }

    def init: (NumberBoxWidget, A) => Unit = { (w, a) =>
      w.setValue(format(get(a).value))
    }

  }

  case class MultiUnitQuantity[A, B <: Quantity[B]](
    leftCaption: String,
    units: Seq[UnitOfMeasure[B]],
    get: A => B,
    set: (A, B) => Unit
  ) extends Prop[A, B] {

    val leftComponent =
      new Label(leftCaption) {
        horizontalAlignment = Alignment.Left
      }

    val rightComponent =
      new ComboBox[UnitOfMeasure[B]](units) {
        renderer = Renderer(_.symbol)
      }

    def unit= rightComponent.selection.item

    def edit: (A, Double) => Unit = {
      (a, d) => set(a, unit(d))
    }

    def init: (NumberBoxWidget, A) => Unit = { (w, a) =>
      w.setValue(format(get(a).value))
      rightComponent.selection.item = get(a).unit
    }

  }

  object Prop {

    def apply[A](leftCaption: String, rightCaption: String, f: A => Double, g: (A, Double) => Unit): Prop[A, Double] =
      DoubleProp(leftCaption, rightCaption, f, g)

    def apply[A, B](leftOptions: List[B], rightCaptions: Map[B, String], initial: B, render: B => String, f: (A, B) => Double, g: (A, B, Double) => Unit, h: B => NumberFormat): Prop[A, Double] =
      TransformableProp(leftOptions, rightCaptions, initial, render, f, g, h)

    def apply[A, B <: Quantity[B]](leftCaption: String, get: A => B, set: (A, B) => Unit, units: UnitOfMeasure[B]*): Prop[A, B] =
      units.length match {
        case 0      => sys.error("quantity property needs at least one unit")
        case 1      => SingleUnitQuantity(leftCaption, units.head, get, set)
        case _      => MultiUnitQuantity(leftCaption,  units,      get, set)
      }

  }

}
