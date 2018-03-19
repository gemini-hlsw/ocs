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
  val epoch:    ProperMotion @> Epoch                         = Lens(t => Store(s => t.copy(epoch = s), t.epoch))

  // Warning: This assumes the same epoch across proper motion.
  // We use it in GhostAsterism to simplify, where we assume same epoch.
  implicit val monoid: Monoid[ProperMotion] = Monoid.instance(
    (pm1,pm2) => ProperMotion(pm1.deltaRA |+| pm2.deltaRA, pm1.deltaDec |+| pm2.deltaDec),
    zero)
}
