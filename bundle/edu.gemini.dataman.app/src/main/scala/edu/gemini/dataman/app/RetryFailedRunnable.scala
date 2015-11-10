package edu.gemini.dataman.app

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.dataset.DatasetExecRecord
import edu.gemini.spModel.dataset.QaRequestStatus.Failed
import edu.gemini.spModel.dataset.SummitState.ActiveRequest

import java.security.Principal
import java.time.{Duration, Instant}
import java.util.logging.{Logger, Level}

import scalaz._
import Scalaz._

/** A Runnable that resets any Failed active QA requests to pending and then
  * forks an action to retry posting the requests to the FITS storage server.
  */
final class RetryFailedRunnable(
              user: java.util.Set[Principal],
              minDelay: Duration,
              exec: DmanActionExec,
              odb: IDBDatabaseService) extends Runnable {

  private val Log = Logger.getLogger(getClass.getName)

  override def run(): Unit = {
    // Make sure that at least a bit of time has past since the QA attempt
    // failed before trying again.
    def oldEnough(i: Instant): Boolean =
      Duration.between(i, Instant.now()).compareTo(minDelay) > 0

    Log.info("Dataman retry failed QA updates.")
    DatasetFunctor.collectExec(odb, user) {
      case DatasetExecRecord(ds, ActiveRequest(_, _, id0, Failed(_), w, _), _) if oldEnough(w) => (ds.getLabel, id0)
    } match {
      case \/-(labs) =>
        if (labs.isEmpty) {
          Log.info("No failed QA updates.")
        } else {
          val (prefix, suffix) = labs.splitAt(50)
          Log.info("Retrying failed datasets: " + prefix.mkString(", ") + (suffix.isEmpty ? "" | " ..."))
          exec.fork(new ObsLogActions(odb).resetFailed(labs))
        }
      case -\/(f)   =>
        Log.log(Level.WARNING, f.explain, f.exception.orNull)
    }
  }
}
