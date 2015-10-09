package edu.gemini.dataman.gsa.query

import argonaut._
import Argonaut._

import scalaz._
import Scalaz._

/** Just a temporary, trivial use of argonaut as a proof of concept. */
object FileList extends App {

  val sample =
    """
      |[
      |    {
      |        "lastmod": "2015-09-12 03:06:41.656631+00:00",
      |        "name": "N20150218S0145.fits",
      |        "mdready": true,
      |        "data_md5": "b2b71e5fd771a08132d800e7b0d941ce",
      |        "filename": "N20150218S0145.fits.bz2",
      |        "compressed": true,
      |        "file_size": 6607082,
      |        "path": "",
      |        "file_md5": "c8396a64ded56d459191f899e7ce75a5",
      |        "data_size": 15102720,
      |        "md5": "c8396a64ded56d459191f899e7ce75a5",
      |        "size": 6607082
      |    },
      |    {
      |        "lastmod": "2015-09-12 03:06:41.663787+00:00",
      |        "name": "N20150218S0146.fits",
      |        "mdready": true,
      |        "data_md5": "397e891c76cabf533160713103901cf2",
      |        "filename": "N20150218S0146.fits.bz2",
      |        "compressed": true,
      |        "file_size": 6606276,
      |        "path": "",
      |        "file_md5": "de4cd106d3aad1fde0a62c0acf265bc2",
      |        "data_size": 15102720,
      |        "md5": "de4cd106d3aad1fde0a62c0acf265bc2",
      |        "size": 6606276
      |    },
      |    {
      |        "lastmod": "2015-09-12 03:06:41.723540+00:00",
      |        "name": "N20150218S0147.fits",
      |        "mdready": true,
      |        "data_md5": "603aefe16f1aaeada4be572eca69d379",
      |        "filename": "N20150218S0147.fits.bz2",
      |        "compressed": true,
      |        "file_size": 6603887,
      |        "path": "",
      |        "file_md5": "8e6b1ac9c3107108f209a2331e3b9979",
      |        "data_size": 15102720,
      |        "md5": "8e6b1ac9c3107108f209a2331e3b9979",
      |        "size": 6603887
      |    }
      |]
    """.stripMargin

  val nameLens = jObjectPL >=> jsonObjectPL("name") >=> jStringPL

  val array = (for {
    p <- Parse.parseOption(sample)
    a <- p.array
  } yield a) | Nil

  array.flatMap(nameLens.get).foreach(println)

  // Prints:
  //  N20150218S0145.fits
  //  N20150218S0146.fits
  //  N20150218S0147.fits

}
