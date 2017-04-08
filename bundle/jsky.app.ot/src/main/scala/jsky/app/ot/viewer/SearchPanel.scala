package jsky.app.ot.viewer

import edu.gemini.pot.sp.{ISPNode, ISPObservation}
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.rich.pot.sp._

import javax.swing.JOptionPane
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

import scala.swing._
import scala.swing.GridBagPanel._
import scala.swing.Swing.EmptyBorder
import scala.swing.event.FocusLost

import scalaz._
import Scalaz._

/** Defines a label and text box to use for searching for program nodes.
  */
class SearchPanel(statusFilter: StatusFilter, tree: SPTree) extends GridBagPanel {
  import SearchPanel._

  private var viewer = Option.empty[SPViewer]

  def searchString: Option[String] =
    Option(textBox.text).filterNot(_.isEmpty)

  object nextAction extends Action("Next") {

    def apply: Unit = {
      val focus = for {
        v     <- viewer
        s     <- searchString
        t     <- Option(v.getTree)
        n     <- Option(t.getCurrentNode)
        p     <- Option(n.getProgram)
        (l, r) = p.toStream.span(_.getNodeKey =/= n.getNodeKey)
        m      = matcher(s)
        f     <- r.drop(1).find(m) orElse l.find(m) orElse r.headOption.filter(m)
      } yield f

      focus match {
        case None    =>
          searchString.foreach { s =>
            val m = s"""Sorry, no matching nodes for search string "$s"."""
            JOptionPane.showMessageDialog(SearchPanel.this.peer, m, "No Results", JOptionPane.WARNING_MESSAGE)
          }

        case Some(n) =>
          // The original "ObsSearchPanel" did this so that any matches in
          // filtered out nodes will be displayed.  Of course it also clobbers
          // whatever filters you had setup regardless of whether there are any
          // matches. Perhaps a better approach would be to skip filtered out
          // observations but I'll just keep the original behavior for now.
          statusFilter.setStatusEnabled()

          ViewerManager.open(n)
      }
    }
  }

  border = EmptyBorder(1, 1, 1, 1)

  layout(new Label("Show")) = new Constraints() {
    gridx  = 1
    insets = new Insets(0,0,0,2)
  }

  object textBox extends TextField {
    action  = nextAction
    tooltip = "Show program node.  Enter observation number or search text and press <Enter>."
  }

  layout(textBox) = new Constraints() {
    gridx   = 2
    fill    = Fill.Horizontal
    weightx = 1.0
  }

  def setViewer(v: SPViewer): Unit = {
    viewer = Option(v)
  }
}

object SearchPanel {
  // A search for a number is assumed to be an observation number.  If you want
  // to search all nodes for one with a title containing the number you can
  // include it in quotes.
  private val ObsNumber    = """(\d+)""".r
  private val QuotedString = """"([^"]*)"""".r

  private def dataObjectMatcher(f: ISPDataObject => Boolean): ISPNode => Boolean =
    (n: ISPNode) =>
      n.getDataObject match {
        case d: ISPDataObject => f(d)
        case _                => false
      }

  private def titleMatcher(text: String): ISPNode => Boolean = {
    val textʹ = text.toLowerCase.trim
    dataObjectMatcher { obj =>
      Option(obj.getTitle).map(_.toLowerCase).exists(_.contains(textʹ))
    }
  }

  private def obsNumberMatcher(num: Int): ISPNode => Boolean =
    (n: ISPNode) =>
      n match {
        case o: ISPObservation => o.getObservationNumber === num
        case _                 => false
      }

  private def matcher(s: String): ISPNode => Boolean =
    s match {
      case ObsNumber(n)     => obsNumberMatcher(n.toInt)
      case QuotedString(sʹ) => titleMatcher(sʹ)
      case _                => titleMatcher(s)
    }
}
