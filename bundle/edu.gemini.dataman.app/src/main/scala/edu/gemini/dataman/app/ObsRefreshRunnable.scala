package edu.gemini.dataman.app

import edu.gemini.dataman.DetailLevel
import edu.gemini.dataman.core.DmanId
import edu.gemini.dataman.core.DmanId.Obs
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.dataset.DataflowStatus.{Diverged, SummitOnly, SyncPending, UpdateInProgress}
import edu.gemini.spModel.dataset.{DataflowStatus, DatasetLabel, DatasetRecord}

import java.security.Principal
import java.util.logging.{Level, Logger}
import scalaz.Scalaz._
import scalaz._

import java.time.Duration
import java.time.Instant
import scala.util.Try

/** A `Runnable` that scans the database looking for datasets for which updates
  * are expected and then asks the archive for a status update.
  * @param now Timestamp in the ODB server when this functor runs.
  */
final class ObsRefreshRunnable(
  odb:     IDBDatabaseService,
  user:    java.util.Set[Principal],
  now:     => Instant,
  refresh: List[Obs] => Unit
) extends Runnable {

  private val Log = Logger.getLogger(getClass.getName)

  // How long we can still hope to see any update from the FITS storage service.
  // After this time, we'll stop checking frequently but still pick up an
  // update should it occur with one of the other normal poll periods.
  private val HopeDuration: Duration =
    Duration.ofDays(1L);

  override def run(): Unit = {

    // "now" is the timestamp in the ODB server when this functor runs.  We'll
    // be comparing this to the FITS server timestamp which is obviously
    // problematic but at the scale we're interested in (days) any discrepancies
    // should not matter if the clocks are at least somewhat accurate.
    def timeSinceDatasetUpdateOnSummit(dr: DatasetRecord): Option[Duration] =
      dr.exec.summit.gsaTimestampOption.flatMap { t =>
        Try(Duration.between(t, now)).toOption
      }

    def updateExpected(dr: DatasetRecord): Boolean =
      DataflowStatus.derive(dr) match {
        case SyncPending | UpdateInProgress => true
        case SummitOnly  | Diverged         => timeSinceDatasetUpdateOnSummit(dr).exists(_.compareTo(HopeDuration) <= 0)
        case _                              => false
      }

    def obsIds(labs: List[DatasetLabel]): List[DmanId.Obs] =
      labs.map(_.getObservationId).distinct.map(DmanId.Obs)

    Log.log(DetailLevel, "Dataman dataflow update.")
    DatasetFunctor.collect(odb, user) {
      case dr if updateExpected(dr) => dr.label
    } match {
      case \/-(labs) =>
        if (labs.isEmpty) {
          Log.log(DetailLevel, "No expected updates.")
        } else {
          val obs = obsIds(labs)
          val (prefix, suffix) = obs.splitAt(50)
          Log.log(DetailLevel, "Refreshing expected updates: " + prefix.mkString(", ") + (suffix.isEmpty ? "" | " ..."))
          refresh(obs)
        }
      case -\/(f)   =>
        Log.log(Level.WARNING, f.explain, f.exception.orNull)
    }
  }
}
