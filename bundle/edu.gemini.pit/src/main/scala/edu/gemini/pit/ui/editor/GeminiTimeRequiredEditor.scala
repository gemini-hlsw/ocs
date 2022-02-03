package edu.gemini.pit.ui.editor

import edu.gemini.model.p1.immutable._

import swing._
import edu.gemini.pit.ui.util.{Rows, SharedIcons, StdModalEditor}
import scalaz._
import Scalaz._
import edu.gemini.pit.ui.util.gface.SimpleListViewer

import java.awt.Color
import javax.swing.{BorderFactory, SwingConstants}

object GeminiTimeRequiredEditor {

  def open[A](model: List[(Option[TimeAmount], GeminiTimeRequired)], parent: UIElement): Option[List[GeminiTimeRequired]] = new GeminiTimeRequiredEditor(model).open(parent)

}

class GeminiTimeRequiredEditor[A] private(model: List[(Option[TimeAmount], GeminiTimeRequired)]) extends StdModalEditor[List[GeminiTimeRequired]]("Gemini Time Required") {

  lazy val table = GeminiTimeRequiredTable(model)
  // Editor component
  object editor extends GridBagPanel with Rows {
    addRow(new Label("""<html>Please indicate whether the time for each site/instrument is a required<br/> part of a multi-facility proposal, e.g. the project is infeasible without it.</html>"""))
    addRow(new Label("""<html>This information will be used for scheduling purposes and will not be<br/> shown to the TACs</html>"""))
    addSpacer()
    addRow(table)
  }
  //
  def value: List[GeminiTimeRequired] = table.currentModel.map(_.map(_._2)).getOrElse(Nil)

}

final case class GeminiTimeRequiredTable(initial: List[(Option[TimeAmount], GeminiTimeRequired)]) extends SimpleListViewer[List[(Option[TimeAmount], GeminiTimeRequired)], List[(Option[TimeAmount], GeminiTimeRequired)], (Option[TimeAmount], GeminiTimeRequired)] {
  // Lens
  val lens = Lens.lensId[List[(Option[TimeAmount], GeminiTimeRequired)]]

  // Initial binding
  bind(initial.some, _ => ())

  border = BorderFactory.createLineBorder(Color.lightGray)

  object columns extends Enumeration {
    val Site, Instrument, Time, Required = Value
  }

  import columns._

  def columnWidth = {
    case Site        => (140, Int.MaxValue)
    case Instrument  => (110, 110)
    case Time        => (70, 70)
    case Required    => (70, 70)
  }

  def size(p: List[(Option[TimeAmount], GeminiTimeRequired)]): Int = {
    p.length
  }

  def currentModel = model

  def elementAt(p: List[(Option[TimeAmount], GeminiTimeRequired)], i: Int): (Option[TimeAmount], GeminiTimeRequired) = p(i)

  override def alignment(s: (Option[TimeAmount], GeminiTimeRequired)) = {
    case Required => SwingConstants.CENTER
    case _        => SwingConstants.LEFT
  }

  def icon(s: (Option[TimeAmount], GeminiTimeRequired)) = {
    case Required       =>
      if (s._2.required) SharedIcons.CHECK_SELECTED else SharedIcons.CHECK_UNSELECTED
  }

  def text(s: (Option[TimeAmount], GeminiTimeRequired)) = {
      case Site       => s._2.site.name
      case Instrument => s._2.instrument.display
      case Time       => s._1.foldMap(t => s"${t.toHours.value} h")
      case Required   => ""
  }

  onClick { ed =>
    val newModel = model.map(_.map {
              case (t, GeminiTimeRequired(s, i, r)) if s == ed._2.site && i == ed._2.instrument =>
                (t, GeminiTimeRequired(s, i, !ed._2.required))
              case g => g
            })
    // Tricky, this actually sets the new model
    bind(newModel, _ => ())
  }
}
