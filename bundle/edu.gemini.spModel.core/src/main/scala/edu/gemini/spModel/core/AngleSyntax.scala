package edu.gemini.spModel.core

/**
 * Syntax for working with angle-like types.
 */
object AngleSyntax {
  implicit class ToDegreesOps[A](value: A)(implicit td: ToDegrees[A]) {
    def angle: Angle    = td.toAngle(value)
    def arcsecs: Double = td.toArcsecs(value)
    def arcmins: Double = td.toArcmins(value)
    def degrees: Double = td.toDegrees(value)

    // ....
  }

  implicit class FromDegreesOps(value: Double) {
    def arcsecs[A](implicit fa: FromDegrees[A]): A = fa.fromArcsecs(value)
    def arcmins[A](implicit fa: FromDegrees[A]): A = fa.fromArcmins(value)
    def degrees[A](implicit fa: FromDegrees[A]): A = fa.fromDegrees(value)

    // ....
  }

  implicit class IsoAngleOps[A](value: A)(implicit ia: IsoAngle[A]) {
    def +(that: A): A        = ia.add(value, that)
    def -(that: A): A        = ia.subtract(value, that)
    def *(factor: Double): A = ia.multiply(value, factor)
    def flip: A              = ia.flip(value)

    // ....
  }
}
