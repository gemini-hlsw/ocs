package edu.gemini.spModel.core

/**
 * Syntax for working with angle-like types.
 */
object AngleSyntax {
  implicit class ToAngleOps[A](value: A) {
    def angle(implicit ta: ToDegrees[A]): Angle    = ta.toAngle(value)
    def arcsecs(implicit ta: ToDegrees[A]): Double = ta.toArcsecs(value)
    def arcmins(implicit ta: ToDegrees[A]): Double = ta.toArcmins(value)
    def degrees(implicit ta: ToDegrees[A]): Double = ta.toDegrees(value)

    // ....
  }

  implicit class FromAngleOps(value: Double) {
    def arcsecs[A](implicit fa: FromDegrees[A]): A = fa.fromArcsecs(value)
    def arcmins[A](implicit fa: FromDegrees[A]): A = fa.fromArcmins(value)
    def degrees[A](implicit fa: FromDegrees[A]): A = fa.fromDegrees(value)

    // ....
  }

  implicit class IsoAngleOps[A](value: A) {
    def +(that: A)(implicit ia: IsoAngle[A]): A =
      (value.degrees + that.degrees).degrees

    def -(that: A)(implicit ia: IsoAngle[A]): A =
      (value.degrees - that.degrees).degrees

    def *(factor: Double)(implicit ia: IsoAngle[A]): A =
      (value.degrees * factor).degrees

    def flip(implicit ia: IsoAngle[A]): A =
      value + 180.degrees[A]

    // ....
  }
}
