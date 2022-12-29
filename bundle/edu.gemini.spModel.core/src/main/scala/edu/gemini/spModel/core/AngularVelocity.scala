package edu.gemini.spModel.core

import scalaz._, Scalaz._

/** An angular velocity in [mas/y]  */
case class AngularVelocity(masPerYear: Double) extends AnyVal with java.io.Serializable {

  /**
   * This `AngularVelocity` in degrees/year, it can be positive or negative
   * @group Conversions
   */
  def toDegreesPerYear: Double = masPerYear / AngularVelocity.DegreesYearToMilliArcSecsYear

}


/** An Angular Velocity in Right Ascension */
case class RightAscensionAngularVelocity(velocity: AngularVelocity)

/** An Angular Velocity in Declination */
case class DeclinationAngularVelocity(velocity: AngularVelocity)

object AngularVelocity {

  /**
    * Builds an AngularVelocity expressed on degrees per year, converting
    * to milli arcsecs per year
    */
  def fromDegreesPerYear(dpy: Double): AngularVelocity =
    AngularVelocity(dpy * DegreesYearToMilliArcSecsYear)

  val masPerYear: AngularVelocity @> Double =
    Lens.lensu((a, b) => a.copy(masPerYear = b), _.masPerYear)

  val DegreesYearToMilliArcSecsYear:Double = 3.6e06

  val Zero = AngularVelocity(0.0)

  /** @group Typeclass Instances */
  implicit val order: Order[AngularVelocity] =
    Order.orderBy(_.masPerYear)

  /** @group Typeclass Instances */
  implicit val ordering: scala.Ordering[AngularVelocity] =
    scala.Ordering.by(_.masPerYear)

  /** @group Typeclass Instances */
  implicit val monoid: Monoid[AngularVelocity] =
    Monoid.instance((a, b) => AngularVelocity(a.masPerYear + b.masPerYear), Zero)

}

object RightAscensionAngularVelocity {

  val velocity: RightAscensionAngularVelocity @> AngularVelocity =
    Lens.lensu((a, b) => a.copy(velocity = b), _.velocity)

  /**
   * The `RightAscensionAngularVelocity` of zero miliarcsecs/year.
   * @group Constructors
   */
  val Zero = RightAscensionAngularVelocity(AngularVelocity(0.0))

  /** @group Typeclass Instances */
  implicit val order: Order[RightAscensionAngularVelocity] =
    Order.orderBy(_.velocity.masPerYear)

  /** @group Typeclass Instances */
  implicit val ordering: scala.Ordering[RightAscensionAngularVelocity] =
    scala.Ordering.by(_.velocity.masPerYear)

  /** @group Typeclass Instances */
  implicit val monoid: Monoid[RightAscensionAngularVelocity] =
    Monoid.instance((a, b) => RightAscensionAngularVelocity(a.velocity |+| b.velocity), Zero)

}

object DeclinationAngularVelocity {

  val velocity: DeclinationAngularVelocity @> AngularVelocity =
    Lens.lensu((a, b) => a.copy(velocity = b), _.velocity)

  /**
   * The `DeclinationAngularVelocity` of zero miliarcsecs/year.
   * @group Constructors
   */
  val Zero = DeclinationAngularVelocity(AngularVelocity(0.0))

  /** @group Typeclass Instances */
  implicit val order: Order[DeclinationAngularVelocity] =
    Order.orderBy(_.velocity.masPerYear)

  /** @group Typeclass Instances */
  implicit val ordering: scala.Ordering[DeclinationAngularVelocity] =
    scala.Ordering.by(_.velocity.masPerYear)

  /** @group Typeclass Instances */
  implicit val monoid: Monoid[DeclinationAngularVelocity] =
    Monoid.instance((a, b) => DeclinationAngularVelocity(a.velocity |+| b.velocity), Zero)
}
