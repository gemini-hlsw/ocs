package edu.gemini.spModel.core

/**
 * A type class for conversions to angle-like types, i.e., constructors.
 *
 * The base type for construction is degrees as a Double which must be supplied.
 * All other constructors are derived from degrees.
 */
trait FromDegrees[A] {
  def fromDegrees(d: Double): A

  def fromAngle(a: Angle): A    = fromDegrees(a.toDegrees)

  def fromArcsecs(s: Double): A = fromDegrees(s/3600.0)

  def fromArcmins(m: Double): A = fromDegrees(m/60.0)

  // etc. etc. ....
}
