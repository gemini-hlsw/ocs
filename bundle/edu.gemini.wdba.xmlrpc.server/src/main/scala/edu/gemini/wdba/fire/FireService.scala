// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.fire

import edu.gemini.wdba.fire.json._

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.event.ExecEvent

import argonaut._
import Argonaut._

import java.util.concurrent.ArrayBlockingQueue
import java.util.logging.{Level, Logger}

import scala.concurrent.duration._
import scalaz.{-\/, \/-}

final class FireService(
  db:      IDBDatabaseService,
  process: FireMessage => FireAction[Unit],
  timeout: Duration = 1.minute
) extends Runnable {

  import FireService.{Log, QueueCapacity}

  private val queue: ArrayBlockingQueue[ExecEvent] =
    new ArrayBlockingQueue(QueueCapacity, true)

  def addEvent(event: ExecEvent): Unit = {
    Log.info(s"FireService enqueuing event $event")
    queue.add(event)
  }

  def run(): Unit =

    while (!Thread.currentThread.isInterrupted) {

      val event = queue.take()

      Log.info(s"FireService handling event $event")

      val action: FireAction[FireMessage] =
        for {
          m <- FireBuilder.buildMessage(db, event)
          _ <- process(m)
        } yield m

      action.run.unsafePerformSyncAttemptFor(timeout) match {
        case -\/(t)      => Log.log(Level.WARNING, "Exception attempting to handle FIRE message", t)
        case \/-(-\/(e)) => Log.log(Level.WARNING, e.message, e.exception.orNull)
        case \/-(\/-(m)) => Log.info(s"Handled FireMessage:\n${m.asJson.spaces2}")
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

  def forTesting(
    db:           IDBDatabaseService,
    stringBuffer: StringBuffer
  ): FireService =
    new FireService(db, m => FireAction(stringBuffer.append(m.asJson.spaces2)))

}
