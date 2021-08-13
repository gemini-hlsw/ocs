// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.fire

import edu.gemini.pot.sp.{ISPObservation, SPObservationID}
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.dataset.{Dataset, DatasetLabel}
import edu.gemini.spModel.event.{EndDatasetEvent, ExecEvent, ObsExecEvent, StartDatasetEvent}
import edu.gemini.spModel.obs.plannedtime.{PlannedTime, PlannedTimeCalculator}
import edu.gemini.spModel.obslog.ObsLog
import edu.gemini.spModel.too.{Too, TooType}
import edu.gemini.wdba.fire.FireMessage.Sequence

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.function.BiFunction

import scala.concurrent.duration._
import scalaz._
import Scalaz._
import scalaz.concurrent.Task


final case class FireBuilder(
  event:       ObsExecEvent,
  too:         TooType,
  obsLog:      ObsLog,
  plannedTime: PlannedTime
) {

  val observationId: SPObservationID =
    event.getObsId

  val datasets: List[Dataset] =
    event match {
      case s: StartDatasetEvent => Option(s.getDataset).toList
      case e: EndDatasetEvent   => Option(e.getDatasetLabel).toList.flatMap(datasetsForLabel)
      case _                    => List.empty
    }

  def datasetsForLabel(label: DatasetLabel): List[Dataset] =
    Option(obsLog.getDatasetRecord(label)).map(_.exec.dataset).toList

  val visitStart: Option[Instant] = {
    val epochMilli = obsLog.getExecRecord.getLastVisitStartTime
    if (epochMilli > 0) Some(Instant.ofEpochMilli(epochMilli)) else None
  }

  val sequenceStepCount: Int =
    plannedTime.sequence.size

  val sequenceCompletedCount: Int =
    FireBuilder.foldPlannedTimeSteps(plannedTime, 0) { (sum, step) =>
      sum + (if (step.executed) 1 else 0)
    }

  private val sequenceDurationMs: Long =
    FireBuilder.foldPlannedTimeSteps(plannedTime, 0L) { (time, step) =>
      time + step.totalTime
    }

  val sequenceDuration: FiniteDuration =
    FiniteDuration(sequenceDurationMs, TimeUnit.MILLISECONDS)

  def toMessage: FireAction[FireMessage] =
    FireAction(UUID.randomUUID).map { u =>
      FireMessage(
        nature        = event.getName,
        uuid          = u,
        time          = Instant.ofEpochMilli(event.getTimestamp),
        observationId = Option(observationId),
        too           = Option(too),
        visitStart    = visitStart,
        datasets      = datasets,
        sequence      = Sequence(
          sequenceStepCount,
          sequenceCompletedCount,
          sequenceDuration
        )
      )
    }

}

object FireBuilder {

  private def foldPlannedTimeSteps[A](p: PlannedTime, init: A)(f: (A, PlannedTime.Step) => A): A =
    p.foldSteps(init, new BiFunction[A, PlannedTime.Step, A] {
      def apply(t: A, u: PlannedTime.Step): A = f(t, u)
    })

  private val NoData: FireAction[Option[FireBuilder]] =
    FireAction(Option.empty[FireBuilder])

  private def lookup(db: IDBDatabaseService, oid: SPObservationID): FireAction[ISPObservation] =
      EitherT(Task.delay(Option(db.lookupObservationByID(oid)) \/> FireFailure.obsNotFound(oid)))

  def fromEvent(db: IDBDatabaseService, event: ExecEvent): FireAction[Option[FireBuilder]] =
    event match {
      case e: ObsExecEvent => fromObsEvent(db, e)
      case _               => NoData
    }

  def fromObsEvent(db: IDBDatabaseService, event: ObsExecEvent): FireAction[Option[FireBuilder]] =
    Option(event.getObsId).fold(NoData) { oid =>
      lookup(db, oid).map { o =>
        for {
          log <- Option(ObsLog.getIfExists(o))
          pt  <- Option(PlannedTimeCalculator.instance.calc(o))
        } yield FireBuilder(event, Too.get(o), log, pt)
      }
    }

  def buildMessage(db: IDBDatabaseService, event: ExecEvent): FireAction[FireMessage] = {
    fromEvent(db, event)
      .flatMap(_.fold(FireMessage.emptyAt(Instant.ofEpochMilli(event.getTimestamp), event.getName))(_.toMessage))
  }

}
