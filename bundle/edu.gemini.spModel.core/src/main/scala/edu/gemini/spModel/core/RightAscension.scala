package edu.gemini.spModel.core

import scalaz._, Scalaz._

/** Newtype for `Angle`, tagging it as a right ascension. */
sealed trait RightAscension extends java.io.Serializable {

  /**
   * This `RightAscension` as an untagged `Angle`.
   * @group Conversions
   */
  def toAngle: Angle

  def toDegrees: Double =
    toAngle.toDegrees

  def toHours: Double =
    toDegrees / 15.0

  /**
   * Offset this `RightAscension` by the given angle.
   * @group Operations
   */
  def offset(a: Angle): RightAscension =
    RightAscension.fromAngle(toAngle + a)

  /** @group Overrides */
  override final def toString =
    f"RA($toAngle)"

  /** @group Overrides */
  override final def equals(a: Any) =
    a match {
      case ra: RightAscension => ra.toAngle == this.toAngle
      case _ => false
    }

  /** @group Overrides */
  override final def hashCode =
    toAngle.hashCode

  /**
    * @see [[Angle.formatDegrees]]
    * @group Formatters
    */
  def formatDegrees: String =
    this.toAngle.formatDegrees

  /**
    * @see [[Angle.formatHMS]]
    * @group Formatters
    */
  def formatHMS: String =
    this.toAngle.formatHMS

}

object RightAscension {

  /**
   * Construct a `RightAscension` from an `Angle`.
   * @group Constructors
   */
  def fromAngle(a: Angle): RightAscension =
    new RightAscension {
      val toAngle = a
    }

  /**
   * Construct a `RightAscension` from the given value in degrees, which will be normalized to [0, 360).
   * @group Constructors
   */
  def fromDegrees(d: Double): RightAscension =
    fromAngle(Angle.fromDegrees(d))

  /**
   * Construct a `RightAscension` from the given value in hours, which will be normalized to [0, 24).
   * @group Constructors
   */
  def fromHours(h: Double): RightAscension =
    fromAngle(Angle.fromHours(h))

  /**
   * The `RightAscension` at zero degrees.
   * @group Constructors
   */
  val zero: RightAscension =
    fromAngle(Angle.zero)

  /** @group Typeclass Instances */
  implicit val RightAscensionOrder: Order[RightAscension] =
    Order.orderBy(_.toAngle)

  /** @group Typeclass Instances */
  implicit val RightAscensionOrdering: scala.Ordering[RightAscension] =
    scala.Ordering.by(_.toAngle)

}
