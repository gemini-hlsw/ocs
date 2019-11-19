package edu.gemini.spModel.core

import java.time.Instant

import scalaz._
import Scalaz._

import scala.math.{cos, sin}

/**
 * Specification of proper motion.
 * @param deltaRA angular velocity in right ascension per year
 * @param deltaDec angular velocity in declination per year
 */
case class ProperMotion(
  deltaRA: RightAscensionAngularVelocity,
  deltaDec: DeclinationAngularVelocity,
  epoch: Epoch = Epoch.J2000) {

  /** Coordinates at the current time. */
  def calculateAt(t: Target, i: Instant): Coordinates =
    plusYears(t, epoch.untilInstant(i))

  /** Coordinates `elapsedYears` fractional epoch-years after `epoch`. */
  def plusYears(t: Target, elapsedYears: Double): Coordinates = {
    // We want the observed coordinates at the epoch in radians: what to do if zero?
    val baseCoordinates = t.coords(None).getOrElse(Coordinates.zero)
    // TODO: Is this calculated correctly?
    val properVelocity: Offset = Offset(OffsetP(Angle.fromDegrees(deltaRA.velocity.toDegreesPerYear)),
                                        OffsetQ(Angle.fromDegrees(deltaDec.velocity.toDegreesPerYear)))

    val radialVelocity: Double = {
      val redshift: Option[Redshift] = Target.redshift.get(t).flatten
      redshift.getOrElse(Redshift.zero).toRadialVelocity.toKilometersPerSecond
    }
    val parallax = Parallax.mas.get(Target.parallax.get(t).flatten.getOrElse(Parallax.zero)) / 1000000.0
    ProperMotion.properMotionCalculator(baseCoordinates,
                                        epoch.scheme.lengthOfYear,
                                        properVelocity,
                                        radialVelocity,
                                        parallax,
                                        elapsedYears)
  }
}

object ProperMotion {
  // Some constants we need
  val AstronomicalUnit:    Long   = 149597870660L
  private val secsPerDay:  Double = 86400.0
  private val auPerKm:     Double = 1000.0 / AstronomicalUnit.toDouble
  private val radsPerAsec: Double = Angle.fromArcsecs(1).toArcsecs

  private type Vec2 = (Double, Double)
  private type Vec3 = (Double, Double, Double)

  /**
   * Calculator to determine the new coordinates due to proper motion from epoch after elapsed years.
   * @param baseCoordinates base coordinates of the target
   * @param daysPerYear     length of epoch year in fractional days
   * @param properVelocity  proper velocity per epoch year
   * @param radialVelocity  radial velocity (km / s, positive if receding)
   * @param parallax        parallax
   * @param elapsedYears    elapsed time in epoch years
   * @return
   */
  def properMotionCalculator(baseCoordinates: Coordinates,
                             daysPerYear: Double,
                             properVelocity: Offset,
                             radialVelocity: Double,
                             parallax: Double,
                             elapsedYears: Double): Coordinates = {
    // We want the baseCoordinates in radians.
    val (ra,  dec)  = (baseCoordinates.ra.toAngle.toRadians, baseCoordinates.dec.toAngle.toRadians)
    val (dRa, dDec) = (properVelocity.p.toAngle.toRadians,   properVelocity.q.toAngle.toRadians)

    val pos: Vec3 = {
      val cd = cos(dec)
      (cos(ra) * cd, sin(ra) * cd, sin(dec))
    }

    // Change per year due to radial velocity and parallax. The units work out to asec/y.
    val dPos1: Vec3 =
        pos            *
        daysPerYear    *
        secsPerDay     *
        radsPerAsec    *
        auPerKm        *
        radialVelocity *
        parallax

    // Change per year due to proper velocity
    val dPos2 = (
      -dRa * pos._2 - dDec * cos(ra) * sin(dec),
      dRa * pos._1 - dDec * sin(ra) * sin(dec),
      dDec *           cos(dec)
    )
  }

  val zero = ProperMotion(RightAscensionAngularVelocity.Zero, DeclinationAngularVelocity.Zero, Epoch.J2000)

  val deltaRA : ProperMotion @> RightAscensionAngularVelocity = Lens(t => Store(s => t.copy(deltaRA = s), t.deltaRA))
  val deltaDec: ProperMotion @> DeclinationAngularVelocity    = Lens(t => Store(s => t.copy(deltaDec = s), t.deltaDec))
  val epoch:    ProperMotion @> Epoch                         = Lens(t => Store(s => t.copy(epoch = s), t.epoch))

  // Warning: This assumes the same epoch across proper motion, which we take to be J2000.
  // We use it in GhostAsterism to simplify, where we assume same epoch.
  implicit val monoid: Monoid[ProperMotion] = Monoid.instance(
    (pm1,pm2) => ProperMotion(pm1.deltaRA |+| pm2.deltaRA, pm1.deltaDec |+| pm2.deltaDec),
    zero)
}
