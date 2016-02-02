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
import edu.gemini.spModel.core.{Site, Ephemeris, HorizonsDesignation, Target}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.ConicTarget

import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.{DialogUtil, TextBoxWidget, TextBoxWidgetWatcher}


import Horizons.HorizonsIO

import scala.swing.Swing
import scalaz._, Scalaz._, effect.IO

final class NonSiderealNameEditor extends TelescopePosEditor with ReentrancyHack {

  private[this] var site = Option.empty[Site]
  private[this] var spt = new SPTarget // never null

  def lookup(site: Option[Site]): Unit = {
    import HorizonsService2.{ Search, Row }, Search._

    case class Wrapper(unwrap: Search[_ <: HorizonsDesignation]) {
      override def toString = unwrap.productPrefix
    }

    Option(
      JOptionPane.showInputDialog(
        name,
        "Select the HORIZONS target type:",
        "Horizons Search",
        JOptionPane.QUESTION_MESSAGE,
        null, // TODO: icon
        List(Comet, Asteroid, MajorBody).map(f => Wrapper(f(name.getText))).toArray[Object],
        null)
    ).map(_.asInstanceOf[Wrapper].unwrap).foreach { s =>

        def show(msg: String): HS2[Unit] =
          HS2.delay(Swing.onEDT(GlassLabel.show(SwingUtilities.getRootPane(name), msg)))

        def hide: HS2[Unit] =
          HS2.delay(Swing.onEDT(GlassLabel.hide(SwingUtilities.getRootPane(name))))

        def lookup(d: HorizonsDesignation, site: Site): HS2[Ephemeris] =
          show("Fetching Ephemeris...") *> HorizonsService2.lookupEphemeris(d, site, 1000) // arbitrary, ok?

        def updateDesignation(hd: HorizonsDesignation, name: String): HS2[Unit] =
          HS2.delay {
            val t0 = Target.name.set(spt.getNewTarget, name)
            Target.horizonsDesignation.set(t0, Some(hd)).foreach(spt.setNewTarget)
          }

        def updateEphem(e: Ephemeris): HS2[Unit] =
          HS2.delay(Target.ephemeris.set(spt.getNewTarget, e).foreach(spt.setNewTarget))

        def manyResults(rs: List[Row[_ <: HorizonsDesignation]]): HS2[Unit] =
          HS2.delay {
            Option(JOptionPane.showInputDialog(
              name,
              "Multiple results were found. Please disambiguate:",
              "Horizons Search",
              JOptionPane.QUESTION_MESSAGE,
              null, // TODO: icon
              rs.toArray[Object],
              null)).map(_.asInstanceOf[Row[_ <: HorizonsDesignation]])
          } >>= {
            case Some(r) => oneResult(r)
            case None    => ().point[HS2]
          }

        def oneResult(r: Row[_ <: HorizonsDesignation]): HS2[Unit] =
          updateDesignation(r.a, r.name).as(site) >>= {
            case Some(site) => lookup(r.a, site) <* hide >>= updateEphem
            case None       => HS2.delay(DialogUtil.error(name, "Cannot determine site for this observation; this is needed for ephemeris lookup."))
          }

        val noResults: HS2[Unit] =
          HS2.delay(DialogUtil.message(name, "No results found."))

        val search =
          (HorizonsService2.search(s) <* hide) >>= {
            case Nil     => noResults
            case List(r) => oneResult(r)
            case rs      => manyResults(rs)
          }

        forkSwingWorker((show("Searching...") *> search).run.ensuring(hide.run).unsafePerformIO) {
          case -\/(t) => DialogUtil.error(name, t)
          case \/-(_) => () // done!
        }

      }

    }


  val name = new TextBoxWidget <| { w =>
    w.setMinimumSize(w.getPreferredSize)
    w.addWatcher(new TextBoxWidgetWatcher {

      override def textBoxKeyPress(tbwe: TextBoxWidget): Unit =
        nonreentrant {
          spt.setNewTarget(Target.name.set(spt.getNewTarget, tbwe.getValue))
        }

      override def textBoxAction(tbwe: TextBoxWidget): Unit =
        lookup(site)

    })
  }

  val search = searchButton(lookup(site))

  val hid = new JLabel <| { a =>
    a.setForeground(Color.DARK_GRAY)
  }

  def hidText(hd: Option[HorizonsDesignation]): String =
    "Horizons: " + hd.fold("«unknown»")(_.queryString)

  def edit(ctx: GOption[ObsContext], target: SPTarget, node: ISPNode): Unit = {
    this.spt = target
    this.site = ctx.asScalaOpt.flatMap(_.getSite.asScalaOpt)
    nonreentrant {
      name.setText(Target.name.get(target.getNewTarget))
      Target.horizonsDesignation.get(target.getNewTarget).map(hidText).foreach(hid.setText)
    }
  }


}
