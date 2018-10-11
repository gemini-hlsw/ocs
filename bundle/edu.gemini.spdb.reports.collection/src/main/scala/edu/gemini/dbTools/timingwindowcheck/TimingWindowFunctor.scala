package edu.gemini.dbTools.timingwindowcheck

import edu.gemini.pot.sp._
import edu.gemini.pot.spdb.{DBAbstractQueryFunctor, IDBDatabaseService}
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.shared.util.immutable.ScalaConverters._
import java.security.Principal
import java.time.Instant
import java.util.{Set => JSet}

import scalaz._
import Scalaz._
import edu.gemini.spModel.util.SPTreeUtil

import scala.collection.JavaConverters._

/**
 * An ODB query functor that finds all active science programs.
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

  }

  def unsafeQuery(db: IDBDatabaseService, user: JSet[Principal]): List[(SPProgramID, List[(SPObservationID, Instant)])] =
    new TimingWindowFunctor |>
            (f => db.getQueryRunner(user).queryPrograms(f).results)

  def query(db: IDBDatabaseService, user: JSet[Principal]): Action[List[(SPProgramID, List[(SPObservationID, Instant)])]] =
    Action.catchLeft(unsafeQuery(db, user))
}

private class TimingWindowFunctor extends DBAbstractQueryFunctor {
  import TimingWindowFunctor._

  /**
   * All observations in active programs with an expiring timing window,
   * regardless of when the timing window expires.
   */
  var results: List[(SPProgramID, List[(SPObservationID, Instant)])] = Nil

  override def execute(db: IDBDatabaseService, node: ISPNode, principals: JSet[Principal]): Unit = {

    def expiringObs(p: ISPProgram): List[(SPObservationID, Instant)] =
      p.allObservations.flatMap { o =>
        o.timingWindowExpiration.strengthL(o.getObservationID).toList
      }

    node match {
      case p: ISPProgram if p.isOngoing =>
        p.pidOption.strengthR(expiringObs(p)) match {
          case Some((pid, o :: os))  =>
            results = (pid, o :: os) :: results

          case _                     =>
            // no pid or no observations with an expiring window so do nothing
        }

      case _ =>
        // do nothing
    }
    
  }
}
