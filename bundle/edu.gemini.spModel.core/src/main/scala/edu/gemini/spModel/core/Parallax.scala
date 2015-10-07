package edu.gemini.spModel.core

import scalaz._
import Scalaz._

/**
 * Parallax in mas
 * Store the value in mas to avoid rounding errors
 */
class Parallax(val mas: Double) extends AnyVal

object Parallax {
  def apply(mas: Double):Parallax = new Parallax(mas)
  //def fromMas(mas: Double):Option[Parallax] = (mas > 0) option new Parallax(mas)

  /**
   * The `No parallax`
   * @group Constructors
   */
  val zero: Parallax = Parallax(0)

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
      def append(a: Parallax, b: => Parallax): Parallax = Parallax(a.mas + b.mas)
    }

}
