// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.fire

import edu.gemini.pot.sp.SPObservationID
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.too.TooType
import argonaut._
import argonaut.Argonaut._

import java.time.Instant
import java.time.format.DateTimeFormatter.ISO_INSTANT
import java.time.temporal.{TemporalAccessor, TemporalQuery}
import java.util.UUID
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}


package object json {

  implicit def EncodeObservationId: EncodeJson[SPObservationID] =
    EncodeJson { oid => Option(oid.stringValue).asJson }

  private def decodeString[A](name: String)(f: String => A): DecodeJson[A] =
    DecodeJson { c =>
      c.as[String].flatMap { s =>
        Try(f(s)) match {
          case Failure(_) => DecodeResult.fail[A](s"Could not parse '$s' as $name", c.history)
          case Success(v) => DecodeResult.ok(v)
        }
      }
    }

  implicit def DecodeObservationId: DecodeJson[SPObservationID] =
    decodeString("observation id") { s => new SPObservationID(s) }

  implicit def EncodeProgramId: EncodeJson[SPProgramID] =
    EncodeJson { pid => Option(pid.stringValue).asJson }

  implicit def DecodeProgramId: DecodeJson[SPProgramID] =
    decodeString("program id")(SPProgramID.toProgramID)

  implicit def EncodeTooType: EncodeJson[TooType] =
    EncodeJson { _.getDisplayValue.asJson }

  implicit def DecodeTooType: DecodeJson[TooType] =
    decodeString("too type")(TooType.getTooType)

  implicit def DecodeUuid: DecodeJson[UUID] =
    decodeString("uuid")(UUID.fromString)

  implicit def DecodeInstant: DecodeJson[Instant] =
    decodeString("instant")(s => ISO_INSTANT.parse(s, new TemporalQuery[Instant] {
      override def queryFrom(temporal: TemporalAccessor): Instant =
        Instant.from(temporal)
    }))

  implicit def EncodeFireMessageSequence: EncodeJson[FireMessage.Sequence] =
    EncodeJson { s =>
      ("durationMilliseconds"  := s.duration.toMillis)  ->:
      ("completedStepCount"    := s.completedStepCount) ->:
      ("totalStepCount"        := s.totalStepCount)     ->:
      ("completedDatasetCount" := s.completedStepCount) ->:
      ("totalDatasetCount"     := s.totalStepCount)     ->:  // for now # steps == # datasets
      jEmptyObject
    }

  implicit def DecodeFireMessageSequence: DecodeJson[FireMessage.Sequence] =
    DecodeJson { c =>
      for {
        total     <- (c --\ "totalStepCount").as[Int]
        completed <- (c --\ "completedStepCount").as[Int]
        duration  <- (c --\ "durationMilliseconds").as[Long]
      } yield FireMessage.Sequence(
        total,
        completed,
        FiniteDuration(duration, TimeUnit.MILLISECONDS)
      )
    }

  implicit def EncodeFireMessage: EncodeJson[FireMessage] =
    EncodeJson { fm =>
      ("sequence"       := fm.sequence)                           ->:
      ("visitStartTime" := fm.visitStart.map(ISO_INSTANT.format)) ->:
      ("fileNames"      := fm.fileNames)                          ->:
      ("too"            := fm.too)                                ->:
      ("observationId"  := fm.observationId)                      ->:
      ("programId"      := fm.programId)                          ->:
      ("eventTime"      := ISO_INSTANT.format(fm.time))           ->:
      ("id"             := fm.uuid.toString)                      ->:
      ("nature"         := fm.nature)                             ->:
      jEmptyObject
    }

  implicit def DecodeFireMessage: DecodeJson[FireMessage] =
    DecodeJson { c =>
      for {
        id     <- (c --\ "id"            ).as[UUID]
        time   <- (c --\ "eventTime"     ).as[Instant]
        nature <- (c --\ "nature"        ).as[String]
        oid    <- (c --\ "observationId" ).as[Option[SPObservationID]]
        too    <- (c --\ "too"           ).as[Option[TooType]]
        vstart <- (c --\ "visitStartTime").as[Option[Instant]]
        dsets  <- (c --\ "fileNames"     ).as[List[String]]
        seq    <- (c --\ "sequence"      ).as[FireMessage.Sequence]
      } yield FireMessage(
        id,
        time,
        nature,
        oid,
        too,
        vstart,
        dsets,
        seq
      )
    }

}
