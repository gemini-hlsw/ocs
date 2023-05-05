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

object CoordinatesSpec extends Specification with ScalaCheck with Arbitraries with Helpers {

  "Coordinates Offsetting" should {

    "be consistent with diff" !
      forAll { (a: Coordinates, b: Coordinates) =>
        val (dRA, dDec) = a diff b
        a.offset(dRA, dDec) ~= b
      }

    "flip the RA when Dec overflows" !
      forAll { (a: Coordinates, dDec: Angle) =>
        val ra0 = a.ra.toAngle
        val ra1 = a.offset(Angle.zero, dDec).ra.toAngle
        if (a.dec.offset(dDec)._2) ra1 ~= ra0.flip
        else                       ra1 ~= ra0
      }

  }

  "Coordinates Diff" should {

    "have identity (1)" !
      forAll { (a: Coordinates) =>
        val (dRA, dDec) = a diff a
        (dRA ~= Angle.zero) && (dDec ~= Angle.zero)
      }

    "have identity (2)" !
      forAll { (a: Coordinates) =>
        val (dRA, dDec) = Coordinates.zero diff a
        (dRA ~= a.ra.toAngle) && (dDec ~= a.dec.toAngle)
      }

  }

  "Coordinates Serialization" should {

    "Support Java Binary" !
      forAll { (coords: Coordinates) =>
        canSerialize(coords)
      }

  }

  "Coordinates Angular Offset" should {

    "result in an angular separation equal to distance, regardless of bearing" !
      forAll { (c: Coordinates, bearing: Angle, distance: Angle) =>
        val newCoordinates = c.angularOffset(bearing, distance)
        c.angularDistance(newCoordinates) ~= distance
      }

    "calculate offset in angular separation" ! {

      val o = Offset(OffsetP(Angle.fromArcmin(2.0)), OffsetQ(Angle.zero))

      val ra   = RightAscension.fromHours(12.0)
      val d0   = Declination.fromDegrees(0).get
      val d45  = Declination.fromDegrees(45).get
      val dm45 = Declination.fromDegrees(-45).get

      // At the equator there is no correction
      val c0 = Coordinates(ra, d0)
      val c1 = Coordinates(ra.offset(Angle.fromArcmin(2.0)), d0)
      c0.offset(o) ~= c1

      // Further away we correct by cos(dec)
      val c2 = Coordinates(ra, d45)
      val c3 = Coordinates(ra.offset(Angle.fromArcmin(2.0/Math.cos(45.0.toRadians))), d45)
      c2.offset(o) ~= c3

      // Further away we correct by cos(dec)
      val c4 = Coordinates(ra, dm45)
      val c5 = Coordinates(ra.offset(Angle.fromArcmin(2.0/Math.cos(-45.0.toRadians))), dm45)
      c4.offset(o) ~= c5
    }
  }

  "Coordinates Angular Separation" should {

    "be in [0, 180]" !
      forAll { (a: Coordinates, b: Coordinates) =>
        val deg = a.angularDistance(b).toDegrees
        deg >= 0 && deg <= 180
      }

    "be 0 for the same Coordinates" !
      forAll { (a: Coordinates) =>
        a.angularDistance(a) === Angle.zero
      }

    "be consistent with RA offsetting at the equator" !
      forAll { (ra: RA, da: Angle) =>
        val a = Coordinates(ra, Dec.zero)
        val b = a.offset(da, Angle.zero)
        val d = a.angularDistance(b)
        d.toDegrees ~= da.toSignedDegrees.abs
      }

    "be consistent with Dec offsetting (includes polar discontinuity)" !
      forAll { (a: Coordinates, da: Angle) =>
        val b = a.offset(Angle.zero, da)
        val d = a.angularDistance(b)
        d.toDegrees ~= da.toSignedDegrees.abs
      }

  }

  "Coordinates Interpolation" should {

    "be consistent with fractional angular separation" !
      forAll { (c1: Coordinates, c2: Coordinates) =>
        val sep = c1.angularDistance(c2)
        (-10 to 20 by 1).forall { f =>
          val stepSep = c1.interpolate(c2, f / 10.0).angularDistance(c1)
          stepSep.toDegrees ~= (sep * (f / 10.0)).toSignedDegrees.abs
        }
      }

  }

}
