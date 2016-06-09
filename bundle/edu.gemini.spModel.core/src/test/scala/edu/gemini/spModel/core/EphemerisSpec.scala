package edu.gemini.spModel.core

import org.specs2.mutable.Specification

import scalaz._
import Scalaz._
import org.scalacheck.Prop._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

object EphemerisSpec extends Specification with ScalaCheck with Arbitraries with Helpers {

  implicit class EphemerisTrickery(e: Ephemeris) {

    // Peek at the transient lazy field to see if it has been assigned
    def isCompressed: Boolean = {
      val f = e.getClass.getDeclaredField("data")
      f.setAccessible(true)
      f.get(e) == null
    }

  }

  "Ephemeris Data" should {

    "be serializable" ! forAll { (e: Ephemeris) =>
      canSerialize(e)
    }

    "be compressed on construction" ! forAll { (e: Ephemeris) =>
      e.isCompressed
    }


    "be decompressed on access to 'data' field" ! forAll { (e: Ephemeris) =>
      e.data
      e.isCompressed == false
    }

    "be compressed after serialization roundtrip" ! forAll { (e: Ephemeris) =>
      e.data
      canSerializeP(e) { (e, e2) =>
        e2.isCompressed && !e.isCompressed
      }
    }

  }

}
