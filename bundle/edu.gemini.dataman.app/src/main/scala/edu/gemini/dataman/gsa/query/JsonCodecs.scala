package edu.gemini.dataman.gsa.query

import edu.gemini.spModel.dataset.{DatasetGsaState, DatasetMd5, DatasetQaState, DatasetLabel}

import argonaut._
import Argonaut._
import argonaut.DecodeResult.{fail, ok}

import java.text.ParseException
import java.time.Instant
import java.time.format.DateTimeParseException
import java.time.temporal.{TemporalAccessor, TemporalQuery}

/**
 * A collection of JSON codecs for existing Gemini Java types, as they are
 * encoded by the GSA server.
 */
private[query] object JsonCodecs {

  // *** DatasetMd5 Codec ***

  def invalidMd5(s: String): String =
    s"Could not parse `$s` as an MD5 value"

  implicit val CodecDatasetMd5: CodecJson[DatasetMd5] =
    CodecJson(
      (md5: DatasetMd5) => jString(md5.hexString),
      c => c.as[String].flatMap { hexString =>
        DatasetMd5.parse(hexString).fold(fail[DatasetMd5](invalidMd5(hexString), c.history))(ok)
      }
    )


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

  implicit val EncodeJsonDatasetQaState: EncodeJson[DatasetQaState] =
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

  implicit val EncodeJsonInstant: EncodeJson[Instant] =
    EncodeJson((i: Instant) => jString(TimeFormat.format(i)))

  def invalidTimeInstance(s: String): String =
    s"Could not parse `$s` as a time instance"

  implicit val DecodeJsonInstant: DecodeJson[Instant] =
    DecodeJson(c =>
      c.as[String].flatMap { s =>
        try {
          ok(TimeParse.parse(s, new TemporalQuery[Instant]() {
            override def queryFrom(ta: TemporalAccessor): Instant = Instant.from(ta)
          }))
        } catch {
          case _: DateTimeParseException => fail(invalidTimeInstance(s), c.history)
        }
      }
    )

  // *** DatasetGsaState Codec ***

  implicit val CodecDatasetGsaState: CodecJson[DatasetGsaState] =
    casecodec3(DatasetGsaState.apply, DatasetGsaState.unapply)("qa_state", "entrytime", "data_md5")
}
