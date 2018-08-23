package edu.gemini.dbTools.timingwindowcheck

import edu.gemini.pot.sp._
import edu.gemini.pot.spdb.{DBAbstractQueryFunctor, IDBDatabaseService}
import edu.gemini.spModel.core.{ProgramId, ProgramType, SPProgramID}
import edu.gemini.spModel.gemini.obscomp.{SPProgram, SPSiteQuality}
import java.security.Principal
import java.time.Instant
import java.util.{Set => JSet}

import scala.collection.mutable.Buffer
import scalaz._
import Scalaz._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow
import edu.gemini.spModel.util.SPTreeUtil

import scala.collection.JavaConverters._

/**
 * An ODB query functor that finds all active science programs.
 */
object TimingWindowFunctor {

  private def isScience(p: ISPProgram): Boolean = {
    val programType: Option[ProgramType] =
      for {
        i <- Option(p.getProgramID).map(pid => ProgramId.parse(pid.stringValue))
        t <- i.ptype
      } yield t

    programType.exists(_.isScience)
  }

  private def isActive(p: ISPProgram): Boolean = {
    val obj = p.getDataObject.asInstanceOf[SPProgram]
    if (obj == null) { false } else obj.isActive && !obj.isCompleted
  }

  def unsafeQuery(db: IDBDatabaseService, user: JSet[Principal]): TimingWindows =
    new TimingWindowFunctor |>
            (f => db.getQueryRunner(user).queryPrograms(f).results.toMap)

  def query(db: IDBDatabaseService, user: JSet[Principal]): Action[TimingWindows] =
    Action.catchLeft(unsafeQuery(db, user))
}

private class TimingWindowFunctor extends DBAbstractQueryFunctor {
  import TimingWindowFunctor.{ isActive, isScience }

  var results: Buffer[(SPProgramID, List[(SPObservationID, Instant)])] = Buffer.empty

  override def execute(db: IDBDatabaseService, node: ISPNode, principals: JSet[Principal]): Unit = {

    def findEnd(o: ISPObservation): Option[(SPObservationID, Instant)] = {
      // TODO: Will the null case of getDataObject make it an empty list?
      val tws: List[TimingWindow] = SPTreeUtil.findObsCondNode(o).getDataObject.asInstanceOf[SPSiteQuality].getTimingWindows.asScala.toList
      tws.flatMap(_.getEnd.asScala).maximumBy(_.toEpochMilli).map(tw => (o.getObservationID, tw))
    }

    node match {
      case p: ISPProgram => if (isActive(p) && isScience(p)) results += ((p.getProgramID, p.getAllObservations.asScala.toList.flatMap(findEnd)))
      case _             => // do nothing
    }
  }
}
