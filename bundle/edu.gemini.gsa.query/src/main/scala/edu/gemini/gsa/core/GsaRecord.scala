package edu.gemini.gsa.core

import edu.gemini.spModel.dataset.{DatasetGsaState, DatasetLabel}

import scalaz.Equal

/** Representation of a GSA file record, with only the parts we care about for
  * display in the OT and processing in the Data Manager.
  *
  * The `time` parameter is the GSA ingestion instant which, lacking a more
  * explicit alternative, is used as a version number.
  */
final case class GsaRecord(label: Option[DatasetLabel], filename: String, state: DatasetGsaState)

object GsaRecord {
  implicit val EqualGsaRecord: Equal[GsaRecord] = Equal.equalA
}