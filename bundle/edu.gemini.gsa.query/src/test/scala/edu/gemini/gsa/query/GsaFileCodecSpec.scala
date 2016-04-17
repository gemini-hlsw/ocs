package edu.gemini.gsa.query

import argonaut.Argonaut._
import argonaut._
import edu.gemini.gsa.query.JsonCodecs._
import org.specs2.mutable.Specification

import scalaz.{-\/, \/}

object GsaFileCodecSpec extends Specification {
  val json0 =
    """
      |[
      |    {
      |        "lastmod": "2015-09-12 17:11:25.941802+00:00",
      |        "name": "S20120222S0081.fits",
      |        "mdready": true,
      |        "data_md5": "024b5084c7e22decece9b8440d5ca799",
      |        "filename": "S20120222S0081.fits.bz2",
      |        "compressed": true,
      |        "file_size": 7449511,
      |        "path": "",
      |        "file_md5": "e63412be4d340c8dd6cb24376c211ff5",
      |        "data_size": 14633280,
      |        "md5": "e63412be4d340c8dd6cb24376c211ff5",
      |        "size": 7449511
      |    }
      |]
    """.stripMargin

  val files0 =
    GsaFile("S20120222S0081.fits") :: Nil

  val json1 =
    """
      |[
      |    {
      |        "lastmod": "2015-09-12 17:11:25.941802+00:00",
      |        "name": "S20120222S0081.fits",
      |        "mdready": true,
      |        "data_md5": "024b5084c7e22decece9b8440d5ca799",
      |        "filename": "S20120222S0081.fits.bz2",
      |        "compressed": true,
      |        "file_size": 7449511,
      |        "path": "",
      |        "file_md5": "e63412be4d340c8dd6cb24376c211ff5",
      |        "data_size": 14633280,
      |        "md5": "e63412be4d340c8dd6cb24376c211ff5",
      |        "size": 7449511
      |    },
      |    {
      |        "lastmod": "2015-09-12 10:26:06.795562+00:00",
      |        "name": "S20130629S0037.fits",
      |        "mdready": true,
      |        "data_md5": "9ca5e616e6d5de2ea9944c5da19eab66",
      |        "filename": "S20130629S0037.fits.bz2",
      |        "compressed": true,
      |        "file_size": 15343615,
      |        "path": "",
      |        "file_md5": "dca9ae3b597a34eb113502cc956d0f3a",
      |        "data_size": 57551040,
      |        "md5": "dca9ae3b597a34eb113502cc956d0f3a",
      |        "size": 15343615
      |    }
      |]
    """.stripMargin

  val files1 =
    GsaFile("S20120222S0081.fits") :: GsaFile("S20130629S0037.fits") :: Nil

  val missingName =
    """
      |[
      |    {
      |        "lastmod": "2015-09-12 17:11:25.941802+00:00",
      |        "mdready": true,
      |        "data_md5": "024b5084c7e22decece9b8440d5ca799",
      |        "filename": "S20120222S0081.fits.bz2",
      |        "compressed": true,
      |        "file_size": 7449511,
      |        "path": "",
      |        "file_md5": "e63412be4d340c8dd6cb24376c211ff5",
      |        "data_size": 14633280,
      |        "md5": "e63412be4d340c8dd6cb24376c211ff5",
      |        "size": 7449511
      |    }
      |]
    """.stripMargin

  def jsonList(js: String*): String = js.mkString("[\n", ",\n", "\n]")

  def roundTrip(entries: List[GsaFile]): List[GsaFile] =
    Parse.decodeOption[List[GsaFile]](entries.asJson.spaces2).get

  "GsaFile codec" should {
    "decode empty list" in {
      Parse.decodeOption[List[GsaFile]]("[]").get must_== Nil
    }

    "decode singleton list" in {
      Parse.decodeOption[List[GsaFile]](json0).get must_== files0
    }

    "decode list" in {
      Parse.decodeOption[List[GsaFile]](json1).get must_== files1
    }

    "roundtrip empty list" in {
      roundTrip(Nil) must_== Nil
    }

    "roundtrip singleton list" in {
      roundTrip(files0) must_== files0
    }

    "roundtrip list" in {
      roundTrip(files1) must_== files1
    }

    "cannot decode without name" in {
      \/.fromEither(Parse.decodeEither[GsaRecord](missingName)) match {
        case -\/(_) => success
        case _      => failure("Expecting missing filename warning")
      }
    }
  }
}
