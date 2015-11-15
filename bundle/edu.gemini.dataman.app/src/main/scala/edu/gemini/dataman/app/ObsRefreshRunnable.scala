package edu.gemini.dataman.app

import edu.gemini.dataman.core.DmanId
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.dataset.DataflowStatus.{Diverged, SummitOnly, UpdateInProgress, SyncPending}
import edu.gemini.spModel.dataset.{DatasetLabel, DataflowStatus, DatasetRecord}

import java.security.Principal
import java.util.logging.{Level, Logger}

import scalaz.Scalaz._
import scalaz._

/** A `Runnable` that scans the database looking for datasets for which updates
  * are expected and then asks the archive for a status update.
  */
final class ObsRefreshRunnable(
              odb: IDBDatabaseService,
              user: java.util.Set[Principal],
              pollService: PollService) extends Runnable {

  private val Log = Logger.getLogger(getClass.getName)

  override def run(): Unit = {
    def updateExpected(dr: DatasetRecord): Boolean =
      DataflowStatus.derive(dr) match {
        case SyncPending | UpdateInProgress | SummitOnly | Diverged => true
        case _                                                      => false
      }

    def obsIds(labs: List[DatasetLabel]): List[DmanId.Obs] =
      labs.map(_.getObservationId).distinct.map(DmanId.Obs)

    Log.info("Dataman dataflow update.")
    DatasetFunctor.collect(odb, user) {
      case dr if updateExpected(dr) => dr.label
    } match {
      case \/-(labs) =>
        if (labs.isEmpty) {
          Log.info("No expected updates.")
        } else {
          val obs = obsIds(labs)
          val (prefix, suffix) = obs.splitAt(50)
          Log.info("Refreshing expected updates: " + prefix.mkString(", ") + (suffix.isEmpty ? "" | " ..."))
          pollService.addAll(obs)
        }
      case -\/(f)   =>
        Log.log(Level.WARNING, f.explain, f.exception.orNull)
    }
  }
}
