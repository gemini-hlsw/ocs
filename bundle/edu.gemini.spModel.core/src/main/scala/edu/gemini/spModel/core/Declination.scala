package edu.gemini.spModel.core

import scalaz._, Scalaz._

/** 
 * Newtype for an `Angle` in [270 - 360) + [0 - 90), tagged as a declination. By convention such
 * angles are logically in the range [-90 -90); the provided formatters respect this convention.
 */
sealed trait Declination {
  
  /** 
   * This `Declination` as an angle in [270 - 360) + [0 - 90). 
   * @group Conversions
   */
  def toAngle: Angle

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
      val a2 = if (a1 == 90) 270
         else  if (a1 > 180) 540 - a1
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

}

object Declination {

  /** 
   * Construct a `Declination` from an `Angle` normalizable in [270 - 360) + [0 - 90), if possible. 
   * @group Constructors
   */
  def fromAngle(a: Angle): Option[Declination] =
    (a.toDegrees >= 270 || a.toDegrees < 90) option 
      new Declination {
        def toAngle = a
      }

  /** 
   * The `Declination` at zero degrees. 
   * @group Constructors
   */
  val zero: Declination = 
    new Declination {
      def toAngle = Angle.zero
    }

  // TODO: order/ordering based on -90 to 90

}



