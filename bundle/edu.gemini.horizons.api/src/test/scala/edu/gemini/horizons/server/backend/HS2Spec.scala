package edu.gemini.horizons.server.backend

import scalaz._
import Scalaz._
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

object HS2Spec extends Specification with ScalaCheck {

  import HorizonsService2.{ HS2Error, Row, Search, search, lookupEphemeris, EphemerisEmpty }
  import edu.gemini.spModel.core.{ HorizonsDesignation => HD, Site, Ephemeris }

  def runSearch[A](s: Search[A]): HS2Error \/ List[Row[A]] =
    search(s).run.unsafePerformIO

  def runLookup(d: HD, n: Int): HS2Error \/ Ephemeris =
    lookupEphemeris(d, Site.GS, n).run.unsafePerformIO

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
        Row(HD.AsteroidOldStyle(103), "Hera"),
        Row(HD.AsteroidOldStyle(121), "Hermione"),
        Row(HD.AsteroidOldStyle(135), "Hertha"),
        Row(HD.AsteroidOldStyle(206), "Hersilia"),
        Row(HD.AsteroidOldStyle(214), "Aschera")
      ))
    }

    "handle single result (Format 1) 90377 Sedna (2003 VB12)" in {
      runSearch(Search.Asteroid("sedna")) must_== \/-(List(
        Row(HD.AsteroidNewStyle("2003 VB12"), "Sedna")
      ))
    }

    "handle single result (Format 2) 29 Amphitrite" in {
      runSearch(Search.Asteroid("amphitrite")) must_== \/-(List(
        Row(HD.AsteroidOldStyle(29), "Amphitrite")
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

  }

  "major body search" should {

    "handle empty results" in {
      runSearch(Search.MajorBody("kjhdwekuq")) must_== \/-(Nil)
    }

    "handle empty results with small-body fallthrough (many)" in {
      runSearch(Search.MajorBody("hu")) must_== \/-(Nil)
    }

    "handle empty results with small-body fallthrough (single)" in {
      runSearch(Search.MajorBody("hermione")) must_== \/-(Nil)
    }

    "handle multiple results" in {
      runSearch(Search.MajorBody("mar")).map(_.take(5)) must_== \/-(List(
        Row(HD.MajorBody(4), "Mars Barycenter"),
        Row(HD.MajorBody(499), "Mars"),
        Row(HD.MajorBody(723), "Margaret")
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

    "return a populated ephemeris for Amphitrite (asteroid, new style)" in {
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

}
