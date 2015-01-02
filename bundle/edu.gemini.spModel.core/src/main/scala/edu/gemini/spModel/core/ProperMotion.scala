package edu.gemini.spModel.core

// TODO: review optional parameters; remove if unused

/**
 * Specification of proper motion.
 * @param deltaRA velocity in right ascension, mas/yr
 * @param deltaDec velocity in right ascension, mas/yr
 * @param parallax parallax, if known, in arcseconds
 * @param rv radial velocity, if known, in km/sec
 */
case class ProperMotion(
  deltaRA: Double,
  deltaDec: Double,
  epoch: Epoch = Epoch.J2000,
  parallax: Option[Double] = None,
  rv: Option[Double] = None)

object ProperMotion {

  val zero = ProperMotion(0, 0, Epoch.J2000, None, None)

}

