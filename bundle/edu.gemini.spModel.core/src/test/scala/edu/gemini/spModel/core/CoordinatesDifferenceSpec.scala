package edu.gemini.spModel.core

import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification

import scala.math._

import scalaz._
import Scalaz._

class CoordinatesDifferenceSpec extends Specification {

  def beCloseDifference(cd: Coordinates.Difference, d: Double): Matcher[Coordinates.Difference] =
     beCloseTo(cd.distance.toDegrees +/- d) ^^ ((c:Coordinates.Difference) => c.distance.toDegrees) and beCloseTo(cd.posAngle.toDegrees +/- d) ^^ ((c:Coordinates.Difference) => c.posAngle.toDegrees)

  def beCloseOffset(p: Angle, q: Angle, d: Double): Matcher[Offset] =
     beCloseTo(p.toDegrees +/- d) ^^ ((o:Offset) => o.p.toDegrees) and beCloseTo(q.toDegrees +/- d) ^^ ((o:Offset) => o.q.toDegrees)

  def beCloseToEquator(c: Coordinates, a: Angle, delta:Double): Matcher[Coordinates.Difference] = {
    def normalize(a: Angle): Angle = if (a.toDegrees > 180) Angle.zero - a else a
    val normalizedDec = normalize(c.dec.toAngle)
    val normalizedRa = normalize(c.ra.toAngle)

    // The distance is close enough to a straightforward calculation at the
    // equator and prime meridian. Calculated in arcsec
    val dist = sqrt(pow(normalizedRa.toDegrees / 3600, 2) + pow(normalizedDec.toDegrees / 3600, 2))

    beCloseDifference(Coordinates.Difference(a, Angle.fromDegrees(dist)), delta)
  }

  "CoordinatesDiff" should {
    val errorDelta = 0.01

    "calculate on Z0" in {
      val base = Coordinates(RightAscension.fromDegrees(0), Declination.fromAngle(Angle.fromDegrees(90)).getOrElse(Declination.zero))
      val distance = ~Angle.fromDMS(0, 0, 10)
      val dec = Declination.fromAngle(Angle.fromDegrees(90) - distance).getOrElse(Declination.zero)

      for {
        i   <- 0 to 4
        a    = Angle.fromDegrees(45.0 * i)
        c    = Coordinates(RightAscension.fromAngle(a), dec)
        ref  = Angle.fromDegrees(180) - a
      } yield Coordinates.difference(base, c) should beCloseDifference(Coordinates.Difference(ref, distance), errorDelta)
    }
    "calculate offsets on Z0" in {
      val base = Coordinates(RightAscension.fromDegrees(0), Declination.fromAngle(Angle.fromDegrees(90)).getOrElse(Declination.zero))
      val dec = Declination.fromAngle(Angle.fromDegrees(90) - ~Angle.fromDMS(0, 0, 10)).getOrElse(Declination.zero)

      val results = List((0.0, -10.0), (7.071068, -7.071068), (10.0, 0.0), (7.071068, 7.071068), (0.0, 10.0))
      for {
        i     <- 0 to 4
        r      = results(i)
        a      = Angle.fromDegrees(45.0 * i)
        c      = Coordinates(RightAscension.fromAngle(a), dec)
        offset = Coordinates.difference(base, c).offset
      } yield offset should beCloseOffset(Angle.fromDegrees(r._1 / 3600), Angle.fromDegrees(r._2 / 3600), errorDelta)

    }
    "calculate difference close to the equator" in {
      val base = Coordinates.zero

      def coordinatesTest(ra: Angle, dec: Angle, qa0: Angle) = {
        val q0 = Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
        Coordinates.difference(base, q0) should beCloseToEquator(q0, qa0, errorDelta)

        val q1 = Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(Angle.zero - dec).getOrElse(Declination.zero))
        val qa1 = Angle.fromDegrees(180) - qa0
        Coordinates.difference(base, q1) should beCloseToEquator(q1, qa1, errorDelta)

        val q2 = Coordinates(RightAscension.fromAngle(Angle.zero - ra), Declination.fromAngle(Angle.zero - dec).getOrElse(Declination.zero))
        val qa2 = Angle.fromDegrees(180) + qa0
        Coordinates.difference(base, q2) should beCloseToEquator(q2, qa2, errorDelta)

        val q3 = Coordinates(RightAscension.fromAngle(Angle.zero - ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
        val qa3 = Angle.fromDegrees(360) - qa0
        Coordinates.difference(base, q3) should beCloseToEquator(q3, qa3, errorDelta)
      }

      coordinatesTest(Angle.fromDegrees(10.0 / 3600), Angle.fromDegrees(10.0 / 3600), Angle.fromDegrees(45))
      coordinatesTest(Angle.fromDegrees(10*0.5 / 3600), Angle.fromDegrees(10.0 * sqrt(3)/ 2.0 / 3600), Angle.fromDegrees(30))

    }
    "calculate offsets close to the equator" in {
      val base = Coordinates.zero

      def offsetsTest(ra: Angle, dec: Angle) = {
        val q0 = Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
        Coordinates.difference(base, q0).offset should beCloseOffset(ra, dec, errorDelta)

        val q1 = Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(Angle.zero - dec).getOrElse(Declination.zero))
        Coordinates.difference(base, q1).offset should beCloseOffset(ra, dec * -1, errorDelta)

        val q2 = Coordinates(RightAscension.fromAngle(Angle.zero - ra), Declination.fromAngle(Angle.zero - dec).getOrElse(Declination.zero))
        Coordinates.difference(base, q2).offset should beCloseOffset(ra * -1, dec * -1, errorDelta)

        val q3 = Coordinates(RightAscension.fromAngle(Angle.zero - ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
        Coordinates.difference(base, q3).offset should beCloseOffset(ra * -1, dec, errorDelta)
      }

      offsetsTest(Angle.fromDegrees(10.0 / 3600), Angle.fromDegrees(10.0 / 3600))
      offsetsTest(Angle.fromDegrees(10*0.5 / 3600), Angle.fromDegrees(10.0 * sqrt(3)/2.0 / 3600))
    }

  }
}
