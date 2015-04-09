package edu.gemini.spModel.core

/**
 * A type class for conversions from angle-like types.
 *
 * The base type for conversions is degrees as a Double, which must be supplied.
 * All other conversions are derived from degrees.
 */
trait ToDegrees[A] {

  def toDegrees(a: A): Double

  def toArcmins(a: A): Double   = toDegrees(a) *      60.0

  def toArcsecs(a: A): Double   = toDegrees(a) *    3600.0

  def toMas(a: A): Double       = toDegrees(a) * 3600000.0

  def toHourAngle(a: A): Double = toDegrees(a) /      15.0

  def toAngle(a: A): Angle      = Angle.fromDegrees(toDegrees(a))

  def toRadians(a: A): Double   = toDegrees(a).toRadians
}
