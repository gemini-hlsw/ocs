package edu.gemini.spModel.core

import scalaz._
import Scalaz._
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import AlmostEqual.AlmostEqualOps

object InterpolateSpec extends Specification with ScalaCheck with Arbitraries with Helpers {

  val test = ==>>(
    10L -> 10.0,
    20L -> 20.0,
    30L -> 20.0,
    40L -> 15.0,
    50L -> 10.0
  )

  val min = test.findMin.get._1
  val max = test.findMax.get._1

  "iLookup" should {

    "find all values" in {
      test.keys.traverse(test.iLookup) must_== Some(test.values)
    }

    "interpolate between adjacent points" in {
      val keyPairs     = test.keys   zip test.keys.tail
      val valuePairs   = test.values zip test.values.tail
      val interpolated = keyPairs.traverse { case (a, b) => test.iLookup((a + b) / 2) }
      val computed     = 
        (keyPairs zip valuePairs) map { case ((k1, k2), (v1, v2)) =>
          val f = ((k2 - k1) / 2.0) / (k2 - k1)
          v1 + (v2 - v1) * f
        }
      interpolated must_== Some(computed)
    }

    "interpolate a value everywhere on the min/max range" in {
      (min |-> max).traverse(test.iLookup).isDefined
    }

    "interpolate no values outside the min/max range" ! forAll { (n: Long) =>
      test.iLookup(n).filter(_ => n < min || n > max).isEmpty
    }

  }

  "Interpolate" should { 

    "work in either direction" ! forAll { (a: Int, b: Int) =>
      val x = Interpolate[Long, Double].interpolate((10L, a.toDouble), (20L, b.toDouble), 12L)
      val y = Interpolate[Long, Double].interpolate((10L, b.toDouble), (20L, a.toDouble), 18L)
      x ~= y
    }
    
  }

  "Coordinate interpolation" should {

    "be accurate in ra" ! forAll { (a: Coordinates, b: Coordinates) =>
      val x = Interpolate[Long, Coordinates].interpolate((10L, a), (20L, b), 12L)
      val y = Interpolate[Long, Double].interpolate((10L, a.ra.toAngle.toDegrees), (20L, b.ra.toAngle.toDegrees), 12L)
      x.ra.toAngle.toDegrees ~= y
    }

    "be accurate in dec" ! forAll { (a: Coordinates, b: Coordinates) =>
      val x = Interpolate[Long, Coordinates].interpolate((10L, a), (20L, b), 12L)
      val y = Interpolate[Long, Double].interpolate((10L, a.dec.toDegrees), (20L, b.dec.toDegrees), 12L)
      x.dec.toDegrees ~= y
    }

  }

}
