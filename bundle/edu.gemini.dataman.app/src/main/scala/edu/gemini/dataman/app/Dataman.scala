package edu.gemini.dataman.app

import edu.gemini.dataman.core._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.dataset.{QaRequestStatus, DatasetExecRecord}
import edu.gemini.spModel.dataset.SummitState.ActiveRequest
import edu.gemini.util.security.principal.StaffPrincipal

import java.security.Principal
import java.time.Duration
import java.util.UUID
import java.util.concurrent.{TimeUnit, ThreadFactory, Executors}
import java.util.logging.Logger

import scalaz._
import Scalaz._


/** Dataman ties together the various pieces of the application.  Once created
  * via `Dataman.start(config, odb)`, it begins polling archive servers,
  * watching for QA update requests, etc.
  */
sealed trait Dataman {

  /** Stops a running Dataman, cleaning up the resources that it uses.
    */
  def stop(): TryDman[Unit]
}


object Dataman {
  /** Minimum time that must pass after a dataset is marked failed before it
    * can be retried.
    */
  val retryMinDelay = Duration.ofMinutes(1)

  /** How long between retry attempts for failed datasets.
    */
  val retryPeriod   = Duration.ofMinutes(15)

  /** How long to wait before the initial program sync tasks are placed in the
    * pool.
    */
  val progSyncDelay = Duration.ofMinutes(1)

  private val Log  = Logger.getLogger(getClass.getName)
  private val User = java.util.Collections.singleton[Principal](StaffPrincipal.Gemini)

  /** Creates and starts a Dataman instance.
   */
  def start(config: DmanConfig, odb: IDBDatabaseService): TryDman[Dataman] =
    for {
      _ <- resetOngoingRequests(config, odb)
      d <- create(config, odb)
    } yield d

  private def resetOngoingRequests(config: DmanConfig, odb: IDBDatabaseService): TryDman[Unit] = {
    Log.info("Data Manager resetting incomplete QA requests.")

    val action = for {
      labs <- DatasetFunctor.exec(odb, User) {
                case DatasetExecRecord(ds, ActiveRequest(_, _, uid, stat, _, _), _) if stat != QaRequestStatus.Accepted => (ds.getLabel, uid)
              }.liftDman
      ups  <- new ObsLogActions(odb).failRequest(labs, "Data Manager was shutdown before request completed.")
    } yield ups

    action.unsafeRun.map { case (_, ex) =>
      Log.info("Data Manager reset to failed: " + ex.map(_.label).mkString(", "))
    }
  }

  private def create(config: DmanConfig, odb: IDBDatabaseService): TryDman[Dataman] = {
    val obsLogAction = new ObsLogActions(odb)
    val exec         = DmanActionExec(config, odb)

    val pollSummit   = GsaPollActions(config.summitHost, config.site, odb)
    val pollArchive  = GsaPollActions(config.archiveHost, config.site, odb)

    val progSync     = new ProgramSyncRunnable(User, pollArchive.program, pollSummit.program, exec, odb)
    val trigger      = new QaRequestTrigger(odb, (qaRequest: List[QaRequest]) => {
      exec.fork(obsLogAction.setQa(qaRequest))
    })

    val pool = Executors.newScheduledThreadPool(5, new ThreadFactory() {
      override def newThread(r: Runnable) =
        new Thread(r, "GSA Poll Thread: " + UUID.randomUUID().toString) <|
          (_.setDaemon(true))                                           <|
          (_.setPriority(Thread.NORM_PRIORITY - 1))
    })

    def shutdownNow(): Unit = {
      trigger.stop()
      progSync.shutdownNow()
      pool.shutdownNow()
    }

    def schedule(r: Runnable, delay: Duration, period: Duration): Unit =
      pool.scheduleWithFixedDelay(r, delay.toMillis, period.toMillis, TimeUnit.MILLISECONDS)

    def schedulePoll(a: DmanAction[DatasetUpdates], period: PollPeriod): Unit =
      schedule(exec.runnable(a), period.time, period.time)

    val res = tryOp {
      schedulePoll(pollSummit.tonight,   config.tonightPeriod)
      schedulePoll(pollArchive.tonight,  config.tonightPeriod)
      schedulePoll(pollSummit.thisWeek,  config.thisWeekPeriod)
      schedulePoll(pollArchive.thisWeek, config.thisWeekPeriod)

      schedule(progSync, progSyncDelay, config.allPeriod.time)
      schedule(new RetryFailedRunnable(User, retryMinDelay, exec, odb), retryMinDelay.plusMillis(1), retryPeriod)

      trigger.start()

      new Dataman {
        override def stop(): TryDman[Unit] = tryOp { shutdownNow() }
      }
    }

    res <| { _.swap.foreach { _ => shutdownNow() }}
  }

}