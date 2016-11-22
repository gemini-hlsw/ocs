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

    private var glass: Option[GlassLabel] = None

    def onEDT(f: => Unit): HS2[Unit] =
      HS2.delay(Swing.onEDT(f))

    def show(msg: String): HS2[Unit] = onEDT {
      glass = GlassLabel.show(c(), msg)
    }

    val hide: HS2[Unit] = onEDT {
      glass.foreach(_.hide())
    }

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
    } yield Ephemeris(site, e1.data.union(e2.data))
  }

  /** Get the target node and site; we need both of these. */
  private def args(obsN: ISPObservation): Option[(ISPObsComponent, Site)] =
    for {
      tocN  <- obsN.findObsComponentByType(TargetObsComp.SP_TYPE)
      site  <- ObsContext.getSiteFromObservation(obsN).asScalaOpt
    } yield (tocN, site)

  /**
   * Action to compute a program to update the ephimerides for all nonsidereal targets in `obs`,
   * showing status in the given `UI`. So, running this fetches all the ephemerides and constructs
   * a program that updates the model all at once. The idea is that we can perform this final action
   * quickly holding the program lock.
   */
  def refreshEphemerides(obsN: ISPObservation, start: Long, ui: UI): HS2[HS2[Unit]] =
    args(obsN) match {

      case Some((tocN, site)) =>

        val toc  = tocN.getDataObject.asInstanceOf[TargetObsComp]
        val env  = toc.getTargetEnvironment
        val spts = env.getTargets.asScalaList

        val nstArgs: List[(SPTarget, NonSiderealTarget, HorizonsDesignation)] =
          for {
            spt <- spts
            nst <- spt.getNonSiderealTarget.toList
            hd  <- nst.horizonsDesignation.toList
          } yield (spt, nst, hd)

        def update(spt: SPTarget, nst: NonSiderealTarget, hd: HorizonsDesignation, n: Int): HS2[HS2[Unit]] =
          for {
            _ <- ui.show(s"Updating Ephemeris ${n + 1} of ${nstArgs.length} ...")
            e <- lookup(hd, site, start)
          } yield HS2.delay(spt.setTarget(NonSiderealTarget.ephemeris.set(nst, e)))

        val updates: HS2[HS2[Unit]] =
          nstArgs.zipWithIndex
                 .traverseU { case ((spt, nst, hd), n) => update(spt, nst, hd, n) }
                 .map(_.sequenceU.void)

        updates.map(_ *> HS2.delay(tocN.setDataObject(toc)))

      case None => HS2.unit.point[HS2]

    }

  /**
   * Fetch updated ephemerides for all nonsidereal targets in `obs`, showing status in the root pane
   * associated with the given `Component`, returning *another* program that completes the update
   * without any further network access (i.e., quickly) and clears out the UI.
   */
  def refreshEphemerides(obsN: ISPObservation, start: Long, c: Component): IO[IO[Unit]] = {
    val ui = UI(c)
    refreshEphemerides(obsN, start, ui).withResultLogging(Log).run.map {
      case -\/(e) => IO.ioUnit
      case \/-(a) => (a.withResultLogging(Log) ensuring ui.hide).run.void
    }
  }

}