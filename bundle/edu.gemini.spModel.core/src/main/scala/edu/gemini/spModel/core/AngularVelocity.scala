package edu.gemini.spModel.core

import scalaz._, Scalaz._

/** An angular velocity in [mas/y] in the range [-1296000000, 1296000000] equivalent to [-360, 360] degrees. */
sealed trait AngularVelocity extends java.io.Serializable {

  /** 
   * This `AngularVelocity` in milli-arc-seconds/year, it can be positive or negative
   * @group Conversions
   */
  def toMilliArcSecondsPerYear: Double

  /**
   * This `AngularVelocity` in degrees/year, it can be positive or negative
   * @group Conversions
   */
  def toDegreesPerYear: Double = toMilliArcSecondsPerYear / AngularVelocity.MilliArcSecsInADegree

  /** @group Overrides */
  override final def equals(a: Any) =
    a match {
      case av: AngularVelocity => av.toMilliArcSecondsPerYear === this.toMilliArcSecondsPerYear
      case _                   => false
    }

  /** @group Overrides */
  override final def hashCode =
    toMilliArcSecondsPerYear.hashCode
  
}

/** An Angular Velocity in Right Ascension */
trait RightAscensionAngularVelocity extends AngularVelocity
/** An Angular Velocity in Declination */
trait DeclinationAngularVelocity extends AngularVelocity

object AngularVelocity {
  val MilliArcSecsInADegree:Double = 1296000000
  
  /** @group Typeclass Instances */
  implicit val order: Order[AngularVelocity] =
    Order.orderBy(_.toMilliArcSecondsPerYear)

  /** @group Typeclass Instances */
  implicit val ordering: scala.Ordering[AngularVelocity] =
    scala.Ordering.by(_.toMilliArcSecondsPerYear)

}

object RightAscensionAngularVelocity {

  /** 
   * The `RightAscensionAngularVelocity` of zero miliarcsecs/year.
   * @group Constructors
   */
  val Zero = fromMilliArcSecondsPerYear(0.0)

  /**
   * Construct a `RightAscensionAngularVelocity` from the given value in mas/y, which will be normalized to [-1296000000, 1296000000].
   * @group Constructors
   */
  def fromMilliArcSecondsPerYear(mas: Double): RightAscensionAngularVelocity = new RightAscensionAngularVelocity {
    override def toMilliArcSecondsPerYear: Double = mas % AngularVelocity.MilliArcSecsInADegree
  }
}

object DeclinationAngularVelocity {
  /** 
   * The `DeclinationAngularVelocity` of zero miliarcsecs/year.
   * @group Constructors
   */
  val Zero = fromMilliArcSecondsPerYear(0.0)

  /**
   * Construct a `DeclinationAngularVelocity` from the given value in mas/y, which will be normalized to [-1296000000, 1296000000].
   * @group Constructors
   */
  def fromMilliArcSecondsPerYear(mas: Double): DeclinationAngularVelocity = new DeclinationAngularVelocity {
    override def toMilliArcSecondsPerYear: Double = mas % AngularVelocity.MilliArcSecsInADegree
  }
}
