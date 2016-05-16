package jsky.app.ot.gemini.editor.targetComponent.details2

import java.awt.Color
import java.util.Date
import javax.swing.{SwingUtilities, JOptionPane, JLabel}

import edu.gemini.horizons.server.backend.HorizonsService2
import edu.gemini.horizons.server.backend.HorizonsService2.HS2
import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.gui.GlassLabel
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.skycalc.ObservingNight
import edu.gemini.spModel.core._
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget

import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.{DialogUtil, TextBoxWidget, TextBoxWidgetWatcher}

import scala.swing.Swing
import scalaz._, Scalaz._, effect.IO, concurrent.Task

final class NonSiderealNameEditor extends TelescopePosEditor with ReentrancyHack {
  import HorizonsService2.{ Search, Row }, Search._

  private[this] var site  = Option.empty[Site]
  private[this] var start = Option.empty[Long]
  private[this] var spt   = new SPTarget // never null

  /// Some IO actions for looking up targets and epherimides

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

  def show(msg: String): HS2[Unit] =
    HS2.delay(Swing.onEDT(GlassLabel.show(SwingUtilities.getRootPane(name), msg)))

  def hide: HS2[Unit] =
    HS2.delay(Swing.onEDT(GlassLabel.hide(SwingUtilities.getRootPane(name))))

  // semester for scheduling block if any, or current time
  def semester(site: Site): Semester =
    new Semester(site, start.getOrElse(System.currentTimeMillis))

  def lookup(d: HorizonsDesignation, site: Site): HS2[Ephemeris] = {
    val s = semester(site)
    val n = start.map(new ObservingNight(site, _))
    for {
      _  <- show(s"Fetching Ephemeris for $s ...")
      e1 <- HorizonsService2.lookupEphemerisWithPadding(d, site, 1000, s)
      e2 <- n.fold(Ephemeris.empty.point[HS2]) { n =>
              show(s"Fetching Ephemeris for ${n.getNightString} ...") *>
              HorizonsService2.lookupEphemeris(d, site, new Date(n.getStartTime), new Date(n.getEndTime), 300)
            }
    } yield e1.union(e2)
  }

  // Given designation and name from horizons, along with the current target name, provide a new
  // target name. We try to find a match, and if there is none we just return the old name.
  def modifyName(hd: HorizonsDesignation, hn: String)(n: String): String =
    List(hn, hd.des).find(s => s.toLowerCase.indexOf(n.toLowerCase) >= 0).getOrElse(n)

  def updateDesignation(hd: HorizonsDesignation, name: String, rename: Boolean): HS2[Unit] =
    HS2.delay {
      Swing.onEDT {
        val t0 = if (rename) Target.name.mod(modifyName(hd, name), spt.getTarget) else spt.getTarget
        Target.horizonsDesignation.set(t0, Some(hd)).foreach(spt.setTarget)
      }
    }

  def updateEphem(e: Ephemeris): HS2[Unit] =
    HS2.delay {
      Swing.onEDT {
        Target.ephemeris.set(spt.getTarget, e).foreach(spt.setTarget)
      }
    }

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
      case Some(site) => lookup(r.a, site) <* hide >>= updateEphem
      case None       => HS2.delay(Swing.onEDT(DialogUtil.error(name, "Cannot determine site for this observation; this is needed for ephemeris lookup.")))
    }

  val noResults: HS2[Unit] =
    HS2.delay(DialogUtil.message(name, "No results found."))

  val search = show("Searching...") *> askSearch >>= {
    case None => ().point[HS2] // user hit cancel
    case Some(s) =>
      (HorizonsService2.search(s) <* hide) >>= {
        case Nil     => noResults
        case List(r) => oneResult(r, true)
        case rs      => manyResults(rs)
      }
  }

  def unsafeRun[A](io: IO[A]): Unit =
    Task(io.unsafePerformIO).unsafePerformAsync {
      case -\/(t) => Swing.onEDT(DialogUtil.error(name, t))
      case \/-(_) => () // done!
    }

  def lookup(site: Option[Site]): Unit =
    unsafeRun(search.run.ensuring(hide.run))

  def horizonsDesignation: Option[HorizonsDesignation] =
    spt.getNonSiderealTarget.flatMap(Target.horizonsDesignation.get).flatten

  def refreshEphemeris(): Unit =
    horizonsDesignation.map(hd => oneResult(Row(hd, spt.getName), false).run).foreach(unsafeRun)

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
