package edu.gemini.dataman.gsa.query

import edu.gemini.dataman.gsa.query.JsonCodecs._
import edu.gemini.spModel.dataset.{DatasetGsaState, DatasetLabel, DatasetQaState}

import argonaut._
import Argonaut._

import java.time.Instant

/** Representation of a GSA file record, with only the parts we care about for
  * display in the OT and processing in the Data Manager.
  *
  * The `time` parameter is the GSA ingestion instant which, lacking a more
  * explicit alternative, is used as a version number. */
case class GsaFile(label: DatasetLabel, filename: String, state: DatasetGsaState)

object GsaFile {
  implicit val EncodeJsonGsaFile: EncodeJson[GsaFile] =
    EncodeJson { (f: GsaFile) =>
      ("data_label" := f.label.asJson) ->:
      ("filename" := f.filename) ->:
        f.state.asJson
    }

  implicit val DecodeJsonGsaFile: DecodeJson[GsaFile] =
    DecodeJson { c =>
      for {
        l <- (c --\ "data_label").as[DatasetLabel]
        f <- (c --\ "filename").as[String]
        s <- implicitly[DecodeJson[DatasetGsaState]].decode(c)
      } yield GsaFile(l, f, s)
    }


//    casecodec4(GsaFile.apply, GsaFile.unapply)("data_label", "qa_state", "entrytime", "filename")
}