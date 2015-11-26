package edu.gemini.dataman.app

import edu.gemini.dataman.DetailLevel
import edu.gemini.dataman.core.DmanId.Prog
import edu.gemini.pot.spdb.IDBDatabaseService

import java.security.Principal
import java.util.logging.{Level, Logger}

import scalaz._

/** A runnable that synchronizes summit and archive state of all programs in
  * the database.
  */
final class ProgramSyncRunnable(
              odb: IDBDatabaseService,
              user: java.util.Set[Principal],
              sync: List[Prog] => Unit) extends Runnable {

  private val Log = Logger.getLogger(getClass.getName)

  override def run(): Unit =
    PidFunctor.exec(odb, user) match {
      case \/-(pids) =>
        Log.log(DetailLevel, s"Dataman synchronizing ${pids.length} programs.")
        sync(pids.map(Prog))

      case -\/(e)    =>
        Log.log(Level.WARNING, e.explain, e.exception.orNull)
    }
}
