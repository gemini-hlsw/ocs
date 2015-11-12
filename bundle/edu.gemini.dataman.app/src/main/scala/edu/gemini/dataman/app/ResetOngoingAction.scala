package edu.gemini.dataman.app

import edu.gemini.dataman.core.{DmanAction, DatasetUpdates}
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.dataset.Implicits._
import edu.gemini.spModel.dataset.QaRequestStatus.Accepted
import edu.gemini.spModel.dataset.SummitState.{ActiveRequest, Idle}
import edu.gemini.spModel.dataset.{DatasetExecRecord, DatasetRecord, DatasetQaState, DatasetLabel}

import java.security.Principal
import java.util.UUID

import scalaz._
import Scalaz._

/** A constructor that makes a `DmanAction` which, when executed, finds all
  * pending sync QA updates along with all active requests that were not (yet)
  * accepted and explicitly sets them to the `Failed` request state.  This
  * allows these requests to be retried automatically by the
  * `RetryFailedRunnable` periodic task.
  */
object ResetOngoingAction {

  def apply(odb: IDBDatabaseService, user: java.util.Set[Principal]): DmanAction[DatasetUpdates] = {
    val failureCasePf: PartialFunction[DatasetRecord, (DatasetLabel, DatasetQaState \/ UUID)] = {
      // Pending requests that were not noticed because the Data Manager wasn't running.
      case DatasetRecord(qa, DatasetExecRecord(_, Idle(gsa), _)) if qa.qaState =/= gsa.qa                       =>
        (qa.label, qa.qaState.left[UUID])

      // Active requests that hadn't completed.
      case DatasetRecord(_, DatasetExecRecord(ds, ActiveRequest(_, _, uid, stat, _, _), _)) if stat != Accepted =>
       (ds.getLabel, uid.right[DatasetQaState])
    }

    type IdlePair   = (DatasetLabel, DatasetQaState)
    type ActivePair = (DatasetLabel, UUID)

    val Empty       = (List.empty[IdlePair], List.empty[ActivePair])

    def partition(ds: List[(DatasetLabel, DatasetQaState \/ UUID)]): (List[IdlePair], List[ActivePair]) =
      (Empty/:ds) { case ((idle,active), (lab,dis)) =>
          dis.fold(qa => ((lab, qa) :: idle, active), uid => (idle, (lab, uid) :: active))
      }

    val ola = new ObsLogActions(odb)

    for {
      res       <- DatasetFunctor.collect(odb, user)(failureCasePf).liftDman

      (idle, active) = partition(res)

      upsIdle   <- ola.failIdleRequest(idle,     "Data Manager was not online when QA request was made. It will be retried automatically.")
      upsActive <- ola.failActiveRequest(active, "Data Manager was shutdown before request completed. It will be retried automatically.")
    } yield upsIdle |+| upsActive
  }
}
