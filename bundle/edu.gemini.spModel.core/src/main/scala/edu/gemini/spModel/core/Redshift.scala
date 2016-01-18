package edu.gemini.spModel.core

import squants.motion.{KilometersPerSecond, Velocity}

import scalaz.{Monoid, Order}
import scalaz.std.anyVal._

/**
 * Specification of Radial velocity
 * TODO Cannot be a value class as it breaks java compatibility, convert to value class when the legacy target model disappears
 * @param z dimensionless measurement of redshift
 */
case class Redshift(z: Double) extends Serializable {
  def toRadialVelocity: Velocity = Redshift.C * (((z + 1)*(z + 1) - 1)/((z + 1)*(z + 1) + 1))
  def toApparentRadialVelocity: Velocity = Redshift.C * z
}

object Redshift {
  def instance = this

  val C:Velocity = KilometersPerSecond(299792.458) // Speed of light in km/s

  def fromRadialVelocity(v: Velocity):Redshift = {
    val t = (1 + v / C) / (1 - v / C)
    Redshift(scala.math.sqrt(t) - 1)
  }

  /**
    * Make a new redshift assuming the value is in km/sec
    */
  def fromRadialVelocityJava(v: Double):Redshift = {
    val t = (1 + v / C.toKilometersPerSecond) / (1 - v / C.toKilometersPerSecond)
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
    Order.orderBy(_.z)

  /** @group Typeclass Instances */
  implicit val ordering: scala.Ordering[Redshift] =
    scala.Ordering.by(_.z)

  /**
   * Additive monoid for `Redshift`
   * @group Typeclass Instances
   */
  implicit val monoid: Monoid[Redshift] =
    new Monoid[Redshift] {
      val zero = Redshift.zero
      def append(a: Redshift, b: => Redshift): Redshift = Redshift(a.z + b.z)
    }

}
