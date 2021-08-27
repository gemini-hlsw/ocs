// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.fire

import edu.gemini.wdba.fire.json._
import edu.gemini.wdba.session.ServiceExecutor
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.event.ExecEvent
import argonaut._
import Argonaut._

import java.net.URL
import java.util.concurrent.ArrayBlockingQueue
import java.util.logging.{Level, Logger}

import scala.concurrent.duration._
import scalaz._
import Scalaz._

/**
 * FireService receives `ExecEvent`s and turns them into `FireMessage`s and then
 * does "something" with them according to the supplied `process`.  In
 * production this will be posting to a configured web server.  For testing, it
 * can be to simply record the messages received for later verification.
 */
final class FireService(
  db:      IDBDatabaseService,
  process: FireMessage => FireAction[Unit],
  timeout: Duration = 1.minute
) {

  import FireService.{Log, QueueCapacity}

  private val queue: ArrayBlockingQueue[ExecEvent] =
    new ArrayBlockingQueue(QueueCapacity, true)

  private val exec: ServiceExecutor =
    new ServiceExecutor("FireService", task)

  def handleEvent(event: ExecEvent): Unit = {
    Log.log(DetailLevel, s"FireService enqueuing event $event")
    queue.add(event)
  }

  def start(): Unit =
    exec.start()

  def stop(): Unit =
    exec.stop()

  private object task extends Runnable {

    def handleEvent(event: ExecEvent): Unit = {
        Log.log(DetailLevel, s"FireService handling event $event")

        val action: FireAction[FireMessage] =
          for {
            m <- FireBuilder.buildMessage(db, event)
            _ <- process(m)
          } yield m

        action.run.unsafePerformSyncAttemptFor(timeout) match {
          case -\/(t)      => Log.log(Level.WARNING, "Exception attempting to handle FIRE message", t)
          case \/-(-\/(e)) => Log.log(Level.WARNING, e.message, e.exception.orNull)
          case \/-(\/-(m)) => Log.log(DetailLevel, s"Handled FireMessage:\n${m.asJson.spaces2}")
        }
    }

    def run(): Unit =
      while (!Thread.currentThread.isInterrupted) {
        try {
          handleEvent(queue.take())
        } catch {
          case _: InterruptedException =>
            Log.log(DetailLevel, "Stopping FireService")
          case ex: Throwable =>
            Log.log(Level.WARNING, "Unexpected exception taking exec event from queue", ex)
        }
      }

  }
}

object FireService {

  val Log: Logger =
    Logger.getLogger(getClass.getName)

  val QueueCapacity: Int =
    10000

  def loggingOnly(db: IDBDatabaseService): FireService =
    new FireService(db, _ => FireAction.unit)

  def posting(db: IDBDatabaseService, url: URL): FireService =
    new FireService(db, msg => FirePost.post[FireMessage](url, msg).void)

}
