package edu.gemini.gsa.query

import edu.gemini.spModel.dataset.DatasetLabel

/** Defines the response to a particular update request, identified by
  * DatasetLabel.
  */
final case class QaResponse(label: DatasetLabel, failure: Option[String])

import scalaz.Equal

object QaResponse {
  implicit val EqualQaResponse: Equal[QaResponse] = Equal.equalA
}