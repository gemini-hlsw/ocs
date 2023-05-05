package edu.gemini.spModel.core

import AngleSyntax._

import scala.math._

import scalaz._
import Scalaz._

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
    val pd = p.degrees - other.p.degrees
    val qd = q.degrees - other.q.degrees
    val d = sqrt(pd*pd + qd*qd)

    Angle.fromDegrees(d)
  }

  /**
   * Calculates the bearing from 0,0 as an Angle clockwise from north 0º.
   */
  def bearing: Angle = {
    val x = -p.toAngle.toSignedDegrees
    val y =  q.toAngle.toSignedDegrees
    Angle.fromRadians(atan2(x, y))
  }

  /**
   * Calculates the distance between the zero position and the offset
   * position
   *
   * @return angular separation between base position and offset position
   */
  def distance: Angle = distance(Offset.zero)

  def +(that: Offset): Offset = Offset(p + that.p, q + that.q)
  def -(that: Offset): Offset = Offset(p - that.p, q - that.q)

  def *(scale: Double): Offset = Offset(p * scale, q * scale)

  def flipP: Offset = Offset(p * -1, q)
  def flipQ: Offset = Offset(p, q * -1)
}

object Offset {
  val zero = Offset(OffsetP.Zero, OffsetQ.Zero)

  /** @group Typeclass Instances */
  implicit val equals = scalaz.Equal.equalA[Offset]

  /** @group Typeclass Instances. */
  implicit val MonoidOffset: Monoid[Offset] =
    new Monoid[Offset] {
      val zero = Offset.zero
      def append(a: Offset, b: => Offset): Offset = a + b
    }

  implicit val ShowOffset: Show[Offset] =
    Show.shows[Offset] { off => s"Offset(${off.p.shows}, ${off.q.shows})" }
}