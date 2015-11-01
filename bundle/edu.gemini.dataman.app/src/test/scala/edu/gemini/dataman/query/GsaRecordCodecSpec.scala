package edu.gemini.dataman.query

import argonaut.Argonaut._
import argonaut._
import edu.gemini.dataman.core.GsaRecord
import edu.gemini.dataman.query.JsonCodecs._
import edu.gemini.spModel.dataset.DatasetQaState._
import edu.gemini.spModel.dataset.{DatasetGsaState, DatasetLabel, DatasetMd5}
import org.specs2.mutable.Specification

import java.time.Instant

import scalaz._


object GsaRecordCodecSpec extends Specification {
  val json0 =
    """
      |{
      |  "qa_state": "Pass",
      |  "data_label": "GN-2015A-Q-4-95-019",
      |  "entrytime": "2015-09-12 03:06:39.961433+00:00",
      |  "filename": "N20150912S0124.fits",
      |  "data_md5": "0bee89b07a248e27c83fc3d5951213c1"
      |}
    """.stripMargin

  val json1 =
  """
    |{
    |  "qa_state": "Usable",
    |  "data_label": "GN-2015A-Q-4-92-004",
    |  "entrytime": "2015-09-12 03:06:41.803116+00:00",
    |  "filename": "N20150912S0125.fits",
    |  "data_md5": "ba1f2511fc30423bdbb183fe33f3dd0f"
    |}
  """.stripMargin

  val file0 =
    GsaRecord(new DatasetLabel("GN-2015A-Q-4-95-019"), "N20150912S0124.fits",  DatasetGsaState(PASS, Instant.parse("2015-09-12T03:06:39.00Z").plusNanos(961433000), DatasetMd5.parse("0bee89b07a248e27c83fc3d5951213c1").get))

  val file1 =
    GsaRecord(new DatasetLabel("GN-2015A-Q-4-92-004"), "N20150912S0125.fits", DatasetGsaState(USABLE, Instant.parse("2015-09-12T03:06:41.00Z").plusNanos(803116000), DatasetMd5.parse("ba1f2511fc30423bdbb183fe33f3dd0f").get))

  def jsonList(js: String*): String = js.mkString("[\n", ",\n", "\n]")

  def roundTrip(entries: List[GsaRecord]): List[GsaRecord] =
    Parse.decodeOption[List[GsaRecord]](entries.asJson.spaces2).get

  "GsaFile codec" should {
    "decode empty list" in {
      Parse.decodeOption[List[GsaRecord]]("[]").get must_== Nil
    }

    "decode singleton list" in {
      Parse.decodeOption[List[GsaRecord]](jsonList(json0)).get must_== List(file0)
    }

    "decode list" in {
      Parse.decodeOption[List[GsaRecord]](jsonList(json0, json1)).get must_== List(file0, file1)
    }

    "roundtrip empty list" in {
      roundTrip(Nil) must_== Nil
    }

    "roundtrip singleton list" in {
      roundTrip(List(file0)) must_== List(file0)
    }

    "roundtrip list" in {
      roundTrip(List(file0, file1)) must_== List(file0, file1)
    }

    "fail missing key" in {
      val json =
        """
          |{
          |  "qa_state": "Pass",
          |  "data_label": "GN-2015A-Q-4-95-019",
          |  "entrytime": "2015-09-12 03:06:39.961433+00:00"
          |}
        """.stripMargin

      Parse.decodeEither[GsaRecord](json) match {
        case -\/(_) => success
        case _      => failure("Expecting missing filename warning")
      }
    }

    "fail invalid value" in {
      val json =
        """
          |{
          |  "qa_state": "PASS",
          |  "data_label": "GN-2015A-Q-4-95-019",
          |  "entrytime": "2015-09-12 03:06:39.961433+00:00",
          |  "filename": "N20150912S0124.fits"
          |}
        """.stripMargin

      Parse.decodeEither[GsaRecord](json) match {
        case -\/(_) => success
        case _      => failure("Expecting unparseable QA state warning")
      }
    }
  }
}
