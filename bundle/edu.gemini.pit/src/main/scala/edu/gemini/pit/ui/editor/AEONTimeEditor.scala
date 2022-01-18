package edu.gemini.pit.ui.editor

import edu.gemini.pit.ui.view.partner.PartnersFlags
import edu.gemini.model.p1.immutable._
import edu.gemini.shared.gui.textComponent.NumberField
import com.jgoodies.forms.factories.Borders._

import swing._
import event.ValueChanged
import Swing._
import edu.gemini.pit.ui.util.{Rows, SharedIcons, StdModalEditor, ValueRenderer}
import scalaz._
import Scalaz._
import edu.gemini.pit.ui.binding.Bound
import edu.gemini.pit.ui.util.gface.SimpleListViewer
import edu.gemini.ui.gface.{GSubElementDecorator, GViewer}
import scalaz.Reducer.AnyReducer.monoid

import java.awt.{BorderLayout, Color}
import javax.swing.{BorderFactory, JLabel, SwingConstants}
import javax.swing.border.Border

object AEONTimeEditor {

  def open[A](table: Component, parent: UIElement) = new AEONTimeEditor(table).open(parent)

}

class AEONTimeEditor[A] private (table: Component) extends StdModalEditor[List[GeminiTimeRequired]]("Gemini Time Required") {

  // Editor component
  object editor extends GridBagPanel with Rows {
    addRow(new Label("""<html>Please indicate whether the time for a site/instrument is required<br/>of a multi-facility proposal, e.g. the project is infeasible without it.</html>"""))
    addRow(new Label("""<html>This information will be used for scheduling purposes<br/> and will not be shown to the TACs.</html>"""))
    addSpacer()
    addRow(table)
  }
  //
  def value: List[GeminiTimeRequired] = Nil
}

