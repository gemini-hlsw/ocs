package edu.gemini.spModel.core

trait AlmostEqual[A] {
  def almostEqual(a: A, b: A): Boolean
}

object AlmostEqual {

  implicit class AlmostEqualOps[A](a: A)(implicit A: AlmostEqual[A]) {
    def ~=(b: A): Boolean = A.almostEqual(a, b)
  }

  implicit val AngleAlmostEqual =
    new AlmostEqual[Angle] {
      def almostEqual(a: Angle, b: Angle) =
        (a.toDegrees - b.toDegrees).abs < 0.00001
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

}