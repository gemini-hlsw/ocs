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

/**
 * Edits a non-sidereal target name, resolving it to a unique HorizonsDesignation
 * using NonSiderealNameResolver and then looking up the associated ephemeris.
 */
final class NonSiderealNameEditor extends TelescopePosEditor[SPTarget] with ReentrancyHack {
  import HorizonsService2.{ Search, Row }, Search._

  private[this] var site  = Option.empty[Site]
  private[this] var start = Option.empty[Long]
  private[this] var spt   = new SPTarget // never null

  def lookup(d: HorizonsDesignation, site: Site): HS2[Ephemeris] =
    start.fold(Ephemeris.empty.point[HS2]) { when =>
      resolver.ui.show(s"Fetching Ephemeris ...") *> EphemerisUpdater.lookup(d, site, when)
    }

  def updateEphem(e: Ephemeris): HS2[Unit] =
    resolver.ui.onEDT(Target.ephemeris.set(spt.getTarget, e).foreach(spt.setTarget))

  def handleResult(r: Row[_ <: HorizonsDesignation], rename: Boolean): HS2[Unit] =
    handleResult(r.a, r.name, rename)

  def handleResult(d: HorizonsDesignation, n: String, rename: Boolean): HS2[Unit] =
    resolver.updateDesignationAction(d, n, rename, spt).as(site) >>= {
      case Some(site) => lookup(d, site) <* resolver.ui.hide >>= updateEphem
      case None       => HS2.delay(Swing.onEDT(DialogUtil.error(name, "Cannot determine site for this observation; this is needed for ephemeris lookup.")))
    }

  def lookup(): Unit =
    resolver.lookup()

  def horizonsDesignation: Option[HorizonsDesignation] =
    spt.getNonSiderealTarget.flatMap(Target.horizonsDesignation.get).flatten

  def refreshEphemeris(): Unit =
    horizonsDesignation.map(hd => handleResult(Row(hd, spt.getName), false)).foreach(resolver.unsafeRun)

  val name = new TextBoxWidget <| { w =>
    w.setMinimumSize(w.getPreferredSize)
    w.addWatcher(new TextBoxWidgetWatcher {

      override def textBoxKeyPress(tbwe: TextBoxWidget): Unit =
        nonreentrant {
          spt.setTarget(Target.name.set(spt.getTarget, tbwe.getValue))
        }

      override def textBoxAction(tbwe: TextBoxWidget): Unit =
        lookup()

    })
  }

  val resolver: NonSiderealNameResolver =
    new NonSiderealNameResolver(name, handleResult(_, _, true))

  object buttons {
    val search  = searchButton(lookup())
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