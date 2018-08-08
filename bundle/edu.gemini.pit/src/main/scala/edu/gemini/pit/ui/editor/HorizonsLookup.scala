package edu.gemini.pit.ui.editor

import edu.gemini.horizons.server.backend.HorizonsService2
import edu.gemini.horizons.server.backend.HorizonsService2.{HS2, HS2Ops, Search, Row}
import edu.gemini.horizons.server.backend.HorizonsService2.Search._
import edu.gemini.shared.gui.GlassLabel
import edu.gemini.skycalc.ObservingNight
import edu.gemini.spModel.core._
import java.awt.Component
import java.util.Date
import java.util.logging.Logger
import java.util.UUID
import javax.swing.JOptionPane
import jsky.util.gui.DialogUtil
import scala.swing.Swing
import scalaz._, Scalaz._
import scalaz.concurrent.Task
import edu.gemini.model.p1.mutable.CoordinatesEpoch
import edu.gemini.model.p1.immutable.{ EphemerisElement, NonSiderealTarget }

/** Simplified version of the OT's Horizons search + ephemeris lookup. We pass
  * in the site and time and only allow the query string itself to vary.
  */
final class HorizonsLookup(editor: TargetEditor, site: Site, when: Long) {
  private[this] val Log = Logger.getLogger(getClass.getName)
  private[this] var glass = Option.empty[GlassLabel]

  /** Perform a search with the given query, prompting for object type and
    * disambiguation if needed, then look up the ephemeris for the semester
    * containing `when`, replacing the TargetEditor on success.
    * @param query the horizons query
    */
  def lookup(query: String): Unit = {
    val action = search(query).withResultLogging(Log).ensuring(hide)
    Task(action.run.unsafePerformIO).unsafePerformAsync {
      case -\/(t) => Swing.onEDT(DialogUtil.error(editor.peer, t))
      case \/-(_) => () // done!
    }
  }

  /** Attempt a search followed up an ephemeris lookup. */
  private def search(query: String): HS2[Unit] =
    show("Searching...") *> createSearch(query).flatMap {
      case None    => ().point[HS2] // user hit cancel
      case Some(s) =>
        (HorizonsService2.search(s) <* hide).flatMap {
          case Nil     => handleNoResults
          case List(r) => handleSingleResult(r)
          case rs      => handleManyResults(rs)
        }
    }

  /** Create a Search from a query string by prompting for object type. */
  private def createSearch(query: String): HS2[Option[Search[_ <: HorizonsDesignation]]] =
    HS2.delay {
      case class Wrapper(unwrap: Search[_ <: HorizonsDesignation]) {
        override def toString = unwrap.productPrefix
      }
      Option {
        JOptionPane.showInputDialog(
          editor.peer,
          "Select the HORIZONS target type:",
          "Horizons Search",
          JOptionPane.QUESTION_MESSAGE,
          null, // TODO: icon
          List(Comet, Asteroid, MajorBody)
            .map(f => Wrapper(f(query)))
            .toArray[Object],
          null)
      }.map(_.asInstanceOf[Wrapper].unwrap)
    }

  /** Pop up a dialog for disambiguating many results. */
  private def handleManyResults(rs: List[Row[_ <: HorizonsDesignation]]): HS2[Unit] =
    HS2.delay {
      case class Wrapper(unwrap: Row[_ <: HorizonsDesignation]) {
        override def toString = unwrap.name + " - " + unwrap.a.des
      }
      Option {
        JOptionPane.showInputDialog(
          editor.peer,
          "Multiple results were found. Please disambiguate:",
          "Horizons Search",
          JOptionPane.QUESTION_MESSAGE,
          null, // TODO: icon
          rs.map(Wrapper).sortBy(_.toString).toArray[Object],
          null)
      }.map(_.asInstanceOf[Wrapper].unwrap)
    } flatMap {
      case Some(r) => handleSingleResult(r)
      case None    => ().point[HS2]
    }

  /** One result! Replace the TargetEditor. */
  private def handleSingleResult(r: Row[_ <: HorizonsDesignation]): HS2[Unit] =
    for {
      e <- fetchEphemeris(r.a)
      t <- p1target(r.name, e)
      _ <- onEDT(editor.close(TargetEditor.Replace(t)))
    } yield ()

  /** No result! Inform the user. */
  private val handleNoResults: HS2[Unit] =
    HS2.delay(DialogUtil.message(editor.peer, "No results found."))

  /** Fetch the ephemeris for the given designation. */
  private def fetchEphemeris(d: HorizonsDesignation): HS2[Ephemeris] =
    show(s"Fetching Ephemeris ...") *> {
      val s  = new Semester(site, when)
      val n  = new ObservingNight(site, when)
      val d1 = new Date(n.getStartTime)
      val d2 = new Date(n.getEndTime)
      for {
        e1 <- HorizonsService2.lookupEphemerisWithPadding(d, site, 200, s)
      } yield Ephemeris(site, e1.data)
    }

  /** Create a new phase 1 target from a name and phase-2 (!) ephemeris. */
  def p1target(name: String, ephemeris: Ephemeris): HS2[NonSiderealTarget] =
    HS2.delay {
      NonSiderealTarget(
        UUID.randomUUID, // side-effect, so need delay above
        name,
        ephemeris.data.toAscList.map {
          case (t, cd) => EphemerisElement(cd, None, t) // no magnitude info
        },
        CoordinatesEpoch.J_2000
      )
    }

  /** Run a thunk on the EDT. */
  private def onEDT(f: => Unit): HS2[Unit] =
    HS2.delay(Swing.onEDT(f))

  /** Show the glass panel (on the EDT). */
  private def show(msg: String): HS2[Unit] =
    onEDT(glass = GlassLabel.show(editor.peer, msg))

  /** Hide the glass panel (on the EDT). */
  private val hide: HS2[Unit] =
    onEDT(glass.foreach(_.hide()))

}