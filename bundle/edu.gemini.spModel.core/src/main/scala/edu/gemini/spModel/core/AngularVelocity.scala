package edu.gemini.spModel.core

import scalaz._, Scalaz._

/** An angular velocity in [mas/y]  */
case class AngularVelocity(masPerYear: Double) extends AnyVal with java.io.Serializable {

  /**
   * This `AngularVelocity` in degrees/year, it can be positive or negative
   * @group Conversions
   */
  def toDegreesPerYear: Double = masPerYear / AngularVelocity.MilliArcSecsInADegree

}

/** An Angular Velocity in Right Ascension */
case class RightAscensionAngularVelocity(velocity: AngularVelocity)

/** An Angular Velocity in Declination */
case class DeclinationAngularVelocity(velocity: AngularVelocity)

object AngularVelocity {
  val MilliArcSecsInADegree:Double = 1296000000

  val Zero = AngularVelocity(0.0)

  /** @group Typeclass Instances */
  implicit val order: Order[AngularVelocity] =
    Order.orderBy(_.masPerYear)

  /** @group Typeclass Instances */
  implicit val ordering: scala.Ordering[AngularVelocity] =
    scala.Ordering.by(_.masPerYear)

  /** @group Typeclass Instances */
  implicit val equal: Equal[AngularVelocity] =
    Equal.equalA[AngularVelocity]
  
  /** @group Typeclass Instances */
  implicit val monoid: Monoid[AngularVelocity] =
    Monoid.instance((a, b) => AngularVelocity(a.masPerYear + b.masPerYear), Zero)

}

object RightAscensionAngularVelocity {

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
  implicit val equal: Equal[RightAscensionAngularVelocity] =
    Equal.equalA[RightAscensionAngularVelocity]

  /** @group Typeclass Instances */
  implicit val monoid: Monoid[RightAscensionAngularVelocity] =
    Monoid.instance((a, b) => RightAscensionAngularVelocity(a.velocity |+| b.velocity), Zero)

}

object DeclinationAngularVelocity {

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
  implicit val equal: Equal[DeclinationAngularVelocity] =
    Equal.equalA[DeclinationAngularVelocity]

  /** @group Typeclass Instances */
  implicit val monoid: Monoid[DeclinationAngularVelocity] =
    Monoid.instance((a, b) => DeclinationAngularVelocity(a.velocity |+| b.velocity), Zero)
}
