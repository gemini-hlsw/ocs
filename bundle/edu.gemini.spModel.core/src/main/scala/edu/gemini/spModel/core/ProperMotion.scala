package edu.gemini.spModel.core

// TODO: review optional parameters; remove if unused

/**
 * Specification of proper motion.
 * @param deltaRA velocity in right ascension per year
 * @param deltaDec velocity in right ascension per year
 * @param parallax parallax, if known
 * @param rv radial velocity, if known, in km/sec
 */
case class ProperMotion(
  deltaRA: Angle,
  deltaDec: Angle,
  epoch: Epoch = Epoch.J2000,
  parallax: Option[Angle] = None,
  rv: Option[Double] = None)

object ProperMotion {
  val zero = ProperMotion(Angle.zero, Angle.zero, Epoch.J2000, None, None)
}

