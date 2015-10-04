package edu.gemini.spModel.core

import scalaz._
import Scalaz._

/**
 * Specification of proper motion.
 * @param deltaRA angular velocity in right ascension per year
 * @param deltaDec angular velocity in declination per year
 */
case class ProperMotion(
  deltaRA: RightAscensionAngularVelocity,
  deltaDec: DeclinationAngularVelocity,
  epoch: Epoch = Epoch.J2000)

object ProperMotion {
  val zero = ProperMotion(RightAscensionAngularVelocity.Zero, DeclinationAngularVelocity.Zero, Epoch.J2000)

  val deltaRA : ProperMotion @> RightAscensionAngularVelocity = Lens(t => Store(s => t.copy(deltaRA = s), t.deltaRA))
  val deltaDec: ProperMotion @> DeclinationAngularVelocity    = Lens(t => Store(s => t.copy(deltaDec = s), t.deltaDec))
}
