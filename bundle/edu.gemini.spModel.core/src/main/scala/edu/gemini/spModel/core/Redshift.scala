package edu.gemini.spModel.core

import squants.motion.{KilometersPerSecond, Velocity}

import scalaz.{Monoid, Order}

/**
 * Specification of Radial velocity
 * TODO Cannot be a value class as it breaks java compatibility, convert to value class when the legacy target model disappears
 * @param redshift dimensionless measurement of redshift
 */
case class Redshift(redshift: Double) {
  def toRadialVelocity: Velocity = Redshift.C * (((redshift + 1)*(redshift + 1) - 1)/((redshift + 1)*(redshift + 1) + 1))
  def toApparentRadialVelocity: Velocity = Redshift.C * redshift
}

object Redshift {
  def instance = this

  val C:Velocity = KilometersPerSecond(299792.458) // Speed of light in km/s

  def fromRadialVelocity(v: Velocity):Redshift = {
    val t = (1 + v / C) / (1 - v / C)
    Redshift(scala.math.sqrt(t) - 1)
  }

  def fromApparentRadialVelocity(v: Velocity):Redshift =
    Redshift(v / C)

  /**
   * The `No redshift`
   * @group Constructors
   */
  val zero: Redshift = Redshift(0)

  /** @group Typeclass Instances */
  implicit val order: Order[Redshift] =
    Order.orderBy(_.redshift)

  /** @group Typeclass Instances */
  implicit val ordering: scala.Ordering[Redshift] =
    scala.Ordering.by(_.redshift)

  /**
   * Additive monoid for `Redshift`
   * @group Typeclass Instances
   */
  implicit val monoid: Monoid[Redshift] =
    new Monoid[Redshift] {
      val zero = Redshift.zero
      def append(a: Redshift, b: => Redshift): Redshift = Redshift(a.redshift + b.redshift)
    }

}