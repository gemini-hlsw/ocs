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

  /** @group Typeclass Instances */
  implicit val order: Order[AngularVelocity] =
    Order.orderBy(_.masPerYear)

  /** @group Typeclass Instances */
  implicit val ordering: scala.Ordering[AngularVelocity] =
    scala.Ordering.by(_.masPerYear)

}

object RightAscensionAngularVelocity {

  /** 
   * The `RightAscensionAngularVelocity` of zero miliarcsecs/year.
   * @group Constructors
   */
  val Zero = RightAscensionAngularVelocity(AngularVelocity(0.0))

}

object DeclinationAngularVelocity {
  /** 
   * The `DeclinationAngularVelocity` of zero miliarcsecs/year.
   * @group Constructors
   */
  val Zero = DeclinationAngularVelocity(AngularVelocity(0.0))

}
