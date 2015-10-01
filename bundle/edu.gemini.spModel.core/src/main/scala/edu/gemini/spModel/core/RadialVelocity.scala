package edu.gemini.spModel.core

import squants.motion.{KilometersPerSecond, Velocity}

import scalaz._
import Scalaz._

/**
 * Specification of Radial velocity
 * @param velocity in km/s
 */
case class RadialVelocity(velocity: Velocity)

object RadialVelocity {
  /**
   * The `No velocity`
   * @group Constructors
   */
  val zero: RadialVelocity = RadialVelocity(KilometersPerSecond(0))

  /** @group Typeclass Instances */
  implicit val order: Order[RadialVelocity] =
    Order.orderBy(_.velocity)

  /** @group Typeclass Instances */
  implicit val ordering: scala.Ordering[RadialVelocity] =
    scala.Ordering.by(_.velocity)

  /**
   * Additive monoid for `RadialVelocity`
   * @group Typeclass Instances
   */
  implicit val monoid: Monoid[RadialVelocity] =
    new Monoid[RadialVelocity] {
      val zero = RadialVelocity.zero
      def append(a: RadialVelocity, b: => RadialVelocity): RadialVelocity = RadialVelocity(a.velocity + b.velocity)
    }
}
