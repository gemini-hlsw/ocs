package edu.gemini.spModel.core

import scala.math._

/**
 * Offset coordinates expressed as angular separations between two points, a
 * base position and a second position.
 */
case class Offset(p: Angle, q: Angle) {
  /**
   * Calculates the absolute distance between this Offset position and the
   * given position
   *
   * @param other distance computed is relative to the given offset
   *
   * @return distance, expressed as an Angle, between
   *         this offset and the given <code>other</code> offset
   */
  def distance(other: Offset): Angle = {
    val pd = p.toDegrees - other.p.toDegrees
    val qd = q.toDegrees - other.q.toDegrees
    val d = sqrt(pd*pd + qd*qd)

    Angle.fromDegrees(d)
  }

  /**
   * Calculates the distance between the zero position and the offset
   * position
   *
   * @return angular separation between base position and offset position
   */
  def distance: Angle = distance(Offset.zero)
}

object Offset {
  val zero = Offset(Angle.zero, Angle.zero)
}