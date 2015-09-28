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
      x ~= Some(a.toDouble)
    }

    "be invariant at max" ! forAll { (a: Int, b: Int) =>
      val x = Interpolate[Long, Double].interpolate((10L, a.toDouble), (20L, b.toDouble), 20L)
      x ~= Some(b.toDouble)
    }

    "work off the end" ! forAll { (a: Int, b: Int) =>
      val x = Interpolate[Long, Double].interpolate((10L, a.toDouble), (20L, b.toDouble), 30L)
      x ~= Some(b.toDouble + (b.toDouble - a.toDouble))
    }

    "interoplate a constant value for constaint domain" ! forAll { (a: Int) =>
      (0 to 30).map { n => 
        Interpolate[Long, Double].interpolate((10L, a.toDouble), (20L, a.toDouble), n)
      }.forall(_ == Some(a))
    }

    "yield None in the degenerate case" ! forAll { (a: Int, b: Int) =>
      (a != b) ==> {
        val x = Interpolate[Long, Double].interpolate((10L, a.toDouble), (10L, b.toDouble), 10L)
        x == None
      }
    }

  }

  "Interpolate[Coordinates]" should {

    "be consistent with Coordinates.interpolate" ! forAll { (a: Coordinates, b: Coordinates, n1: Short, n2: Short, c: Short) => 
      val f =  ((c.toDouble - n1.toDouble) / (n2.toDouble - n1.toDouble))
      val c1 = Interpolate[Long, Coordinates].interpolate((n1.toLong, a), (n2.toLong, b), c.toLong)
      val c2 = Some(f).filterNot(f => f.isInfinity || f.isNaN).map(a.interpolate(b, _))
      c1 ~= c2
    }

  }

}
