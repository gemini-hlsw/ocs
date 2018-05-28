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
    refreshEphemerides(List(obsN), start, ui)

  def refreshEphemerides(obsNs: List[ISPObservation], start: Long, ui: UI): HS2[HS2[Unit]] = {

    // Get the target node and its data object for each observation.
    val targetArgs: List[(ISPObsComponent, TargetObsComp, Site)] =
      obsNs.flatMap { obsN =>
        (for {
          tocN <- obsN.findObsComponentByType(TargetObsComp.SP_TYPE)
          toc  <- Option(tocN.getDataObject)
          site <- ObsContext.getSiteFromObservation(obsN).asScalaOpt
        } yield (tocN, toc.asInstanceOf[TargetObsComp], site)).toList
      }

    // Get the list of nonsidereal target information for each target node.
    // We separate the lookup key (HorizonsDesignation, Site) from the target
    // information (which is needed to do the actual update).
    val nstArgs: List[((HorizonsDesignation, Site), (SPTarget, NonSiderealTarget))] =
      targetArgs.flatMap { case (tocN, toc, site) =>
        val env  = toc.getTargetEnvironment
        val spts = env.getTargets.asScalaList

        for {
          spt <- spts
          nst <- spt.getNonSiderealTarget.toList
          hd  <- nst.horizonsDesignation.toList
        } yield ((hd, site), (spt, nst))
      }

    // Group all the targets by the (horizons designation, site) lookup key.
    // There's no need to lookup the ephemeris for a given (horizons designation,
    // site) pair more than once.
    val groupedNstArgs: List[((HorizonsDesignation, Site), List[(SPTarget, NonSiderealTarget)])] =
      nstArgs.groupBy(_._1).mapValues(_.map(_._2)).toList

    val total = groupedNstArgs.size

    // An action that will do all the lookups and maintain the user informed.
    val lookupAction: HS2[List[(Ephemeris, List[(SPTarget, NonSiderealTarget)])]] =
      groupedNstArgs.zipWithIndex.traverseU { case (((hd, site), nsts), n) =>
        for {
          _ <- ui.show(s"Looking up ephemeris ${n+1} of $total ...")
          e <- lookup(hd, site, start)
        } yield (e, nsts)
      }

    // An action that does the lookups and then updates the actual SPTargets
    // with the new ephemeris.
    val updateAction: HS2[HS2[Unit]] =
      lookupAction.map { lst =>
        lst.traverseU { case (e, nsts) =>
          nsts.traverseU { case (spt, nst) =>
            HS2.delay(spt.setTarget(NonSiderealTarget.ephemeris.set(nst, e)))
          }.void
        }.void
      }

    // An action that runs the updates and then updates the target node data
    // objects to store the changes in the program itself.
    updateAction.map(_ *> targetArgs.traverseU { case (tocN, toc, _) =>
        HS2.delay(tocN.setDataObject(toc))
    }.void)
  }

  /**
   * Fetch updated ephemerides for all nonsidereal targets in `obs`, showing status in the root pane
   * associated with the given `Component`, returning *another* program that completes the update
   * without any further network access (i.e., quickly) and clears out the UI.
   */
  def refreshEphemerides(obsN: ISPObservation, start: Long, c: Component): IO[IO[Unit]] =
    refreshEphemerides(List(obsN), start, c)

  def refreshEphemerides(obsNs: List[ISPObservation], start: Long, c: Component): IO[IO[Unit]] = {
    val ui = UI(c)
    refreshEphemerides(obsNs, start, ui).withResultLogging(Log).run.map {
      case -\/(e) => IO.ioUnit
      case \/-(a) => (a.withResultLogging(Log) ensuring ui.hide).run.void
    }
  }

}