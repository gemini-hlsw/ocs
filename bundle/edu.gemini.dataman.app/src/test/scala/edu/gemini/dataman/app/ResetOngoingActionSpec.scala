package edu.gemini.dataman.app

import edu.gemini.dataman.core.DmanAction
import edu.gemini.spModel.dataset.QaRequestStatus.{Failed, Accepted}
import edu.gemini.spModel.dataset.SummitState.{ActiveRequest, Idle}
import edu.gemini.spModel.dataset.{DatasetExecRecord, DatasetRecord}

import scalaz.{\/-, -\/}

object ResetOngoingActionSpec extends TestSupport {

  private def isFail(der: DatasetExecRecord): Boolean = der match {
    case DatasetExecRecord(_, ActiveRequest(_, _, _, Failed(_), _, _), _) => true
    case _                                                                => false
  }

  "ResetOngoingAction" should {
    "fail pending requests" ! forAllPrograms { (odb, progs) =>
      val drs = allDatasets(progs)

      val expectedFail = drs.collect {
        case DatasetRecord(qa, DatasetExecRecord(_, Idle(gsa), _)) if qa.qaState != gsa.qa                        => qa.label
        case DatasetRecord(_, DatasetExecRecord(ds, ActiveRequest(_, _, uid, stat, _, _), _)) if stat != Accepted => ds.getLabel
      }.toSet

      DmanAction.mergeFailure(ResetOngoingAction(odb, User).run.unsafePerformSyncAttempt) match {
        case -\/(f) =>
          println(f.explain)
          false

        case \/-(ups) =>
          // There should be no QA updates
          val qaUpdatesEmpty = ups._1.isEmpty

          // All the exec records that are now failed were the ones we expected
          // to be set to failed.
          val execUpdatesMatch = ups._1.isEmpty && ups._2.collect {
            case ex if isFail(ex) => ex.label
          }.toSet == expectedFail

          // The changes were actually made in the database.
          val changesCorrect = allDatasets(progs).filter(ds => expectedFail(ds.label)).forall(dr => isFail(dr.exec))

          qaUpdatesEmpty && execUpdatesMatch && changesCorrect
      }
    }

  }

}
