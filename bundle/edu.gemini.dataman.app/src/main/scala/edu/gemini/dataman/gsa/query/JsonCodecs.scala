package edu.gemini.dataman.gsa.query

import edu.gemini.spModel.dataset.{DatasetQaState, DatasetLabel}

import argonaut._
import Argonaut._
import argonaut.DecodeResult.{fail, ok}

import java.text.ParseException
import java.time.{Instant, ZoneId}
import java.time.format.{DateTimeParseException, DateTimeFormatter}
import java.time.temporal.{TemporalAccessor, TemporalQuery}

/**
 * A collection of JSON codecs for existing Gemini Java types, as they are
 * encoded by the GSA server.
 */
private[query] object JsonCodecs {

  // *** DatasetLabel Codec ***

  implicit val EncodeJsonDatasetLabel: EncodeJson[DatasetLabel] =
    EncodeJson((label: DatasetLabel) => jString(label.toString))

  def invalidDatasetLabel(s: String): String =
    s"Could not parse `$s` as a dataset label"

  implicit val DecodeJsonDatasetLabel: DecodeJson[DatasetLabel] =
    DecodeJson(c => {
      c.as[String].flatMap { s =>
        try {
          ok(new DatasetLabel(s))
        } catch {
          case _: ParseException => fail(invalidDatasetLabel(s), c.history)
        }
      }
    })


  // *** DatasetQaState Codec ***

  implicit def EncodeJsonDatasetQaState: EncodeJson[DatasetQaState] =
    EncodeJson((qa: DatasetQaState) => jString(qa.displayValue))

  def invalidDatasetQaState(s: String): String =
    s"Could not parse `$s` as a dataset QA state"

  implicit val DecodeJsonDatasetQaState: DecodeJson[DatasetQaState] =
    DecodeJson(c =>
      c.as[String].flatMap { s =>
        val qaOpt = DatasetQaState.values.find(_.displayValue.toLowerCase == s.toLowerCase)
        qaOpt.fold(fail[DatasetQaState](invalidDatasetQaState(s), c.history))(ok)
      }
    )


  // *** GSA Timestamp Codec ***

  // Time format used in the GSA JSON.  The time zone (UTC) is needed in order
  // to encode times.
  val TimeFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSSSSSxxx").withZone(ZoneId.of("Z"))

  implicit val EncodeJsonInstant: EncodeJson[Instant] =
    EncodeJson((i: Instant) => jString(TimeFormat.format(i)))

  def invalidTimeInstance(s: String): String =
    s"Could not parse `$s` as a time instance"

  implicit val DecodeJsonInstant: DecodeJson[Instant] =
    DecodeJson(c =>
      c.as[String].flatMap { s =>
        try {
          ok(TimeFormat.parse(s, new TemporalQuery[Instant]() {
            override def queryFrom(ta: TemporalAccessor): Instant = Instant.from(ta)
          }))
        } catch {
          case _: DateTimeParseException => fail(invalidTimeInstance(s), c.history)
        }
      }
    )
}
