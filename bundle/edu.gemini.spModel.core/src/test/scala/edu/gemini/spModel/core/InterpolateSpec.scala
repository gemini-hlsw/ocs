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

    "be invariant at min" ! forAll { (a: Int, b: Int) =>
      val x = Interpolate[Long, Double].interpolate((10L, a.toDouble), (20L, b.toDouble), 10L)
      x ~= a.toDouble
    }

    "be invariant at max" ! forAll { (a: Int, b: Int) =>
      val x = Interpolate[Long, Double].interpolate((10L, a.toDouble), (20L, b.toDouble), 20L)
      x ~= b.toDouble
    }

    "work off the end" ! forAll { (a: Int, b: Int) =>
      val x = Interpolate[Long, Double].interpolate((10L, a.toDouble), (20L, b.toDouble), 30L)
      x ~= b.toDouble + (b.toDouble - a.toDouble)
    }

  }

  "Coordinate interpolation" should {

    "be invariant at min" ! forAll { (a: Coordinates) => 
      val b = a.offset(Angle.fromDegrees(20), Angle.fromDegrees(-20))
      val c = Interpolate[Long, Coordinates].interpolate((10L, a), (20L, b), 10L)
      c ~= a
    }

    "be invariant at max" ! forAll { (a: Coordinates) => 
      val b = a.offset(Angle.fromDegrees(20), Angle.fromDegrees(20))
      val c = Interpolate[Long, Coordinates].interpolate((10L, a), (20L, b), 20L)
      c ~= b
    }

    "work backwards" ! forAll { (a: Coordinates) => 
      val b = a.offset(Angle.fromDegrees(20), Angle.fromDegrees(20))
      val c = Interpolate[Long, Coordinates].interpolate((10L, b), (20L, a), 12L)
      val d = Interpolate[Long, Coordinates].interpolate((10L, a), (20L, b), 18L)
      c ~= d
    }

    "interpolate Declination correctly" ! forAll { (a: Coordinates, xa: Angle, xy: Angle) => 
      val b = a.offset(xa, xy)
      val decs = (10L to 20L).map { n => 
        Interpolate[Long, Coordinates].interpolate((10L, b), (20L, a), n).dec.toDegrees
      }
      val deltas = (decs, decs.tail).zipped.map(_ - _)
      (deltas, deltas.tail).zipped.map(_ ~= _).forall(identity)
    }

    "interpolate RA correctly" ! forAll { (a: Coordinates, xa: Angle, xy: Angle) => 
      val b = a.offset(xa, xy)
      val ras = (10L to 20L).map { n => 
        Interpolate[Long, Coordinates].interpolate((10L, b), (20L, a), n).ra.toAngle.toDegrees
      }
      val deltas = (ras, ras.tail).zipped.map((a, b) => a - b) 
      (deltas, deltas.tail).zipped.map(_ ~= _).distinct.length <= 2 // can cross 0 one time
    }

  }

}
