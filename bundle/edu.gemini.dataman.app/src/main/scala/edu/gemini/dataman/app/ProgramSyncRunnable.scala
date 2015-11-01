package edu.gemini.dataman.app

import edu.gemini.dataman.core._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.SPProgramID

import java.security.Principal
import java.util.UUID
import java.util.concurrent.{ThreadFactory, Executors}
import java.util.logging.{Level, Logger}

import scalaz._
import Scalaz._

/** A runnable that synchronizes summit and archive state of all programs in
  * the database.
  */
final class ProgramSyncRunnable(
              user: java.util.Set[Principal],
              archivePoll: SPProgramID => DmanAction[DatasetUpdates],
              summitPoll:  SPProgramID => DmanAction[DatasetUpdates],
              exec: DmanActionExec,
              odb: IDBDatabaseService) extends Runnable {

  private val Log = Logger.getLogger(getClass.getName)

  // These tasks are IO Bound.  There's little to the actual computation but
  // there will be queries to remote servers.  Why 25?  No reason.  Maybe it
  // should be 50 or a 100, not sure.
  private val pool = Executors.newFixedThreadPool(25, new ThreadFactory {
    override def newThread(r: Runnable): Thread =
      new Thread(r, "Program sync thread: " + UUID.randomUUID()) <|
        (_.setPriority(Thread.NORM_PRIORITY - 1))                <|
        (_.setDaemon(true))
  })

  final class SyncOne(pid: SPProgramID, loc: String, poll: SPProgramID => DmanAction[DatasetUpdates]) extends Runnable {
    override def run(): Unit = {
      Log.fine(s"Dataman synchronizing $pid with $loc")
      exec.now(poll(pid))
      Log.fine(s"Dataman finished synchronizing $pid with $loc")
    }
  }

  override def run(): Unit =
    PidFunctor.exec(odb, user) match {
      case \/-(pids) =>
        Log.info(s"Dataman synchronizing ${pids.length} programs.")
        pids.foreach { pid =>
          pool.execute(new SyncOne(pid, "summit",  summitPoll))
          pool.execute(new SyncOne(pid, "archive", archivePoll))
        }

      case -\/(e)    =>
        Log.log(Level.WARNING, e.explain, e.exception.orNull)
    }

  // DMAN TODO: this task will spawn a large number of sub-tasks [# progs]*2,
  // which will take a while to complete. What's the proper way to handle
  // shutdown
  def shutdownNow(): Unit = {
    Log.info("Dataman shutdown sync all thread pool.")
    pool.shutdownNow()
  }
}
