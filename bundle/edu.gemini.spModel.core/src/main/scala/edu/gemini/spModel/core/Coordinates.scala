package edu.gemini.spModel.core

import scala.math._
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

object Coordinates extends ((RightAscension, Declination) => Coordinates) {

  val ra:  Coordinates @> RightAscension = Lens(c => Store(r => c.copy(ra = r), c.ra))
  val dec: Coordinates @> Declination    = Lens(c => Store(d => c.copy(dec = d), c.dec))

  /**
   * The origin, at RA = Dec = 0. 
   * @group Constructors
   */
  val zero = Coordinates(RightAscension.zero, Declination.zero)

  /** @group Typeclass Instances */
  implicit val CoordinatesEqual: Equal[Coordinates] =
    Equal.equalA

  case class Difference(posAngle: Angle, distance: Angle) {
    /**
     * Gets the offset coordinates relative to the base position.
     */
    def offset: Offset = {
      val phi = posAngle.toRadians
      val h = distance.toDegrees
      // Position angle is east of north, or relative to 90 degrees.
      // Swapping sin and cos here to compensate.
      val p = Angle.fromDegrees(h * sin(phi))
      val q = Angle.fromDegrees(h * cos(phi))
      Offset(p, q)
    }

    override def toString =
      f"distance ${distance.toDegrees * 3600} arcsec, ${posAngle.toDegrees} deg E of N"
  }

  def difference(base: Coordinates, point: Coordinates): Difference = {
    val radian: Double = 180.0 / Math.PI
    val limitDistance = 0.0000004

    // coordinates transformed to radians
    val alf = point.ra.toAngle.toRadians
    val alf0 = base.ra.toAngle.toRadians
    val del = point.dec.toAngle.toRadians
    val del0 = base.dec.toAngle.toRadians

    val sd0 = Math.sin(del0)
    val sd = Math.sin(del)
    val cd0 = Math.cos(del0)
    val cd = Math.cos(del)
    val cosda = Math.cos(alf - alf0)
    val cosd = sd0 * sd + cd0 * cd * cosda
    val dist = {
      val acos = Math.acos(cosd)
      if (acos.isNaN) 0.0 else acos
    }

    val phi = if (dist > limitDistance) {
        val sind = Math.sin(dist)
        val pcospa = (sd * cd0 - cd * sd0 * cosda) / sind
        val cospa = {
          val abs = Math.abs(pcospa)
          if (abs > 1.0) pcospa / abs else pcospa
        }
        val sinpa = cd * Math.sin(alf - alf0) / sind
        val pphi = Math.acos(cospa)

        if (sinpa < 0.0) (Math.PI * 2) - pphi else pphi
      } else {
        0
      }
    Difference(Angle.fromDegrees(phi * radian), Angle.fromDegrees(dist * radian))
  }
}



