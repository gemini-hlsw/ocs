package jsky.app.ot.gemini.editor.targetComponent.details2

import java.awt.Color
import java.util.logging.Logger
import javax.swing.{ JOptionPane, JLabel}

import edu.gemini.horizons.server.backend.HorizonsService2
import edu.gemini.horizons.server.backend.HorizonsService2.{ HS2, HS2Ops }
import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core._
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import jsky.app.ot.gemini.editor.EphemerisUpdater

import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.{DialogUtil, TextBoxWidget, TextBoxWidgetWatcher}

import scala.swing.Swing
import scalaz._, Scalaz._, effect.IO, concurrent.Task

final class NonSiderealNameEditor extends TelescopePosEditor[SPTarget] with ReentrancyHack {
  import HorizonsService2.{ Search, Row }, Search._

  private[this] var site  = Option.empty[Site]
  private[this] var start = Option.empty[Long]
  private[this] var spt   = new SPTarget // never null

  // A module of actions for displaying status in the GlassPane
  val ui = EphemerisUpdater.UI(name)

  val askSearch: HS2[Option[Search[_ <: HorizonsDesignation]]] =
    HS2.delay {
      case class Wrapper(unwrap: Search[_ <: HorizonsDesignation]) {
        override def toString = unwrap.productPrefix
      }
      Option {
        JOptionPane.showInputDialog(
          name,
          "Select the HORIZONS target type:",
          "Horizons Search",
          JOptionPane.QUESTION_MESSAGE,
          null, // TODO: icon
          List(Comet, Asteroid, MajorBody).map(f => Wrapper(f(name.getText))).toArray[Object],
          null)
      } .map(_.asInstanceOf[Wrapper].unwrap)
    }

  def lookup(d: HorizonsDesignation, site: Site): HS2[Ephemeris] =
    start.fold(Ephemeris.empty.point[HS2]) { when =>
      ui.show(s"Fetching Ephemeris ...") *> EphemerisUpdater.lookup(d, site, when)
    }

  // Given designation and name from horizons, along with the current target name, provide a new
  // target name. We try to find a match, and if there is none we just return the old name.
  def modifyName(hd: HorizonsDesignation, hn: String)(n: String): String =
    List(hn, hd.des).find(s => s.toLowerCase.indexOf(n.toLowerCase) >= 0).getOrElse(n)

  def updateDesignation(hd: HorizonsDesignation, name: String, rename: Boolean): HS2[Unit] =
    ui.onEDT {
      val t0 = if (rename) Target.name.mod(modifyName(hd, name), spt.getTarget) else spt.getTarget
      Target.horizonsDesignation.set(t0, Some(hd)).foreach(spt.setTarget)
    }

  def updateEphem(e: Ephemeris): HS2[Unit] =
    ui.onEDT(Target.ephemeris.set(spt.getTarget, e).foreach(spt.setTarget))

  def manyResults(rs: List[Row[_ <: HorizonsDesignation]]): HS2[Unit] =
    HS2.delay {
      case class Wrapper(unwrap: Row[_ <: HorizonsDesignation]) {
        override def toString = unwrap.name + " - " + unwrap.a.des
      }
      Option {
        JOptionPane.showInputDialog(
          name,
          "Multiple results were found. Please disambiguate:",
          "Horizons Search",
          JOptionPane.QUESTION_MESSAGE,
          null, // TODO: icon
          rs.map(Wrapper).sortBy(_.toString).toArray[Object],
          null)
      } .map(_.asInstanceOf[Wrapper].unwrap)
    } >>= {
      case Some(r) => oneResult(r, true)
      case None    => ().point[HS2]
    }

  def oneResult(r: Row[_ <: HorizonsDesignation], rename: Boolean): HS2[Unit] =
    updateDesignation(r.a, r.name, rename).as(site) >>= {
      case Some(site) => lookup(r.a, site) <* ui.hide >>= updateEphem
      case None       => HS2.delay(Swing.onEDT(DialogUtil.error(name, "Cannot determine site for this observation; this is needed for ephemeris lookup.")))
    }

  val noResults: HS2[Unit] =
    HS2.delay(DialogUtil.message(name, "No results found."))

  val search: HS2[Unit] =
    ui.show("Searching...") *>
    askSearch >>= {
      case None => ().point[HS2] // user hit cancel
      case Some(s) =>
        (HorizonsService2.search(s) <* ui.hide) >>= {
          case Nil     => noResults
          case List(r) => oneResult(r, true)
          case rs      => manyResults(rs)
        }
    }

  // run this action on another thread, ensuring that the glasspane is always hidden, and log any failures
  def unsafeRun[A](hs2: HS2[A]): Unit = {
    val action = hs2.withResultLogging(NonSiderealNameEditor.Log) ensuring ui.hide
    Task(action.run.unsafePerformIO).unsafePerformAsync {
      case -\/(t) => Swing.onEDT(DialogUtil.error(name, t))
      case \/-(_) => () // done!
    }
  }

  def lookup(site: Option[Site]): Unit =
    unsafeRun(search)

  def horizonsDesignation: Option[HorizonsDesignation] =
    spt.getNonSiderealTarget.flatMap(Target.horizonsDesignation.get).flatten

  def refreshEphemeris(): Unit =
    horizonsDesignation.map(hd => oneResult(Row(hd, spt.getName), false)).foreach(unsafeRun)

  val name = new TextBoxWidget <| { w =>
    w.setMinimumSize(w.getPreferredSize)
    w.addWatcher(new TextBoxWidgetWatcher {

      override def textBoxKeyPress(tbwe: TextBoxWidget): Unit =
        nonreentrant {
          spt.setTarget(Target.name.set(spt.getTarget, tbwe.getValue))
        }

      override def textBoxAction(tbwe: TextBoxWidget): Unit =
        lookup(site)

    })
  }

  object buttons {
    val search  = searchButton(lookup(site))
    val refresh = refreshButton(refreshEphemeris())
  }

  val hid = new JLabel <| { a =>
    a.setForeground(Color.DARK_GRAY)
  }

  def hidText(hd: Option[HorizonsDesignation]): String =
    hd.fold("--")(_.queryString)

  def edit(ctx: GOption[ObsContext], target: SPTarget, node: ISPNode): Unit = {
    this.spt   = target
    this.start = ctx.asScalaOpt.flatMap(_.getSchedulingBlockStart.asScalaOpt.map(_.toLong))
    this.site  = ctx.asScalaOpt.flatMap(_.getSite.asScalaOpt)
    nonreentrant {
      name.setText(Target.name.get(target.getTarget))
      buttons.refresh.setEnabled(horizonsDesignation.isDefined && site.isDefined)
      Target.horizonsDesignation.get(target.getTarget).map(hidText).foreach(hid.setText)
    }
  }


}

object NonSiderealNameEditor {
  val Log = Logger.getLogger(classOf[NonSiderealNameEditor].getName)
}