package edu.gemini.spModel.core

import scalaz._, Scalaz._


/** A point in the sky, given right ascension and declination. */
final case class Coordinates(ra: RightAscension, dec: Declination) {

  /** 
   * Offset this `Coordinates` by the given deltas, flipping the RA by 180° if the Declination
   * overflows.
   * @group Operations
   */
  def offset(deltaRA: Angle, deltaDec: Angle): Coordinates = {
    val (dec0, carry) = dec.offset(deltaDec)
    val ra0 = ra.offset(if (carry) deltaRA.flip else deltaRA)
    Coordinates(ra0, dec0)
  }

  /** 
   * Compute the offset require to transform this `Coordinates` to the given one, such that
   * c1 offset (c1 diff c2) == c2.
   * @group Operations
   */
  def diff(c: Coordinates): (Angle, Angle) =
    (c.ra.toAngle - ra.toAngle, c.dec.toAngle - dec.toAngle)

}

object Coordinates {

  /** 
   * The origin, at RA = Dec = 0. 
   * @group Constructors
   */
  val zero = Coordinates(RightAscension.zero, Declination.zero)

  /** @group Typeclass Instances */
  implicit val CoordinatesEqual: Equal[Coordinates] =
    Equal.equalA

}



