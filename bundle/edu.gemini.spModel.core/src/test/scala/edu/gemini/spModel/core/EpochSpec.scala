package edu.gemini.spModel.core

import scalaz._
import Scalaz._
import org.scalacheck.Prop._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

final class EpochSpec extends Specification with ScalaCheck with Arbitraries {
  "Epoch" should {
    "give an offset of 0 to its own year" ! {
      math.abs(Epoch.J2000.untilEpochYear(Epoch.J2000.year)) < 1e-9
    }
  }

  "provide sanity in offsets using short" ! {
    forAll { (s: Short) =>
      Epoch.J2000.untilEpochYear(Epoch.J2000.year + s.toDouble) shouldEqual s.toDouble
    }
  }
}

