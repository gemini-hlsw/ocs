package edu.gemini.pit.ui.editor

import edu.gemini.model.p1.immutable.{ Target, SiderealTarget }
import edu.gemini.pit.catalog.{ Result => SimbadResult, Simbad }
import edu.gemini.pit.catalog.{ Success, NotFound, Error, Offline }
import edu.gemini.shared.gui.GlassLabel
import java.awt.Component
import java.util.logging.Logger
import javax.swing.JOptionPane
import jsky.util.gui.DialogUtil
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing.Swing
import scalaz._, Scalaz._, scalaz.concurrent.Task

/** Synchronous wrapper for the UI aspects of a Simbad lookup. */
final class SimbadLookup(editor: TargetEditor) {
  private[this] val Log = Logger.getLogger(getClass.getName)
  private[this] var glass = Option.empty[GlassLabel]

  /** Add an `ensuring` method to Task so it works like IO. */
  private implicit class MoreTaskOps[A](ta: Task[A]) {
    def ensuring(always: Task[Unit]): Task[A] =
      ta.attempt.flatMap {
        case -\/(t) => always *> Task.fail(t)
        case \/-(r) => always.as(r)
      }
  }

  /** Perform a search with the given query, prompting for disambiguation if
    * needed, replacing the TargetEditor on success.
    * @param query the horizons query
    */
  def lookup(query: String): Unit = {
    val action = search(query).ensuring(hide)
    action.unsafePerformAsync {
      case -\/(t)  => Swing.onEDT(DialogUtil.error(editor.peer, t))
      case \/-(()) => ()
    }
  }

  private def search(query: String): Task[Unit] =
    show("Searching...") *> Task.async[SimbadResult] { k =>
      Simbad.find(query).onComplete {
        case scala.util.Success(result) => k(result.right)
        case scala.util.Failure(ex)     => k(ex.left)
      }
    } >>= {

      // Failure cases
      case Offline     => fail("SIMBAD is offline or unreachable.")
      case NotFound(d) => fail("No results found.")
      case Error(t)    => Task.fail(t) // roll into top-level handler

      // Success case will always be Nil on the right for a VOTableCatalog.
      // This is a bad design but ¯\_(ツ)_/¯
      case Success(t :: Nil, _) => handleSingleResult(t)
      case Success(ts,       _) => handleManyResults(ts)

      // Should never happen, heh-heh
      case r => Task.delay(Log.severe(s"SIMBAD: unexpected result: $r"))
    }

  /** Fail with an error, notifying the user. */
  private def fail(msg: String): Task[Unit] =
    onEDT(DialogUtil.message(editor.peer, msg))

  /** Pop up a dialog for disambiguating many results. */
  private def handleManyResults(ts: List[Target]): Task[Unit] =
    Task.delay {
      case class Wrapper(unwrap: Target) {
        override def toString = unwrap.name
      }
      Option {
        JOptionPane.showInputDialog(
          editor.peer,
          "Multiple results were found. Please disambiguate:",
          "SIMBAD Search",
          JOptionPane.QUESTION_MESSAGE,
          null, // TODO: icon
          ts.map(Wrapper).sortBy(_.toString).toArray[Object],
          null)
      }.map(_.asInstanceOf[Wrapper].unwrap)
    } flatMap {
      case Some(r) => handleSingleResult(r)
      case None    => ().point[Task]
    }

  /** One result! Replace the TargetEditor. */
  private def handleSingleResult(t: Target): Task[Unit] =
    onEDT(editor.close(TargetEditor.Replace(t)))

  /** Run a thunk on the EDT. */
  private def onEDT(f: => Unit): Task[Unit] =
    Task.delay(Swing.onEDT(f))

  /** Show the glass panel (on the EDT). */
  private def show(msg: String): Task[Unit] =
    onEDT(glass = GlassLabel.show(editor.peer, msg))

  /** Hide the glass panel (on the EDT). */
  private val hide: Task[Unit] =
    onEDT(glass.foreach(_.hide()))

}
