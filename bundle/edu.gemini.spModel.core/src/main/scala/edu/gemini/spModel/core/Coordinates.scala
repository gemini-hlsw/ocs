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

  /**
   * Angular distance from `this` to `that`, always a positive angle in [0, 180].
   * Source: http://www.movable-type.co.uk/scripts/latlong.html
   */
  def angularDistance(that: Coordinates): Angle = {
    val φ1 = this.dec.toAngle.toRadians
    val φ2 = that.dec.toAngle.toRadians
    val Δφ = (that.dec.toAngle - this.dec.toAngle).toRadians
    val Δλ = (that.ra.toAngle  - this.ra.toAngle) .toRadians
    val a  = sin(Δφ / 2) * sin(Δφ / 2) +
             cos(φ1)     * cos(φ2)     *
             sin(Δλ / 2) * sin(Δλ / 2)
    Angle.fromRadians(2 * atan2(sqrt(a), sqrt(1 - a)))
  }

  /**
   * Interpolate between `this` and `that` at a position specified by `f`, where `0.0` is `this`,
   * `1.0` is `other`, and `0.5` is halfway along the great circle connecting them. Note that this
   * computation is undefined where `f` is `NaN` or `Infinity`.
   * Source: http://www.movable-type.co.uk/scripts/latlong.html
   */
  def interpolate(that: Coordinates, f: Double): Coordinates = {
    val δ = angularDistance(that).toRadians
    if (δ == 0) this
    else {
      val φ1 = this.dec.toAngle.toRadians
      val φ2 = that.dec.toAngle.toRadians
      val λ1 = this.ra.toAngle.toRadians
      val λ2 = that.ra.toAngle.toRadians
      val a = sin((1 - f) * δ) / sin(δ) // n.b. this line is wrong on the web page
      val b = sin(f * δ) / sin(δ)
      val x = a * cos(φ1) * cos(λ1) + b * cos(φ2) * cos(λ2)
      val y = a * cos(φ1) * sin(λ1) + b * cos(φ2) * sin(λ2)
      val z = a * sin(φ1) + b * sin(φ2)
      val φi = atan2(z, sqrt(x * x + y * y))
      val λi = atan2(y, x)
      Coordinates(RA.fromAngle(Angle.fromRadians(λi)), Dec.fromAngle(Angle.fromRadians(φi)).get)
    }
  }
}

object Coordinates extends ((RightAscension, Declination) => Coordinates) {

  val ra:  Coordinates @> RightAscension = Lens(c => Store(r => c.copy(ra = r), c.ra))
  val dec: Coordinates @> Declination    = Lens(c => Store(d => c.copy(dec = d), c.dec))

  /** Construct a `Coordinates` from values in degrees, where `dec` is in [-90, 90). */
  def fromDegrees(ra: Double, dec: Double): Option[Coordinates] =
    Dec.fromAngle(Angle.fromDegrees(dec)).map(Coordinates(RA.fromDegrees(ra), _))

  /**
   * The origin, at RA = Dec = 0.
   * @group Constructors
   */
  val zero = Coordinates(RightAscension.zero, Declination.zero)

  /** @group Typeclass Instances */
  implicit val CoordinatesEqual: Equal[Coordinates] =
    Equal.equalA

  /** @group Typeclass Instances. */
  implicit val ShowCoordinates: Show[Coordinates] =
    Show.shows { c =>
      s"(${Angle.formatHMS(c.ra.toAngle)}, ${Declination.formatDMS(c.dec)})"
    }

  case class Difference(posAngle: Angle, distance: Angle) {
    /**
     * Gets the offset coordinates relative to the base position.
     */
    def offset: Offset = {
      val phi = posAngle.toRadians
      val h = distance.toDegrees
      // Position angle is east of north, or relative to 90 degrees.c
      // Swapping sin and cos here to compensate.
      import AngleSyntax._
      val p = (h * sin(phi)).degrees[OffsetP]
      val q = (h * cos(phi)).degrees[OffsetQ]
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



