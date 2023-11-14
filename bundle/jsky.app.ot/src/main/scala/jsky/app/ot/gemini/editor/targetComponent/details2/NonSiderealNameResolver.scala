package jsky.app.ot.gemini.editor.targetComponent.details2


import java.util.logging.Logger

import edu.gemini.horizons.server.backend.HorizonsService2
import edu.gemini.horizons.server.backend.HorizonsService2.{Row, HS2, HS2Ops, Search}
import edu.gemini.horizons.server.backend.HorizonsService2.Search._
import edu.gemini.spModel.core.{Target, HorizonsDesignation, NonSiderealTarget}
import edu.gemini.spModel.target.SPTarget
import jsky.app.ot.gemini.editor.EphemerisUpdater

import javax.swing.text.JTextComponent

import jsky.util.gui.DialogUtil

import scala.swing.Swing
import scalaz._
import Scalaz._
import scalaz.concurrent.Task

/**
 * An editor that resolves a non-sidereal target name into a unique
 * HorizonsDesignation.  See NonSiderealNameEditor for a client of this code
 * which uses the HorizonsDesignation to find and set the associated ephemeris.
 *
 * @param name text component where name is typed
 * @param action action to be performed once the name is resolved
 */
final class NonSiderealNameResolver(
  name:   JTextComponent,
  action: (HorizonsDesignation, String) => HS2[Unit]
) {

  // A module of actions for displaying status in the GlassPane
  val ui = EphemerisUpdater.UI(name)

  val askSearch: HS2[Option[Search[_ <: HorizonsDesignation]]] =
    HS2.delay {
      case class Wrapper(unwrap: Search[_ <: HorizonsDesignation]) {
        override def toString = unwrap.productPrefix
      }
      ui.ask(
        name,
        "Select the HORIZONS target type:",
        List(Comet, Asteroid, MajorBody).map(f => Wrapper(f(name.getText))).toArray[Object]
      ).map(_.asInstanceOf[Wrapper].unwrap)
    }

  val search: HS2[Unit] =
    ui.show("Searching...") *>
    askSearch >>= {
      case None    => ().point[HS2] // user hit cancel
      case Some(s) =>
        (HorizonsService2.search(s) <* ui.hide) >>= {
          case Nil     => noResults
          case List(r) => action(r.a, r.name)
          case rs      => manyResults(rs)
        }
    }

  val noResults: HS2[Unit] =
    ui.onEDT {
      DialogUtil.message(name, "No results found.")
    }

  def manyResults(rs: List[Row[_ <: HorizonsDesignation]]): HS2[Unit] =
    HS2.delay {
      case class Wrapper(unwrap: Row[_ <: HorizonsDesignation]) {
        override def toString = unwrap.name + " - " + unwrap.a.des
      }
      ui.ask(
        name,
        "Multiple results were found. Please disambiguate:",
        rs.map(Wrapper).sortBy(_.toString).toArray[Object]
      ).map(_.asInstanceOf[Wrapper].unwrap)
    } >>= {
      case Some(r) => action(r.a, r.name)
      case None    => ().point[HS2]
    }


  def updateDesignationAction(hd: HorizonsDesignation, name: String, rename: Boolean, spt: SPTarget): HS2[Unit] =
    ui.onEDT(updateDesignation(hd, name, rename, spt))

  def updateDesignation(hd: HorizonsDesignation, name: String, rename: Boolean, spt: SPTarget): Unit = {
    val t0 = if (rename) Target.name.mod(modifyName(hd, name), spt.getTarget) else spt.getTarget
    Target.horizonsDesignation.set(t0, Some(hd)).foreach(spt.setTarget)
  }

  // Given designation and name from horizons, along with the current target name, provide a new
  // target name. We try to find a match, and if there is none we just return the old name.
  def modifyName(hd: HorizonsDesignation, hn: String)(n: String): String =
    List(hn, hd.des).find(s => s.toLowerCase.indexOf(n.toLowerCase) >= 0).getOrElse(n)

  // run this action on another thread, ensuring that the glasspane is always hidden, and log any failures
  def unsafeRun[A](hs2: HS2[A]): Unit = {
    val action = hs2.withResultLogging(NonSiderealNameResolver.Log) ensuring ui.hide
    Task(action.run.unsafePerformIO).unsafePerformAsync {
      case -\/(t) => Swing.onEDT(DialogUtil.error(name, t))
      case \/-(_) => () // done!
    }
  }

  def lookup(): Unit =
    unsafeRun(search)

}

object NonSiderealNameResolver {
  val Log = Logger.getLogger(classOf[NonSiderealNameResolver].getName)
}
