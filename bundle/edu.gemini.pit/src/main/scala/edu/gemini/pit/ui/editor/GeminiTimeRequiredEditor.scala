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

  def open[A](model: List[GeminiTimeRequired], parent: UIElement): Option[List[GeminiTimeRequired]] = new GeminiTimeRequiredEditor(model).open(parent)

}

class GeminiTimeRequiredEditor[A] private(model: List[GeminiTimeRequired]) extends StdModalEditor[List[GeminiTimeRequired]]("Gemini Time Required") {

  lazy val table = GeminiTimeRequiredTable(model)
  // Editor component
  object editor extends GridBagPanel with Rows {
    addRow(new Label("""<html>Please indicate whether the time for a site/instrument is required<br/>of a multi-facility proposal, e.g. the project is infeasible without it.</html>"""))
    addRow(new Label("""<html>This information will be used for scheduling purposes<br/> and will not be shown to the TACs.</html>"""))
    addSpacer()
    addRow(table)
  }
  //
  def value: List[GeminiTimeRequired] = table.currentModel.getOrElse(Nil)

}

final case class GeminiTimeRequiredTable(initial: List[GeminiTimeRequired]) extends SimpleListViewer[List[GeminiTimeRequired], List[GeminiTimeRequired], GeminiTimeRequired] {
  // Lens
  val lens = Lens.lensId[List[GeminiTimeRequired]]

  // Initial binding
  bind(initial.some, _ => ())

  border = BorderFactory.createLineBorder(Color.lightGray)

  object columns extends Enumeration {
    val Site, Instrument, Required = Value
  }

  import columns._

  def columnWidth = {
    case Site        => (160, Int.MaxValue)
    case Instrument  => (160, 160)
    case Required    => (70, 70)
  }

  def size(p: List[GeminiTimeRequired]): Int = {
    p.length
  }

  def currentModel = model

  def elementAt(p: List[GeminiTimeRequired], i: Int): GeminiTimeRequired = p(i)

  override def alignment(s: GeminiTimeRequired) = {
    case Required => SwingConstants.CENTER
    case _        => SwingConstants.LEFT
  }

  def icon(s: GeminiTimeRequired) = {
    case Required       =>
      if (s.required) SharedIcons.CHECK_SELECTED else SharedIcons.CHECK_UNSELECTED
  }

  def text(s: GeminiTimeRequired) = {
      case Site       => s.site.name
      case Instrument => s.instrument.display
      case Required   => ""
  }

  onClick { ed =>
    val newModel = model.map(_.collect {
              case GeminiTimeRequired(s, i, r) if s == ed.site && i == ed.instrument =>
                GeminiTimeRequired(s, i, !ed.required)
              case g => g
            })
    // Tricky, this actually sets the new model
    bind(newModel, _ => ())
  }
}
