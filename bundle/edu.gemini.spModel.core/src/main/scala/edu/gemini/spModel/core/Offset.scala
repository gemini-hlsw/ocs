package edu.gemini.spModel.core

import scala.math._

/**
 * Offset coordinates expressed as angular separations between two points, a
 * base position and a second position.
 */
case class Offset(p: OffsetP, q: OffsetQ) {
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
    import AngleSyntax._
    val pd = p.degrees - other.p.degrees
    val qd = q.degrees - other.q.degrees
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
  val zero = Offset(OffsetP.Zero, OffsetQ.Zero)

  /** @group Typeclass Instances */
  implicit val equals = scalaz.Equal.equalA[Offset]

}