package edu.gemini.spModel.core

import scalaz._
import Scalaz._

// TODO: review optional parameters; remove if unused

/**
 * Specification of proper motion.
 * @param deltaRA angular velocity in right ascension per year
 * @param deltaDec angular velocity in declination per year
 * @param parallax parallax, if known
 * @param rv radial velocity, if known, in km/sec
 */
case class ProperMotion(
  deltaRA: RightAscensionAngularVelocity,
  deltaDec: DeclinationAngularVelocity,
  epoch: Epoch = Epoch.J2000,
  parallax: Option[Angle] = None,
  rv: Option[Double] = None)

object ProperMotion {
  val zero = ProperMotion(RightAscensionAngularVelocity.Zero, DeclinationAngularVelocity.Zero, Epoch.J2000, None, None)

  val deltaRA : ProperMotion @> RightAscensionAngularVelocity = Lens(t => Store(s => t.copy(deltaRA = s), t.deltaRA))
  val deltaDec: ProperMotion @> DeclinationAngularVelocity    = Lens(t => Store(s => t.copy(deltaDec = s), t.deltaDec))
}

