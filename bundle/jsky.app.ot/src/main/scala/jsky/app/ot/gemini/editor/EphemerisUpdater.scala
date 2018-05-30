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

  /**
   * A structure that contains all the information required to lookup and set
   * the ephemerides associated with a given observation `obsN`.
   */
  private final case class UpdateContext(
    obsN: ISPObservation,
    site: Site,
    tocN: ISPObsComponent,
    toc:  TargetObsComp,
    nsts: List[(HorizonsDesignation, SPTarget, NonSiderealTarget)]
  ) {

    // Updates the nonsidereal targets in the associated observation using the
    // provided map of Ephemeris data.
    def updateAction(m: Map[(HorizonsDesignation, Site), Ephemeris]): HS2[Unit] =
      (nsts.traverseU { case (hd, spt, nst) =>
        m.get((hd, site)).fold(HS2.unit) { e =>
          HS2.delay(spt.setTarget(NonSiderealTarget.ephemeris.set(nst, e)))
        }
      }.void) *> HS2.delay(tocN.setDataObject(toc))

  }

  private object UpdateContext {

    def fromObservation(obsN: ISPObservation): Option[UpdateContext] = {
      // Get the target node and its data object for each observation.
      val args: Option[(Site, ISPObsComponent, TargetObsComp)] =
        for {
          site <- ObsContext.getSiteFromObservation(obsN).asScalaOpt
          tocN <- obsN.findObsComponentByType(TargetObsComp.SP_TYPE)
          toc  <- Option(tocN.getDataObject)
        } yield (site, tocN, toc.asInstanceOf[TargetObsComp])

      // Get the list of nonsidereal target information for each target node.
      // We separate the lookup key (HorizonsDesignation, Site) from the target
      // information (which is needed to do the actual update).
      args.map { case (site, tocN, toc) =>
        val env  = toc.getTargetEnvironment
        val spts = env.getTargets.asScalaList

        val nsts = for {
          spt <- spts
          nst <- spt.getNonSiderealTarget.toList
          hd  <- nst.horizonsDesignation.toList
        } yield (hd, spt, nst)

        UpdateContext(obsN, site, tocN, toc, nsts)
      }
    }

  }

  // Computes an HS2 action that looks up ephemerides for all nonsidereal
  // targets in the list of observations, displaying status in the glass pane UI.
  // Returns a map that can be used after the fact to lookup the `Ephemeris`
  // associated with a horizons key and site.
  private def bulkLookup(
    obsNs: List[ISPObservation],
    start: Long,
    ui:    UI
  ): HS2[Map[(HorizonsDesignation, Site), Ephemeris]] = {

    val keys = obsNs.flatMap(o => UpdateContext.fromObservation(o).toList).flatMap { uc =>
      uc.nsts.map { case (hd, _, _) => (hd, uc.site) }
    }.distinct

    val total = keys.size

    keys.zipWithIndex.traverseU { case ((hd, site), n) =>
      for {
        _ <- ui.show(s"Looking up ephemeris ${n+1} of $total ...")
        e <- lookup(hd, site, start)
      } yield ((hd, site), e)
    }.map(_.toMap)

  }

  /**
   * Fetches updated ephemerides for all nonsidereal targets in the list of
   * observations, showing status in the root pane associated with the given
   * `Component`, returning a function that given a single observation computes
   * *another* action that completes the update without any further network
   * access (i.e., quickly).  The idea is that we can perform the final update
   * actions quickly while holding the program lock.
   */
  def refreshAll(
    obsNs:  List[ISPObservation],
    start:  Long,
    c:      Component
  ): IO[ISPObservation => IO[Unit]] = {

    def setEphemerides(
      obsN: ISPObservation,
      m:    Map[(HorizonsDesignation, Site), Ephemeris]
    ): IO[Unit] =
      UpdateContext.fromObservation(obsN).fold(IO.ioUnit) { uc =>
        uc.updateAction(m).withResultLogging(Log).run.map(_.leftMap(_ => ()).merge)
      }

    val ui = UI(c)
    (bulkLookup(obsNs, start, ui).withResultLogging(Log) ensuring ui.hide).run.map {
      case -\/(_) => (obsN: ISPObservation) => IO.ioUnit
      case \/-(m) => setEphemerides(_, m)
    }

  }

  /**
   * Fetches updated ephemerides for all nonsidereal targets in `obsN`, showing
   * status in the root pane associated with the given `Component`, returning
   * *another* program that completes the update without any further network
   * access (i.e., quickly).  The idea is that we can perform the final action
   * quickly while holding the program lock.
   */
  def refreshOne(obsN: ISPObservation, start: Long, c: Component): IO[IO[Unit]] =
    refreshAll(List(obsN), start, c).map(f => f(obsN))

}