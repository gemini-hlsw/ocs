package edu.gemini.spModel.core

import scalaz._
import Scalaz._

/**
 * Parallax in mas
 * Store the value in mas to avoid rounding errors
 */
sealed abstract case class Parallax private (mas: Double) {

  assert(mas >= 0.0, "Parallax must be >= 0.0")

}

object Parallax {

  def fromMas(mas: Double): Option[Parallax] =
    (mas >= 0.0).option(new Parallax(mas) {})

  def unsafeFromMas(mas: Double): Parallax =
    fromMas(mas).get

  /**
   * The `No parallax`
   * @group Constructors
   */
  val zero: Parallax =
    unsafeFromMas(0.0)

  /** @group Typeclass Instances */
  implicit val order: Order[Parallax] =
    Order.orderBy(_.mas)

  /** @group Typeclass Instances */
  implicit val ordering: scala.Ordering[Parallax] =
    scala.Ordering.by(_.mas)

  /**
   * Additive monoid for `Parallax`
   * @group Typeclass Instances
   */
  implicit val monoid: Monoid[Parallax] =
    new Monoid[Parallax] {
      val zero = Parallax.zero
      def append(a: Parallax, b: => Parallax): Parallax = new Parallax(a.mas + b.mas) {}
    }

}
