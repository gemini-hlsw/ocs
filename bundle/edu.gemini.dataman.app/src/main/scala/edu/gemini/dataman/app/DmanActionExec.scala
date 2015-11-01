package edu.gemini.dataman.app

import edu.gemini.dataman.core._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.dataset.DatasetExecRecord
import edu.gemini.spModel.dataset.QaRequestStatus.PendingPost
import edu.gemini.spModel.dataset.SummitState.ActiveRequest

import java.util.logging.{Logger, Level}

import scalaz.{-\/, \/-}

sealed trait DmanActionExec {
  /** Executes the given action in the caller's thread.
    * DMAN TODO: Or so I claim.  What does runAync actually do?
    */
  def now(action: DmanAction[DatasetUpdates]): Unit


  /** Executes the given action in a separate thread.
    */
  def fork(action: DmanAction[DatasetUpdates]): Unit

  /** Creates a Runnable instance that will execute the action in the same
    * Thread that calls the run method.  This is intended to be used from a
    * thread pool for repeated tasks like polling.
    */
  def runnable(action: DmanAction[DatasetUpdates]): Runnable
}


object DmanActionExec {
  val Log = Logger.getLogger(DmanActionExec.getClass.getName)

  def apply(config: DmanConfig, odb: IDBDatabaseService) = new DmanActionExec {
    val post = GsaPostAction(config.summitHost, config.site, config.gsaAuth, odb)

    // Processes the output of a DmanAction[Updates] to fork any required GSA
    // update QA posts.
    val completion: (TryDman[DatasetUpdates] => Unit) = {
      case \/-((_, exUpdates)) =>
        val pairs = exUpdates.collect {
          case DatasetExecRecord(d, ActiveRequest(_, _, id0, PendingPost, _, _), _) => (d.getLabel, id0)
        }
        if (pairs.nonEmpty) fork(post.postUpdate(pairs))

      case -\/(f)              =>
        Log.log(Level.WARNING, f.explain, f.exception.orNull)
    }

    override def now(action: DmanAction[DatasetUpdates]): Unit =
      action.run.runAsync { r => completion(DmanAction.mergeFailure(r)) }

    override def fork(action: DmanAction[DatasetUpdates]): Unit =
      action.forkAsync(completion)

    override def runnable(action: DmanAction[DatasetUpdates]): Runnable =
      new Runnable {
        override def run(): Unit = now(action)
      }
  }
}