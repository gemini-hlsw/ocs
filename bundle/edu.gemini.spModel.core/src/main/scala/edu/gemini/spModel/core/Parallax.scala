package edu.gemini.spModel.core

import scalaz.{Monoid, Order}

/**
 * Parallax in mas
 */
case class Parallax(angle: Angle)

object Parallax {
  /**
   * The `No parallax`
   * @group Constructors
   */
  val zero: Parallax = Parallax(Angle.zero)

  /** @group Typeclass Instances */
  implicit val order: Order[Parallax] =
    Order.orderBy(_.angle)

  /** @group Typeclass Instances */
  implicit val ordering: scala.Ordering[Parallax] =
    scala.Ordering.by(_.angle)

  /**
   * Additive monoid for `Parallax`
   * @group Typeclass Instances
   */
  implicit val monoid: Monoid[Parallax] =
    new Monoid[Parallax] {
      val zero = Parallax.zero
      def append(a: Parallax, b: => Parallax): Parallax = Parallax(a.angle + b.angle)
    }

}
