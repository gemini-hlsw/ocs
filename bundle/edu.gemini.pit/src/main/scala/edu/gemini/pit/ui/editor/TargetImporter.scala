package edu.gemini.pit.ui.editor

import edu.gemini.model.p1.immutable._

import swing.BorderPanel.Position._
import javax.swing.BorderFactory.createEmptyBorder
import java.io.File

import TargetImporter._
import edu.gemini.model.p1.targetio.api._
import edu.gemini.model.p1.targetio.impl._
import edu.gemini.pit.ui.util.gface.SimpleListViewer

import scalaz.Lens
import swing._
import event.ButtonClicked
import edu.gemini.pit.ui.util._
import java.awt.CardLayout
import javax.swing.JOptionPane

import edu.gemini.shared.gui.Chooser

object TargetImporter {

  // Our possible states
  sealed trait State
  private case object Begin extends State
  private case class Selected(file:File) extends State
  private case class Errors(file:File, errors:List[ParseError], targets:List[Target]) extends State
  private case class Done(file:File, errors:List[ParseError], targets:List[Target]) extends State

  // Entry point
  def open(parent:UIElement) = new TargetImporter().open(parent)

}

class TargetImporter private extends StdModalWizard[State, List[Target]]("Import Targets", Begin) {wiz =>

  // Respond to state changes
  def apply(s:State) {
    s match {

      // Our initial state
      case Begin =>
        page1.file.label.text = " <none>"
        editor.setPage(page1)
        enable(false, false, false)

      // A file was just selected
      case Selected(f) =>
        page1.file.label.text = " %s (in folder %s)".format(f.getName, Option(f.getParentFile).map(_.getName).getOrElse("<none>"))
        editor.setPage(page1)
        enable(false, true, false)

      // We just received parse errors
      case Errors(_, es, ts) =>
        page2.header.text = "%s of %s target(s) could not be read:".format(es.length, es.length + ts.length)
        page2.listView.bind(Some(es), (x:Option[List[ParseError]]) => sys.error("model is immutable"))
        editor.setPage(page2)
        enable(true, !ts.isEmpty, false)

      // Done
      case Done(_, es, ts) =>
        page3.header.text = "Ready to import %s of %s target(s):".format(ts.length, es.length + ts.length)
        page3.listView.bind(Some(ts), (x:Option[List[Target]]) => sys.error("model is immutable"))
        editor.setPage(page3)
        enable(true, false, true)

    }
  }

  // Respond to the next button
  def next() {
    state match {

      // A file is selected, next step is to parse it
      case Selected(f) => state = read(AnyTargetReader, f)

      // Errors, but the user chose to ignore them
      case Errors(f, es, ts) => state = Done(f, es, ts)

      // All other cases are illegal
      case s => sys.error("next() shouldn't ever happen if state is " + s)
    }
  }

  // Respond to the back button
  def back() {
    state match {

      // Reconstruct previous state.
      case Errors(f, _, _) => state = Selected(f)
      case Done(f, Nil, _) => state = Selected(f)
      case Done(f, es, ts) => state = Errors(f, es, ts)
      case s => sys.error("back() shouldn't ever happen if state is " + s)

    }
  }

  // Respond to the finish button
  def finish = state match {
    case Done(_, _, ts) => ts
    case s => sys.error("finish shouldn't ever happen if state is " + s)
  }


  object editor extends Panel {
    private val cl = new CardLayout
    peer.setLayout(cl)

    def add(c:Component) {
      peer.add(c.peer, System.identityHashCode(c).toString)
    }

    def setPage(c:Component) {
      cl.show(peer, System.identityHashCode(c).toString)
    }

    add(page1)
    add(page2)
    add(page3)

  }

  private object page1 extends BorderPanel {

    // Configure the panel
    border = createEmptyBorder(2, 2, 2, 2)

    // Our content, defined below
    add(new BorderPanel {
      add(header, North)
      add(file, Center)
      add(footer, South)
    }, North)

    // Header text
    object header extends Label {
      horizontalAlignment = Alignment.Left
      text = "Select a target file to import:"
    }

    // Footer text
    object footer extends Label {
      horizontalAlignment = Alignment.Left
      text = "See the Help documentation for information on accepted formats."
    }

    // File section is a grid
    object file extends GridBagPanel with Rows {panel =>

      // Our content, defined below
      addRow(new Label("File:"), new BorderPanel {
        add(select, BorderPanel.Position.West)
        add(label, BorderPanel.Position.Center)
      })

      // Select button
      object select extends Button {
        icon = SharedIcons.ICON_ATTACH
        reactions += {
          case ButtonClicked(_) => for {
            file <- new Chooser[TargetImporter]("file", panel.peer).chooseOpen("Target File", "")
          } state = Selected(file)
        }
      }

      // Filename label
      object label extends Label {
        horizontalAlignment = Alignment.Left
      }

    }

  }

  object page2 extends BorderPanel {

    // Our content, defined below
    add(header, BorderPanel.Position.North)
    add(new ScrollPane(listView), BorderPanel.Position.Center)

    // Header text
    object header extends Label {
      horizontalAlignment = Alignment.Left
      border = createEmptyBorder(0,0,2,0)
    }

    // Our list
    object listView extends SimpleListViewer[List[ParseError], List[ParseError], ParseError] {

      val lens = Lens.lensId[List[ParseError]]

      object columns extends Enumeration {
        val Name, Message = Value
      }

      import columns._

      viewer.getTable.getTableHeader.setResizingAllowed(true)

      def columnWidth = {
        case Name    => (200, Int.MaxValue)
        case Message => (350, Int.MaxValue)
      }

      def size(ps:List[ParseError]) = ps.length
      def elementAt(ps:List[ParseError], i:Int) = ps(i)

      val siderealError = new CompositeIcon(SharedIcons.ICON_SIDEREAL, SharedIcons.OVL_ERROR)
      val nonSiderealError = new CompositeIcon(SharedIcons.ICON_NONSIDEREAL, SharedIcons.OVL_ERROR)

      def icon(p:ParseError) = {
        // TODO: show the right kind of icon for the file that we imported
        // at least when there were some successful targets so we can figure out
        // which kind they were
        case _ => siderealError
      }

      def text(p:ParseError) = {
        case Name    => p.targetName.getOrElse("<unknown>")
        case Message => p.msg
      }

    }

  }

  object page3 extends BorderPanel {

    // Our content, defined below
    add(header, BorderPanel.Position.North)
    add(new ScrollPane(listView), BorderPanel.Position.Center)

    // Header text
    object header extends Label {
      horizontalAlignment = Alignment.Left
      border = createEmptyBorder(0,0,2,0)
    }

    // Our list
    object listView extends SimpleListViewer[List[Target], List[Target], Target] {

      val lens = Lens.lensId[List[Target]]

      object columns extends Enumeration {
        val Name = Value
      }

      import columns._

      viewer.getTable.getTableHeader.setResizingAllowed(true)

      def columnWidth = {
        case Name    => (200, Int.MaxValue)
      }

      def size(ps:List[Target]) = ps.length
      def elementAt(ps:List[Target], i:Int) = ps(i)

      def icon(p:Target) = {
        case Name => p match {
          case _: NonSiderealTarget => SharedIcons.ICON_NONSIDEREAL
          case _                    => SharedIcons.ICON_SIDEREAL
        }
      }

      def text(p:Target) = {
        case Name    => p.name
      }

    }

  }

  def unlift[A, B](xs:List[Either[A, B]]):(List[A], List[B]) = (xs.flatMap(_.left.toOption), xs.flatMap(_.right.toOption))

  def read[T <: Target](reader:TargetReader[T], file:File):State = reader.read(file) match {
    case Left(err) =>
      alert("The selected file is not in a recognized format.")
      state
    case Right(rs) => unlift(rs) match {
      case (Nil, ts) => Done(file, Nil, ts)
      case (es, ts)  => Errors(file, es, ts)
    }
  }

  private def alert(s: String) {
    JOptionPane.showMessageDialog(wiz.peer, s, "Open Failed", JOptionPane.ERROR_MESSAGE)
  }

}