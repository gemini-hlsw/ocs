package edu.gemini.spModel.core

import org.scalacheck.Prop._
import org.specs2.mutable.Specification

import AlmostEqual._
import org.specs2.ScalaCheck

import scalaz._
import Scalaz._


object AlmostEqualSpec extends Specification with ScalaCheck with Arbitraries with Helpers {
  val smallDiff: Angle = Angle.fromDegrees(1e-6)
  val largeDiff: Angle = Angle.fromDegrees(1e-2)

  "Adding a very minute offset to an angle" should {
    "result in an angle considered almost equal to the original" ! forAll { (a : Angle) =>
      (a ~= (a + smallDiff)) && (a ~= (a - smallDiff))
    }
  }

  "Adding a very large offset to an angle" should {
    "result in an angle considered not equal to the original" ! forAll { (a: Angle) =>
      !(a ~= (a + largeDiff)) || !(a ~= (a - largeDiff))
    }
  }
}
