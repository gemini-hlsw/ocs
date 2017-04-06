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
class SearchPanel(statusFiler: StatusFilter, tree: SPTree) extends GridBagPanel {
  import SearchPanel._

  private var viewer = Option.empty[SPViewer]
  private var zip    = Option.empty[Zipper[ISPNode]]

  private def resetZipper: Unit = {
    zip = None
  }

  def searchString: Option[String] =
    Option(textBox.text).filterNot(_.isEmpty)

  def searchNumber: Option[Int] =
    for {
      s <- searchString
      i <- \/.fromTryCatchNonFatal(s.toInt).toOption
    } yield i

  object nextAction extends Action("Next") {

    def matcher(s: String): ISPNode => Boolean =
      searchNumber.fold(titleMatcher(s)) { obsNumberMatcher }

    def initZipper: Option[Zipper[ISPNode]] =
      for {
        v <- viewer
        s <- searchString
        t <- Option(v.getTree)
        n <- Option(t.getCurrentNode)
        z <- n.zipper(matcher(s))
      } yield (z.focus.getNodeKey === n.getNodeKey) ? z.nextC | z

    def apply: Unit = {
      // If we have a zipper setup, move the focus to the next item.  Otherwise
      // initialize the zipper.
      zip = zip.map(_.nextC) orElse initZipper
      zip match {
        case None    =>
          val msg = searchNumber.map(n => s"Sorry, there is no observation $n in this program.") orElse
                      searchString.map(s => s"Sorry, no node titles contain the text: $s.")

          msg.foreach { m =>
            JOptionPane.showMessageDialog(SearchPanel.this.peer, m, "No Results", JOptionPane.WARNING_MESSAGE)
          }
        case Some(z) =>
          // statusFilter.setStatusEnabled()  TODO: is this necessary?
          ViewerManager.open(z.focus)
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
    tooltip = "Show program node.  Enter observation number or search text and hit <Enter>"

    // When anything changes in the text field, reset the zipper.
    peer.getDocument.addDocumentListener(new DocumentListener() {
      override def insertUpdate(e: DocumentEvent): Unit  = resetZipper
      override def removeUpdate(e: DocumentEvent): Unit  = resetZipper
      override def changedUpdate(e: DocumentEvent): Unit = resetZipper
    })

    // If we move away to edit anything, reset the zipper.
    reactions += {
      case FocusLost(_, _, false) => resetZipper
    }

    listenTo(this)
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
  // A search for a number is assumed to be an observation number.  If you
  // want to search all nodes for one with a title including the number you
  // can include it in quotes.
  private val QuotedString = """"([^"]*)"""".r

  private def dataObjectMatcher(f: ISPDataObject => Boolean): ISPNode => Boolean =
    (n: ISPNode) =>
      n.getDataObject match {
        case d: ISPDataObject => f(d)
        case _                => false
      }

  private def titleMatcher(text: String): ISPNode => Boolean = {
    val textʹ = text.toLowerCase.trim match {
      case QuotedString(s) => s
      case s               => s
    }

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
}
