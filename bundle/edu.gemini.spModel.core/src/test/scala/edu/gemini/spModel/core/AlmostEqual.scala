package edu.gemini.spModel.core

import scalaz._, Scalaz._

trait AlmostEqual[A] {
  def almostEqual(a: A, b: A): Boolean
}

object AlmostEqual {

  implicit class AlmostEqualOps[A](a: A)(implicit A: AlmostEqual[A]) {
    def ~=(b: A): Boolean = A.almostEqual(a, b)
  }

  // almost equal if both None, or both Some and values are almost equal
  implicit def AlmostEqualOption[A: AlmostEqual]: AlmostEqual[Option[A]] =
    new AlmostEqual[Option[A]] {
      def almostEqual(a: Option[A], b: Option[A]) =
        (a |@| b)(_ ~= _).getOrElse(true)
    }

  implicit val DoubleAlmostEqual =
    new AlmostEqual[Double] {
      def almostEqual(a: Double, b: Double) =
        (a - b).abs < 0.00001
    }

  implicit val AngleAlmostEqual =
    new AlmostEqual[Angle] {
      def almostEqual(a: Angle, b: Angle) =
        a.toDegrees ~= b.toDegrees
    }

  implicit val RightAscensionAngularVelocityAlmostEqual =
    new AlmostEqual[RightAscensionAngularVelocity] {
      def almostEqual(a: RightAscensionAngularVelocity, b: RightAscensionAngularVelocity) =
        a.velocity.masPerYear ~= b.velocity.masPerYear
    }

  implicit val DeclinationAngularVelocityAlmostEqual =
    new AlmostEqual[DeclinationAngularVelocity] {
      def almostEqual(a: DeclinationAngularVelocity, b: DeclinationAngularVelocity) =
        a.velocity.masPerYear ~= b.velocity.masPerYear
    }

  implicit val ProperMotionAlmostEqual =
    new AlmostEqual[ProperMotion] {
      def almostEqual(a: ProperMotion, b: ProperMotion) =
        (a.deltaRA ~=  b.deltaRA) && (a.deltaDec ~= b.deltaDec)
    }

  implicit val RightAscensionAlmostEqual =
    new AlmostEqual[RightAscension] {
      def almostEqual(a: RightAscension, b: RightAscension) =
        a.toAngle ~= b.toAngle
    }

  implicit val DeclinationAlmostEqual =
    new AlmostEqual[Declination] {
      def almostEqual(a: Declination, b: Declination) =
        a.toAngle ~= b.toAngle
    }

  implicit val CoordinatesAlmostEqual =
    new AlmostEqual[Coordinates] {
      def almostEqual(a: Coordinates, b: Coordinates) =
        (a.ra ~= b.ra) && (a.dec ~= b.dec)
    }

  implicit val WavelengthAlmostEqual =
    new AlmostEqual[Wavelength] {
      def almostEqual(a: Wavelength, b: Wavelength) =
        a.toNanometers ~= b.toNanometers
    }
}