package edu.gemini.dataman.query

import edu.gemini.dataman.core.{GsaRecord, QaRequest, QaResponse}
import edu.gemini.spModel.dataset.{DatasetGsaState, DatasetLabel, DatasetMd5, DatasetQaState}

import argonaut._
import argonaut.Argonaut._
import argonaut.DecodeResult.{fail, ok}

import java.text.ParseException
import java.time.Instant
import java.time.format.DateTimeParseException
import java.time.temporal.{TemporalAccessor, TemporalQuery}

import scalaz.Scalaz._

/** A collection of JSON codecs for existing Gemini Java types, as they are
  * encoded by the GSA server.
  */
private[query] object JsonCodecs {

  // *** DatasetMd5 ***

  def invalidMd5(s: String): String =
    s"Could not parse `$s` as an MD5 value"

  implicit val CodecDatasetMd5: CodecJson[DatasetMd5] =
    CodecJson(
      (md5: DatasetMd5) => jString(md5.hexString),
      c => c.as[String].flatMap { hexString =>
        DatasetMd5.parse(hexString).fold(fail[DatasetMd5](invalidMd5(hexString), c.history))(ok)
      }
    )


  // *** DatasetLabel ***

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

  implicit val CodecJsonDatasetLabel = CodecJson.derived[DatasetLabel]


  // *** DatasetQaState ***

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

  implicit val CodecJsonDataseQaState = CodecJson.derived[DatasetQaState]

  // *** GSA Timestamp ***

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

  implicit val CodecJsonInstant = CodecJson.derived[Instant]

  // *** DatasetGsaState ***

  implicit val CodecDatasetGsaState: CodecJson[DatasetGsaState] =
    casecodec3(DatasetGsaState.apply, DatasetGsaState.unapply)("qa_state", "entrytime", "data_md5")


  // *** GsaRecord ***

  implicit val EncodeJsonGsaRecord: EncodeJson[GsaRecord] =
    EncodeJson { (r: GsaRecord) =>
      ("data_label" := r.label.asJson) ->:
      ("filename"   := r.filename)     ->:
        r.state.asJson
    }

  implicit val DecodeJsonGsaRecord: DecodeJson[GsaRecord] =
    DecodeJson { c =>
      for {
        l <- (c --\ "data_label").as[DatasetLabel]
        f <- (c --\ "filename").as[String]
        s <- implicitly[DecodeJson[DatasetGsaState]].decode(c)
      } yield GsaRecord(l, f, s)
    }

  implicit val CodecJsonGsaRecord = CodecJson.derived[GsaRecord]

  // *** QaRequest **

  // The GSA API accepts multiple kinds of updates but we only care about
  // setting the QA state.  Since the update parameter is a separate object
  // with its own keys and values, we need to do the encoding manually.
  implicit val EncodeQaRequest: EncodeJson[QaRequest] =
    EncodeJson((r: QaRequest) =>
      ("values"     := ("qa_state" := r.qa.displayValue) ->: jEmptyObject) ->:
      ("data_label" := r.label.toString)                                   ->:
      jEmptyObject
    )

  // ** QaResponse **

  implicit val EncodeJsonQaResponse: EncodeJson[QaResponse] =
    EncodeJson((r: QaResponse) =>
      ("error"  :=? r.failure)                            ->?:
      ("result" :=  r.failure.fold("true")(_ => "false")) ->:
      ("id"     :=  r.label.toString)                     ->:
      jEmptyObject
    )

  // Decoder for the server response to an update request for a particular
  // dataset.  Here we ignore the "result" flag that comes in the JSON and base
  // success or failure on the absence or presence of the error message.
  implicit val DecodeJsonQaResponse: DecodeJson[QaResponse] =
    DecodeJson(c =>
      for {
        label   <- (c --\ "id").as[DatasetLabel]
        failure <- (c --\ "error").focus.fold(DecodeResult.ok(none[String]))(_.as[String].map(some))
      } yield QaResponse(label, failure)
    )

  implicit val CodecJsonQaResponse = CodecJson.derived[QaResponse]
}
