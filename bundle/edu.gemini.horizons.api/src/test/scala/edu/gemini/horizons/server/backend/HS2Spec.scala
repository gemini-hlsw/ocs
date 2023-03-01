package edu.gemini.horizons.server.backend

import scalaz._
import Scalaz._
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** NOTE: there is no test for format 6 because all known examples have been
  * reclassified as comets.
  */
object HS2Spec extends Specification with ScalaCheck {

  import HorizonsService2.{ HS2Error, Row, Search, search, lookupEphemeris, lookupEphemerisE, EphemerisEmpty }
  import edu.gemini.spModel.core.{ HorizonsDesignation => HD, Semester, Site, Ephemeris }
  import edu.gemini.horizons.api.EphemerisEntry

  def runSearch[A](s: Search[A]): HS2Error \/ List[Row[A]] =
    search(s).run.unsafePerformIO

  def runLookup(d: HD, n: Int): HS2Error \/ Ephemeris =
    lookupEphemeris(d, Site.GS, n).run.unsafePerformIO

  def runLookupE(d: HD, n: Int): HS2Error \/ (Long ==>> EphemerisEntry) = {
    val sem = Semester.parse("2023A") // pick a point in time
    lookupEphemerisE(d, Site.GS, sem.getStartDate(Site.GS), sem.getEndDate(Site.GS), n)(_.some).run.unsafePerformIO
  }

  "comet search" should {

    "handle empty results" in {
      runSearch(Search.Comet("kjhdwekuq")) must_== \/-(Nil)
    }

     "handle multiple results" in {
       runSearch(Search.Comet("hu")).map(_.take(5)) must_== \/-(List(
         Row(HD.Comet("67P"), "Churyumov-Gerasimenko"),
         Row(HD.Comet("106P"), "Schuster"),
         Row(HD.Comet("130P"), "McNaught-Hughes"),
         Row(HD.Comet("178P"), "Hug-Bell"),
         Row(HD.Comet("C/1880 Y1"), "Pechule")
       ))
     }

    "handle single result (Format 1) Hubble (C/1937 P1)" in {
      runSearch(Search.Comet("hubble")) must_== \/-(List(
        Row(HD.Comet("C/1937 P1"), "Hubble")
      ))
    }

    "handle single result (Format 2) 81P/Wild 2 pattern" in {
      runSearch(Search.Comet("81P")) must_== \/-(List(
        Row(HD.Comet("81P"), "Wild 2")
      ))
    }

  }

  "asteroid search" should {

    "handle empty results" in {
      runSearch(Search.Asteroid("kjhdwekuq")) must_== \/-(Nil)
    }

    "handle multiple results" in {
      runSearch(Search.Asteroid("her")).map(_.take(5)) must_== \/-(List(
        Row(HD.AsteroidNewStyle("A868 RA"), "Hera"),
        Row(HD.AsteroidNewStyle("A872 JA"), "Hermione"),
        Row(HD.AsteroidNewStyle("A874 DA"), "Hertha"),
        Row(HD.AsteroidNewStyle("A879 TC"), "Hersilia"),
        Row(HD.AsteroidNewStyle("A880 DB"), "Aschera")
      ))
    }

    "handle single result (Format 1) 90377 Sedna (2003 VB12)" in {
      runSearch(Search.Asteroid("sedna")) must_== \/-(List(
        Row(HD.AsteroidNewStyle("2003 VB12"), "Sedna")
      ))
    }

    "handle single result (Format 2) 29 Amphitrite" in {
      runSearch(Search.Asteroid("amphitrite")) must_== \/-(List(
        Row(HD.AsteroidNewStyle("A854 EB"), "Amphitrite")
      ))
    }

    "handle single result (Format 3) (2016 GB222)" in {
      runSearch(Search.Asteroid("2016 GB222")) must_== \/-(List(
        Row(HD.AsteroidNewStyle("2016 GB222"), "2016 GB222")
      ))
    }

    "handle single result (Format 4) 418993 (2009 MS9)" in {
      runSearch(Search.Asteroid("2009 MS9")) must_== \/-(List(
        Row(HD.AsteroidNewStyle("2009 MS9"), "2009 MS9")
      ))
    }

    "handle single result (Format 5) 1I/'Oumuamua (A/2017 U1)" in {
      runSearch(Search.Asteroid("A/2017 U1")) must_== \/-(List(
        Row(HD.AsteroidNewStyle("A/2017 U1"), "A/2017 U1")
      ))
    }

  }

  "major body search" should {

    "handle empty results" in {
      runSearch(Search.MajorBody("kjhdwekuq")) must_== \/-(Nil)
    }

    "handle empty results with small-body fallthrough (many)" in {
      runSearch(Search.MajorBody("hh")) must_== \/-(Nil)
    }

    "handle empty results with small-body fallthrough (single)" in {
      runSearch(Search.MajorBody("hermione")) must_== \/-(Nil)
    }

    "handle multiple results" in {
      println(runSearch(Search.MajorBody("mar")).map(_.take(5)))
      runSearch(Search.MajorBody("mar")).map(_.take(5)) must_== \/-(List(
        Row(HD.MajorBody(4), "Mars Barycenter"),
        Row(HD.MajorBody(499), "Mars"),
        Row(HD.MajorBody(723), "Margaret"),
        Row(HD.MajorBody(50000), "Quaoar (primary body)"),    //  up in October of 2022 (no idea why)
        Row(HD.MajorBody(65803), "Didymos (primary body)") // RCN: these two cases started showing
      ))
    }

    "handle single result with trailing space (!)" in {
      runSearch(Search.MajorBody("charon")) must_== \/-(List(
        Row(HD.MajorBody(901), "Charon")
      ))
    }

    "handle single result without trailing space" in {
      runSearch(Search.MajorBody("europa")) must_== \/-(List(
        Row(HD.MajorBody(502), "Europa")
      ))
    }

  }

  "ephemeris lookup" should {

    "return a populated ephemeris for Halley (comet)" in {
      runLookup(HD.Comet("1P"), 100).map(_.size).toOption.exists { s =>
        95 <= s && s <= 105
      }
    }

    "return a populated ephemeris for Sedna (asteroid, new style)" in {
      runLookup(HD.AsteroidNewStyle("2003 VB12"), 100).map(_.size).toOption.exists { s =>
        95 <= s && s <= 105
      }
    }

    "return a populated ephemeris for 'Oumuamua (asteroid, new style)" in {
      runLookup(HD.AsteroidNewStyle("A/2017 U1"), 100).map(_.size).toOption.exists { s =>
        95 <= s && s <= 105
      }
    }

    "return a populated ephemeris for Amphitrite (asteroid, old style)" in {
      runLookup(HD.AsteroidOldStyle(29), 100).map(_.size).toOption.exists { s =>
        95 <= s && s <= 105
      }
    }

    "return a populated ephemeris for Charon (major body)" in {
      runLookup(HD.MajorBody(901), 100).map(_.size).toOption.exists { s =>
        95 <= s && s <= 105
      }
    }

    "return an empty ephemeris on bogus lookup" in {
      runLookup(HD.Comet("29134698698376"), 100).map(_.size) must_== -\/(EphemerisEmpty)
    }

  }

  "ephemeris element lookup" should {

    // sanity check with a known ephemeris, to catch changes in output format
    "return a correct ephemeris for Halley (comet)" in {
      val result = runLookupE(HD.Comet("1P"), 10).toOption.map(_.toList.map(_.toString.replace('\t', ' ')))
      // result.foreach(_.foreach(println))
      result must_== Some(List(
        "(1675184400000,2023-Jan-31 17:00:00 UTC 08:17:44.423 +02:15:52.58 -4.29131 1.183281 -1.0 25.535)",
        "(1676748600000,2023-Feb-18 19:30:00 UTC 08:15:43.624 +02:25:31 -3.88567 1.474407 -1.0 25.54)",
        "(1678312800000,2023-Mar-08 22:00:00 UTC 08:14:01.534 +02:36:42.31 -3.10269 1.605413 2.3 25.55)",
        "(1679877000000,2023-Mar-27 00:30:00 UTC 08:12:47.94 +02:48:16.16 -2.01608 1.573011 1.192 25.565)",
        "(1681441200000,2023-Apr-14 03:00:00 UTC 08:12:09.054 +02:59:03.78 -0.74028 1.38962 2.016 25.583)",
        "(1683005400000,2023-May-02 05:30:00 UTC 08:12:07.022 +03:08:06.46 0.577456 1.085775 -1.0 25.601)",
        "(1684569600000,2023-May-20 08:00:00 UTC 08:12:40.294 +03:14:39.1 1.767352 0.701981 -1.0 25.619)",
        "(1686133800000,2023-Jun-07 10:30:00 UTC 08:13:44.373 +03:18:12.92 2.707549 0.271758 -1.0 25.634)",
        "(1687698000000,2023-Jun-25 13:00:00 UTC 08:15:12.407 +03:18:36.17 3.351083 -0.17043 20.719 25.646)",
        "(1689262200000,2023-Jul-13 15:30:00 UTC 08:16:56.008 +03:15:51.91 3.688434 -0.58833 1.412 25.653)",
        "(1690826400000,2023-Jul-31 18:00:00 UTC 08:18:45.968 +03:10:17.27 3.734413 -0.95298 1.306 25.655)"
      ))
    }

  }

}
