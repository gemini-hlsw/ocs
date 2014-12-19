package edu.gemini.spModel.core

import org.scalacheck.Prop._
import org.specs2.ScalaCheck
import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification
import scala.math._
import AlmostEqual.AlmostEqualOps

import scalaz._
import Scalaz._

class OffsetSpec extends Specification with ScalaCheck with Arbitraries {
  def beCloseTo(a: Angle, d: Double): Matcher[Angle] =
     beCloseTo(a.toDegrees +/- d) ^^ ((a:Angle) => a.toDegrees)

  "Offset" should {
    "calculate distance from base" in {
      val p = ~Angle.fromDMS(0 ,0, 3) // 3 Arcsec
      val q = ~Angle.fromDMS(0 ,0, 4) // 4 Arcsec

      val o = Offset(p, q)

      o.distance ~= ~Angle.fromDMS(0 ,0, 5)
    }
    "calculate relative distances" in {
      val p1 = ~Angle.fromDMS(0, 0, 6.0)
      val q1 = ~Angle.fromDMS(0, 0, 8.0)
      val o1 = Offset(p1, q1)

      val p2 = ~Angle.fromDMS(0, 0, 3.0)
      val q2 = ~Angle.fromDMS(0, 0, 4.0)
      val o2 = Offset(p2, q2)

      val d1 = o1.distance(o2)
      val d2 = o2.distance(o1)
      val expected = ~Angle.fromDMS(0, 0, 5.0)

      d1 ~= expected
      d2 ~= expected
    }
    "have a distance to itself that is always zero" in {
      forAll { (a: Angle) =>
        val o = Offset(a, a)
        o.distance(o) must beEqualTo(Angle.zero)
      }
    }
    "have distance to zero as sqrt of 2" in {
      forAll { (a: Angle) =>
        val o = Offset(a, a)
        o.distance ~= Angle.fromDegrees(sqrt(2)*a.toDegrees)
      }
    }
    "calculate a positive distance always" in {
      forAll { (a: Angle, b: Angle) =>
        val o = Offset(a, b)
        o.distance.toDegrees must beGreaterThanOrEqualTo(0.0)
      }
    }
    "be commutative" in {
      forAll { (a: Angle, b: Angle) =>
        val o1 = Offset(a, b)
        val o2 = Offset(b, a)
        o1.distance(o2) ~= o2.distance(o1)
      }
    }
  }
}
