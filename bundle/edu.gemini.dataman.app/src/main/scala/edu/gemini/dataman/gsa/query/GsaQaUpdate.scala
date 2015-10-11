package edu.gemini.dataman.gsa.query

import edu.gemini.dataman.gsa.query.JsonCodecs._
import edu.gemini.spModel.dataset.{DatasetQaState, DatasetLabel}

import argonaut._
import Argonaut._

import scalaz.Scalaz._

object GsaQaUpdate {

  /** Defines a QA state update request. */
  case class Request(label: DatasetLabel, qa: DatasetQaState)

  // The GSA API accepts multiple kinds of updates but we only care about
  // setting the QA state.  Since the update parameter is a separate object
  // with its own keys and values, we need to do the encoding manually.
  implicit def EncodeJsonRequest: EncodeJson[Request] =
    EncodeJson((r: Request) =>
      ("values"     := ("qa_state" := r.qa.displayValue) ->: jEmptyObject) ->:
      ("data_label" := r.label.toString)                                   ->:
      jEmptyObject)

  /** Defines the response to a particular update request, identified by
    * DatasetLabel. */
  case class Response(label: DatasetLabel, failure: Option[String])

  implicit def EncodeJsonResponse: EncodeJson[Response] =
    EncodeJson((r: Response) =>
      ("error"  :=? r.failure)                            ->?:
      ("result" :=  r.failure.fold("true")(_ => "false")) ->:
      ("id"     :=  r.label.toString)                     ->:
      jEmptyObject
    )

  // Decoder for the server response to an update request for a particular
  // dataset.  Here we ignore the "result" flag that comes in the JSON and base
  // success or failure on the absence or presence of the error message.
  implicit def DecodeJsonResponse: DecodeJson[Response] =
    DecodeJson(c =>
      for {
        label   <- (c --\ "id").as[DatasetLabel]
        failure <- (c --\ "error").focus.fold(DecodeResult.ok(none[String]))(_.as[String].map(some))
      } yield Response(label, failure)
    )
}
