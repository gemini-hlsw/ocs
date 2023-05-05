package edu.gemini.spModel.core

import org.scalacheck.Prop._
import org.specs2.ScalaCheck
import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification
import scala.math._
import AlmostEqual.AlmostEqualOps
import AngleSyntax._

import scalaz._
import Scalaz._

class OffsetSpec extends Specification with ScalaCheck with Arbitraries {
  "Offset" should {
    "calculate distance from base" in {
      val p = 3.arcsecs[OffsetP]
      val q = 4.arcsecs[OffsetQ]

      val o = Offset(p, q)

      o.distance ~= ~Angle.fromDMS(0 ,0, 5)
    }

    "calculate relative distances" in {
      val p1 = 6.arcsecs[OffsetP]
      val q1 = 8.arcsecs[OffsetQ]
      val o1 = Offset(p1, q1)

      val p2 = 3.arcsecs[OffsetP]
      val q2 = 4.arcsecs[OffsetQ]
      val o2 = Offset(p2, q2)

      val d1 = o1.distance(o2)
      val d2 = o2.distance(o1)
      val expected = ~Angle.fromDMS(0, 0, 5.0)

      d1 ~= expected
      d2 ~= expected
    }

    "have a distance to itself that is always zero" in {
      forAll { (a: Angle) =>
        val o = Offset(OffsetP(a), OffsetQ(a))
        o.distance(o) must beEqualTo(Angle.zero)
      }
    }

    "have distance to zero as sqrt of 2" in {
      forAll { (a: Angle) =>
        val deg = Angle.signedDegrees(a.toDegrees)
        val o = Offset(deg.degrees[OffsetP], deg.degrees[OffsetQ])
        o.distance.toDegrees ~= sqrt(2)*deg.abs
      }
    }

    "calculate a positive distance always" in {
      forAll { (a: Angle, b: Angle) =>
        val o = Offset(OffsetP(a), OffsetQ(b))
        o.distance.toDegrees must beGreaterThanOrEqualTo(0.0)
      }
    }

    "be commutative" in {
      forAll { (a: Angle, b: Angle) =>
        val o1 = Offset(OffsetP(a), OffsetQ(b))
        val o2 = Offset(OffsetP(b), OffsetQ(a))
        o1.distance(o2) ~= o2.distance(o1)
      }
    }

    "calculate bearing" in {
      val tests = List(
        ( 0,  1,   0),
        ( 1,  1, 315),
        ( 1,  0, 270),
        ( 1, -1, 225),
        ( 0, -1, 180),
        (-1, -1, 135),
        (-1,  0,  90),
        (-1,  1,  45)
      )

      tests.forall { case (p, q, b) =>
        val p0 = p.arcsecs[OffsetP]
        val q0 = q.arcsecs[OffsetQ]
        val o  = Offset(p0, q0)
        val e  = ~Angle.fromDMS(b, 0, 0)
        o.bearing ~= e
      }
    }
  }
}
