package edu.gemini.dataman.app

import edu.gemini.dataman.core._
import edu.gemini.pot.sp.{ISPProgram, SPCompositeChange}
import edu.gemini.pot.spdb.{ProgramEvent, ProgramEventListener, IDBTriggerAction, IDBTriggerCondition, IDBDatabaseService}
import edu.gemini.spModel.dataset.{DatasetQaState, DatasetLabel}
import edu.gemini.spModel.dataset.Implicits._
import edu.gemini.spModel.obslog.ObsQaLog

import java.util.logging.Logger

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

import QaRequestTrigger._

/** ODB trigger implementation that watches for QA State updates and executes
  * the provided function in response.
  */


final class QaRequestTrigger(odb: IDBDatabaseService, f: List[QaRequest] => Unit) {
  def start(): Unit = {
    odb.registerTrigger(Condition, action)
    odb.addProgramEventListener(replaceListener)
  }

  def stop(): Unit = {
    odb.removeProgramEventListener(replaceListener)
    odb.unregisterTrigger(Condition, action)
  }

  // Adapts the provided listener function to the trigger API.
  private val action = new IDBTriggerAction {
    override def doTriggerAction(change: SPCompositeChange, handback: Object): Unit =
      handback match {
        case l: List[_] => f(l.collect { case r: QaRequest => r })
        case _          => Log.warning("Received trigger action for unexpected type: " + handback)
      }
  }

  // When a QA state is edited in an OT and that change is synchronized with the
  // database, a program replace event is fired.  This class handles that event
  // to find any updated QA states and notify the listener.
  private val replaceListener = new ProgramEventListener[ISPProgram] {
    override def programAdded(e: ProgramEvent[ISPProgram]): Unit   = () // ignore
    override def programRemoved(e: ProgramEvent[ISPProgram]): Unit = () // ignore

    override def programReplaced(e: ProgramEvent[ISPProgram]): Unit = {
      def m(p: ISPProgram): QaMap = {
        val maps = p.getAllObservations.asScala.flatMap { o =>
          Option(o.getObsQaLog).map(_.getDataObject).collect {
            case l: ObsQaLog => qaMap(l)
          }
        }
        (Map.empty[DatasetLabel, DatasetQaState]/:maps) { _ ++ _ }
      }

      val reqs = toRequests(m(e.getOldProgram), m(e.getNewProgram))
      if (reqs.nonEmpty) f(reqs)
    }
  }
}

object QaRequestTrigger {
  val Log = Logger.getLogger(QaRequestTrigger.getClass.getName)

  type QaMap = Map[DatasetLabel, DatasetQaState]

  private def qaMap(log: ObsQaLog): QaMap =
    log.getRecord.qaMap.mapValues(_.qaState)

  // Extract requests for all newMap entries without a corresponding oldMap
  // entry or for which the QA states differ.
  private def toRequests(oldMap: QaMap, newMap: QaMap): List[QaRequest] =
    newMap.filter {
      case (lab, nqa) => oldMap.get(lab).forall(_ =/= nqa)
    }.toList.map((QaRequest.apply _).tupled)

  object Condition extends IDBTriggerCondition {
    /** Returns a List[QaRequest] if the change matches and there are updates,
      * `null` otherwise (as required by the `IDBTriggerCondition` contract).
      *
      * @param change event fired because of a modification to a program node
      *
      * @return List[QaRequest] if the given change should generate a trigger,
      *         `null` otherwise
      */
    override def matches(change: SPCompositeChange): Object = {
      def compare(oldLog: ObsQaLog, newLog: ObsQaLog): Object = {
        // Sorry, IDBTriggerCondition requires a null result to signal that the
        // trigger condition didn't match and so the action shouldn't be fired.
        val reqs   = toRequests(qaMap(oldLog), qaMap(newLog))
        reqs.nonEmpty ? reqs | null
      }

      (change.getOldValue, change.getNewValue) match {
        case (oldLog: ObsQaLog, newLog: ObsQaLog) => compare(oldLog, newLog)
        case _                                    => null // see IDBTriggerCondition
      }
    }
  }
}
