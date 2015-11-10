package edu.gemini.spModel.core

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz.Scalaz._
import scalaz._

object MagnitudeBandSpec extends Specification with ScalaCheck with Arbitraries with Helpers {

  "MagnitudeBand " should {

    "have ascending start, center and end values" !
      forAll { (b: MagnitudeBand) =>
        b.start <= b.center && b.center <= b.end
      }
  }

  "MagnitudeBand serialization" should {

   "support Java binary" !
     forAll { (ee: MagnitudeBand) =>
       canSerialize(ee)
     }
  }

}


