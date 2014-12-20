package edu.gemini.spModel.core

import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification
import scala.math._

class CoordinatesDifferenceSpec extends Specification {

  def beCloseDifference(c: CoordinatesDifference, d: Double): Matcher[CoordinatesDifference] =
     beCloseTo(c.distanceArcsec.toDegrees +/- d) ^^ ((c:CoordinatesDifference) => c.distanceArcsec.toDegrees) and beCloseTo(c.posAngleDec.toDegrees +/- d) ^^ ((c:CoordinatesDifference) => c.posAngleDec.toDegrees)

  def beCloseToEquator(c: Coordinates, a: Angle, delta:Double): Matcher[CoordinatesDifference] = {

    def beCloseInQuadrant(c: Coordinates): Matcher[CoordinatesDifference] = {
      def normalize(a: Angle): Angle = if (a.toDegrees > 180) Angle.zero - a else a
      val normalizedDec = normalize(c.dec.toAngle)
      val normalizedRa = normalize(c.ra.toAngle)

      // The distance is close enough to a straightforward calculation at the
      // equator and prime meridian.
      val dist = sqrt(pow(normalizedRa.toDMS.seconds, 2) + pow(normalizedDec.toDMS.seconds, 2))

      beCloseDifference(CoordinatesDifference(a, Angle.fromDegrees(dist)), delta)
    }
    beCloseInQuadrant(c)
  }

  "CoordinatesDiff" should {
    val errorDelta = 0.01

    "calculate on Z0" in {
      val base = Coordinates(RightAscension.fromDegrees(0), Declination.fromAngle(Angle.fromDegrees(90)).getOrElse(Declination.zero))
      val dec = Declination.fromAngle(Angle.fromDegrees(90) - Angle.fromDMS(0, 0, 10).getOrElse(Angle.zero)).getOrElse(Declination.zero)

      CoordinatesDifference.difference(base, Coordinates(RightAscension.fromAngle(Angle.fromDegrees(0.0)), dec)) should beCloseDifference(CoordinatesDifference(Angle.fromDegrees(180.0), Angle.fromDegrees(10)), errorDelta)
      CoordinatesDifference.difference(base, Coordinates(RightAscension.fromAngle(Angle.fromDegrees(45.0)), dec)) should beCloseDifference(CoordinatesDifference(Angle.fromDegrees(135.0), Angle.fromDegrees(10)), errorDelta)
      CoordinatesDifference.difference(base, Coordinates(RightAscension.fromAngle(Angle.fromDegrees(90.0)), dec)) should beCloseDifference(CoordinatesDifference(Angle.fromDegrees(90.0), Angle.fromDegrees(10)), errorDelta)
      CoordinatesDifference.difference(base, Coordinates(RightAscension.fromAngle(Angle.fromDegrees(135.0)), dec)) should beCloseDifference(CoordinatesDifference(Angle.fromDegrees(45.0), Angle.fromDegrees(10)), errorDelta)
      CoordinatesDifference.difference(base, Coordinates(RightAscension.fromAngle(Angle.fromDegrees(180.0)), dec)) should beCloseDifference(CoordinatesDifference(Angle.fromDegrees(0.0), Angle.fromDegrees(10)), errorDelta)
    }
    "calculate on equator" in {
      val base = Coordinates.zero

      def coordinatesTest(ra: Angle, dec: Angle, qa0: Angle) = {
        val q0 = Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
        CoordinatesDifference.difference(base, q0) should beCloseToEquator(q0, qa0, errorDelta)

        val q1 = Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(Angle.zero - dec).getOrElse(Declination.zero))
        val qa2 = Angle.fromDegrees(180) - qa0
        CoordinatesDifference.difference(base, q1) should beCloseToEquator(q1, qa2, errorDelta)

        val q3 = Coordinates(RightAscension.fromAngle(Angle.zero - ra), Declination.fromAngle(Angle.zero - dec).getOrElse(Declination.zero))
        val qa3 = Angle.fromDegrees(180) + qa0
        CoordinatesDifference.difference(base, q3) should beCloseToEquator(q3, qa3, errorDelta)

        val q4 = Coordinates(RightAscension.fromAngle(Angle.zero - ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
        val qa4 = Angle.fromDegrees(360) - qa0
        CoordinatesDifference.difference(base, q4) should beCloseToEquator(q4, qa4, errorDelta)
      }

      coordinatesTest(Angle.fromDMS(0, 0, 10).getOrElse(Angle.zero), Angle.fromDMS(0, 0, 10).getOrElse(Angle.zero), Angle.fromDegrees(45))
      coordinatesTest(Angle.fromDMS(0, 0, 10*0.5).getOrElse(Angle.zero), Angle.fromDMS(0, 0, 10.0 * sqrt(3)/2.0).getOrElse(Angle.zero), Angle.fromDegrees(30))

    }

  }
}
