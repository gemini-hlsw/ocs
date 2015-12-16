package edu.gemini.gsa.query

import edu.gemini.spModel.dataset.{DatasetLabel, DatasetQaState}

import scalaz.Equal

/** Representation of a request to set the QA state corresponding to the
  * given data label.
  */
final case class QaRequest(label: DatasetLabel, qa: DatasetQaState)

object QaRequest {
  implicit val EqualQaRequest: Equal[QaRequest] = Equal.equalA
}
