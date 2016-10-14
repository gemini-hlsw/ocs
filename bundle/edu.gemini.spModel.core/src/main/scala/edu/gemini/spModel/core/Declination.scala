package edu.gemini.spModel.core

import scalaz._, Scalaz._

/**
 * Newtype for an `Angle` in [270 - 360) + [0 - 90], tagged as a declination. By convention such
 * angles are logically in the range [-90 -90]; the provided formatters respect this convention.
 */
sealed trait Declination extends java.io.Serializable {

  /**
   * This `Declination` as an angle in [270 - 360) + [0 - 90].
   * @group Conversions
   */
  def toAngle: Angle

  /**
   * This `Declination` in degrees in (-90, 90]; for (0, 360] use `.toAngle.toDegrees`.
   * @group Conversions
   */
  def toDegrees: Double = {
    val d = toAngle.toDegrees
    if (d <= 90) d else d - 360
  }

  /**
   * Offset this `Declination` by the given angle, returning the result and a carry bit. A carry
   * of `true` indicates that the result lies on the opposite side of the sphere and the
   * associated `RightAscension` (if any) must be flipped by 180°.
   * @group Operations
   */
  def offset(a: Angle): (Declination, Boolean) = {
    val a0 = toAngle + a
    Declination.fromAngle(a0).strengthR(false).orElse {
      val a1 = a0.toDegrees
      val a2 = if (a1 > 180) 540 - a1
               else          180 - a1
      Declination.fromAngle(Angle.fromDegrees(a2)).strengthR(true)
    }.get // safe
  }

  /** @group Overrides */
  final override def toString: String =
    s"Dec($toAngle)"

  /** @group Overrides */
  final override def equals(a: Any) =
    a match {
      case dec: Declination => dec.toAngle == this.toAngle
      case _                => false
    }

  /** @group Overrides */
  final override def hashCode =
    toAngle.hashCode

  /**
   * @see [[Declination.formatDegrees]]
   * @group Formatters
   */
  def formatDegrees: String =
    Declination.formatDegrees(this)

  /**
   * @see [[Declination.formatSexigesimal]]
   * @group Formatters
   */
  def formatSexigesimal: String =
    Declination.formatSexigesimal(this)

  /**
   * @see [[Declination.formatDMS]]
   * @group Formatters
   */
  def formatDMS: String =
    Declination.formatDMS(this)

}

object Declination {

  /**
   * Construct a `Declination` from an `Angle` normalizable in [270 - 360) + [0 - 90], if possible.
   * @group Constructors
   */
  def fromAngle(a: Angle): Option[Declination] =
    (a.toDegrees >= 270 || a.toDegrees <= 90) option
      new Declination {
        def toAngle = a
      }

  /**
   * Construct a `Declination` from an angle in decimal degrees normalizable in [270 - 360) + [0 - 90], if possible.
   * @group Constructors
   */
  def fromDegrees(d: Double): Option[Declination] =
    fromAngle(Angle.fromDegrees(d))

  /**
   * The `Declination` at zero degrees.
   * @group Constructors
   */
  val zero: Declination =
    new Declination {
      def toAngle = Angle.zero
    }

  /** @group Typeclass Instances */
  implicit val DeclinationOrder: Order[Declination] =
    Order.orderBy(_.toDegrees)

  /** @group Typeclass Instances */
  implicit val DeclinationOrdering: scala.math.Ordering[Declination] =
    scala.math.Ordering.by(_.toDegrees)

  /**
   * Format this `Declination` in decimal degrees in (-90, 90] with three decimal places,
   * followed by the degree sign.
   * @group Formatters
   */
  def formatDegrees(dec: Declination): String =
    f"${dec.toDegrees}%4.03f°"

  /**
   * Format the given `Declination` in sexigesimal `d:mm:ss` with degrees in (-90, 90], with three
   * fractional digits for seconds.
   * @group Formatters
   */
  def formatSexigesimal(dec: Declination, sep: String = ":", fractionalDigits: Int = 2): String = {
    val a0       = dec.toDegrees
    val (a, sgn) = if (a0 < 0) (a0.abs, "-") else (a0, "")
    s"$sgn${Angle.formatSexigesimal(Angle.fromDegrees(a), sep, fractionalDigits)}"
  }

  /**
   * Alias for [[Declination.formatSexigesimal]]
   * @group Formatters
   */
  def formatDMS(dec: Declination, sep: String = ":", fractionalDigits: Int = 2): String =
    formatSexigesimal(dec, sep, fractionalDigits)

}
