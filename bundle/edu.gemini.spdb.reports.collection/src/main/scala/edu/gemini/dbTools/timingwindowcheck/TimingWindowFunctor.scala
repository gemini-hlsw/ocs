package edu.gemini.dbTools.timingwindowcheck

import edu.gemini.pot.sp._
import edu.gemini.pot.spdb.{DBAbstractQueryFunctor, IDBDatabaseService}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.skycalc.Interval
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.util.SPTreeUtil

import java.security.Principal
import java.time.Instant
import java.util.{Set => JSet}

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

import scalaz.effect.IO


/**
 * An ODB query functor that finds all active observations whose last timing
 * window expired during a given time `Interval`.
 */
object TimingWindowFunctor {

  private implicit class ObservationOps(o: ISPObservation) {

    /** Gets the site quality component (data obj) of this observation if any. */
    def siteQuality: Option[SPSiteQuality] =
      Option(SPTreeUtil.findObsCondNode(o))
        .flatMap { n => Option(n.getDataObject) }
        .collect { case s: SPSiteQuality => s }

    /** Gets the time that the last timing window expires, if any. */
    def timingWindowExpiration: Option[Instant] =
      o.siteQuality.flatMap { s =>
        s.getTimingWindows.asScala.toList
         .flatMap(_.getEnd.asScalaOpt.toList)
         .maximumBy(_.toEpochMilli)
      }

    def isActive: Boolean = {
      import ObservationStatus.{ON_HOLD, ONGOING, READY}

      ObservationStatus.computeFor(o) match {
        case ON_HOLD | ONGOING | READY => true
        case _                         => false
      }
    }
  }

  def unsafeQuery(interval: Interval, db: IDBDatabaseService, user: JSet[Principal]): Vector[SPObservationID] =
    new TimingWindowFunctor(interval) |>
            (f => db.getQueryRunner(user).queryPrograms(f).results)

  /**
   * Obtains all active observations whose last timing window (if any) expired
   * during the given `interval`.
   */
  def query(interval: Interval, db: IDBDatabaseService, user: JSet[Principal]): IO[Vector[SPObservationID]] =
    IO(unsafeQuery(interval, db, user))
}

private class TimingWindowFunctor(interval: Interval) extends DBAbstractQueryFunctor {
  import TimingWindowFunctor._

  /**
   * All active observations in active programs with a timing window that
   * expired or expires in the given Interval.
   */
  var results: Vector[SPObservationID] = Vector.empty

  override def execute(db: IDBDatabaseService, node: ISPNode, principals: JSet[Principal]): Unit =
    node match {
      case p: ISPProgram if p.isScience && p.isOngoing =>
        results = results ++
          p.allObservations
            .filter(o => o.timingWindowExpiration.exists(interval.contains) && o.isActive)
            .map(_.getObservationID)
      case _                                           =>
        // Not an ongoing program so do nothing
    }

}
