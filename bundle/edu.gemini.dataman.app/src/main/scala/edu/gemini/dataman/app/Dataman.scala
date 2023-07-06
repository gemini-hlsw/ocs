package edu.gemini.dataman.app

import edu.gemini.dataman.DetailLevel
import edu.gemini.dataman.core.DmanId.{Dset, Obs, Prog}
import edu.gemini.dataman.core._
import edu.gemini.gsa.query.QaRequest
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.util.security.principal.StaffPrincipal

import java.security.Principal
import java.time.Duration
import java.util.UUID
import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}
import java.util.logging.Logger
import scalaz._
import Scalaz._

import java.time.Instant


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
  /** How long to wait before the initial program sync tasks are placed in the
    * pool.
    */
  val progSyncDelay   = Duration.ofMinutes(1)

  /** How long to wait before the initial observation refresh task runs.
    */
  val obsRefreshDelay = Duration.ofSeconds(30)

  /** Minimum time that must pass after a dataset is marked failed before it
    * can be retried.
    */
  val retryMinDelay   = Duration.ofMinutes(1)

  /** How long between retry attempts for failed datasets.
    */
  val retryPeriod     = Duration.ofMinutes(15)

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
    Log.log(DetailLevel, "Data Manager resetting incomplete QA requests.")

    ResetOngoingAction(odb, User).unsafeRun.map { case (_, ex) =>
      Log.log(DetailLevel, "Data Manager reset to failed: " + ex.map(_.label).mkString(", "))
    }
  }

  private def create(config: DmanConfig, odb: IDBDatabaseService): TryDman[Dataman] = {
    val obsLogAction = new ObsLogActions(odb)
    val exec         = DmanActionExec(config, odb)

    val pollArchive  = GsaPollActions(config.archiveHost, config.site, odb)
    val pollSummit   = GsaPollActions(config.summitHost, config.site, odb)

    def pollService(name: String, act: GsaPollActions): PollService =
      PollService(name, workerCount = 10) { id =>
        exec.now(id match {
          case Prog(pid) => act.program(pid)
          case Obs(oid)  => act.observation(oid)
          case Dset(lab) => act.dataset(lab)
        })
      }

    val archivePollService = pollService("Archive", pollArchive)
    val summitPollService  = pollService("Summit",  pollSummit)
    val pollServices       = List(archivePollService, summitPollService)

    val trigger  = new QaRequestTrigger(odb, (qaRequest: List[QaRequest]) => {
      exec.fork(obsLogAction.setQa(qaRequest))
    })

    val pool = Executors.newScheduledThreadPool(5, new ThreadFactory() {
      override def newThread(r: Runnable): Thread =
        new Thread(r, "GSA Poll Thread: " + UUID.randomUUID().toString) <|
          (_.setDaemon(true))                                           <|
          (_.setPriority(Thread.NORM_PRIORITY - 1))
    })

    def shutdownNow(): Unit = {
      trigger.stop()
      pollServices.foreach(_.shutdown())
      pool.shutdownNow()
    }

    def schedule(r: Runnable, delay: Duration, period: Duration): Unit =
      pool.scheduleWithFixedDelay(r, delay.toMillis, period.toMillis, TimeUnit.MILLISECONDS)

    def schedulePoll(a: DmanAction[DatasetUpdates], period: PollPeriod): Unit =
      schedule(exec.runnable(a), period.time, period.time)

    val res = tryOp {
      schedulePoll(pollArchive.tonight,  config.archivePoll.tonight)
      schedulePoll(pollSummit.tonight,   config.summitPoll.tonight)
      schedulePoll(pollArchive.thisWeek, config.archivePoll.thisWeek)
      schedulePoll(pollSummit.thisWeek,  config.summitPoll.thisWeek)

      val progArchiveSync = new ProgramSyncRunnable(odb, User, pids => archivePollService.addAll(pids))
      val progSummitSync  = new ProgramSyncRunnable(odb, User, pids => summitPollService.addAll(pids))
      schedule(progArchiveSync, progSyncDelay, config.archivePoll.allPrograms.time)
      schedule(progSummitSync, progSyncDelay, config.summitPoll.allPrograms.time)

      // Note "now" is passed by value to be re-evaluated every time it is needed.
      val orr = new ObsRefreshRunnable(odb, User, Instant.now(), oids =>
        pollServices.foreach(_.addAll(oids))
      )
      schedule(orr, obsRefreshDelay, config.obsRefreshPeriod.time)
      schedule(new RetryFailedRunnable(odb, User, retryMinDelay, exec), retryMinDelay.plusMillis(1), retryPeriod)

      trigger.start()

      new Dataman {
        override def stop(): TryDman[Unit] = tryOp { shutdownNow() }
      }
    }

    res <| { _.swap.foreach { _ => shutdownNow() }}
  }

}