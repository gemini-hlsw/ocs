package edu.gemini.spModel.core

/**
 * A type class that combines conversion and construction for convenience.
 */
trait IsoAngle[A] extends ToDegrees[A] with FromDegrees[A] {
  def add(a0: A, a1: A): A =
    fromDegrees(toDegrees(a0) + toDegrees(a1))

  def subtract(a0: A, a1: A): A =
    fromDegrees(toDegrees(a0) - toDegrees(a1))

  def multiply(a0: A, factor: Double): A =
    fromDegrees(toDegrees(a0) * factor)

  def flip(a: A): A =
    fromDegrees(toDegrees(a) + 180.0)
}
