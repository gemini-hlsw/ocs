package jsky.app.ot.gemini.editor

import java.awt.Component
import java.util.logging.Logger

import edu.gemini.pot.sp.{ISPObsComponent, ISPObservation}
import edu.gemini.skycalc.ObservingNight

import java.util.Date
import javax.swing.{JRootPane, SwingUtilities}

import edu.gemini.horizons.server.backend.HorizonsService2._
import edu.gemini.shared.gui.GlassLabel
import edu.gemini.spModel.core._
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.TargetObsComp
import jsky.app.ot.ags.BagsManager
import jsky.util.gui.DialogUtil

import scala.swing.Swing
import scalaz._, Scalaz._
import scalaz.effect.IO

object EphemerisUpdater {
  val Log = Logger.getLogger(getClass.getName)

  /**
   * Module of actions in `HS2` for displaying status messages and doing things on the EDT.
 *
   * @param c any component; `UI` will use its parent `RootPane`
   */
  final class UI private (c: () => Component) {

    private def rootPane: Option[JRootPane] =
      Option(SwingUtilities.getRootPane(c()))

    def onEDT(f: => Unit): HS2[Unit] =
      HS2.delay(Swing.onEDT(f))

    def show(msg: String): HS2[Unit] =
      onEDT(rootPane.foreach(GlassLabel.show(_, msg)))

    val hide: HS2[Unit] =
      onEDT(rootPane.foreach(GlassLabel.hide))

  }
  object UI {
    def apply(c: => Component): UI = new UI(c _)
  }

  /**
   * Action to look up an ephemeris given a designation, site, and point in time. The ephemeris
   * will contain 1000 points over the entire semester plus a month's padding on each side; and
   * an additional 300 points for the `ObservingNight` in which `when` falls.
   */
  def lookup(d: HorizonsDesignation, site: Site, when: Long): HS2[Ephemeris] = {
    val s = new Semester(site, when)
    val n = new ObservingNight(site, when)
    for {
      e1 <- lookupEphemerisWithPadding(d, site, 1000, s)
      e2 <- lookupEphemeris(d, site, new Date(n.getStartTime), new Date(n.getEndTime), 300)
    } yield e1.union(e2)
  }

  /** Get the target node, scheduling block start, and site; we need all of these. */
  private def args(obsN: ISPObservation): Option[(ISPObsComponent, Long, Site)] =
    for {
      tocN  <- obsN.findObsComponentByType(TargetObsComp.SP_TYPE)
      start <- obsN.getDataObject.asInstanceOf[SPObservation].getSchedulingBlockStart.asScalaOpt
      site  <- ObsContext.getSiteFromObservation(obsN).asScalaOpt
    } yield (tocN, start, site)

  /**
   * Action to refresh the ephimerides for all nonsidereal targets in `obs`, showing
   * status in the given `UI`.
   */
  def refreshEphemerides(obsN: ISPObservation, ui: UI): HS2[Unit] =
    args(obsN).traverseU_ { case (tocN, start, site) =>

      val toc  = tocN.getDataObject.asInstanceOf[TargetObsComp]
      val env  = toc.getTargetEnvironment
      val spts = env.getTargets.asScalaList

      val nstArgs: List[(SPTarget, NonSiderealTarget, HorizonsDesignation)] =
        for {
          spt <- spts
          nst <- spt.getNonSiderealTarget.toList
          hd  <- nst.horizonsDesignation.toList
        } yield (spt, nst, hd)

      def update(spt: SPTarget, nst: NonSiderealTarget, hd: HorizonsDesignation, n: Int): HS2[Unit] =
        for {
          _ <- ui.show(s"Updating Ephemeris ${n + 1} of ${nstArgs.length} ...")
          e <- lookup(hd, site, start)
          _ <- HS2.delay(spt.setTarget(NonSiderealTarget.ephemeris.set(nst, e)))
        } yield ()

      nstArgs.zipWithIndex.traverseU { case ((spt, nst, hd), n) => update(spt, nst, hd, n) } *>
      ui.onEDT(tocN.setDataObject(toc)) *>
      ui.hide

    }

  /**
   * Refresh the ephemerides for all nonsidereal targets in `obs`, showing status in the root pane
   * associated with the given `Component`, logging failures.
   */
  def refreshEphemerides(obsN: ISPObservation, c: Component): IO[Unit] = {
    val ui = UI(c)
    (refreshEphemerides(obsN, ui).withResultLogging(Log) ensuring ui.hide).run.void
  }

}