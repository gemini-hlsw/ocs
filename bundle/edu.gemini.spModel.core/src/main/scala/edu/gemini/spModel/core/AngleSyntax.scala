package edu.gemini.spModel.core

/**
 * Syntax for working with angle-like types.
 */
object AngleSyntax {
  implicit class ToDegreesOps[A](value: A)(implicit td: ToDegrees[A]) {
    def degrees: Double   = td.toDegrees(value)
    def arcmins: Double   = td.toArcmins(value)
    def arcsecs: Double   = td.toArcsecs(value)
    def mas: Double       = td.toMas(value)
    def hourAngle: Double = td.toHourAngle(value)
    def angle: Angle      = td.toAngle(value)
    def radians: Double   = td.toRadians(value)
  }

  implicit class FromDegreesOps(value: Double) {
    def degrees[A](implicit fd: FromDegrees[A]): A   = fd.fromDegrees(value)
    def arcmins[A](implicit fd: FromDegrees[A]): A   = fd.fromArcmins(value)
    def arcsecs[A](implicit fd: FromDegrees[A]): A   = fd.fromArcsecs(value)
    def mas[A](implicit fd: FromDegrees[A]): A       = fd.fromMas(value)
    def hourAngle[A](implicit fd: FromDegrees[A]): A = fd.fromHourAngle(value)
    def radians[A](implicit fd: FromDegrees[A]): A   = fd.fromRadians(value)
  }

  implicit class IsoAngleOps[A](value: A)(implicit ia: IsoAngle[A]) {
    def +(that: A): A        = ia.add(value, that)
    def -(that: A): A        = ia.subtract(value, that)
    def *(factor: Double): A = ia.multiply(value, factor)
    def flip: A              = ia.flip(value)
  }
}
