package edu.gemini.catalog.api

import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._

import scalaz._
import Scalaz._

trait RadiusConstraint {
  val maxLimit: Angle
  val minLimit: Angle

  /**
   * If there is an offset but there isn't a posAngle, then we have to adjust the
   * search radius to take into account any position angle. That means the
   * outer limit increases by the distance from the base to the offset and the
   * inner limit decreases by the same distance (never less than 0 though).
   *
   * @return a new (possibly) adjusted radius limits
   */
  def adjust(offset: Offset): RadiusConstraint

  /**
   * Returns a filter for coordinates with a distance inside the range
   */
  def targetsFilter(base: Coordinates): SiderealTarget => Boolean

  // REMOVE When AGS is fully ported
  @Deprecated
  def toRadiusLimit = new RadiusLimits(edu.gemini.skycalc.Angle.degrees(maxLimit.toDegrees), edu.gemini.skycalc.Angle.degrees(minLimit.toDegrees))
}

/**
 * Describes limits for catalog cone search radius values.
 * See OT-17.
 */
object RadiusConstraint {
  val empty = between(Angle.zero, Angle.zero)

  private case class RadiusConstraintImpl(minLimit: Angle, maxLimit: Angle) extends RadiusConstraint {

    def adjust(offset: Offset): RadiusConstraint = {
      val d = offset.distance
      val max = maxLimit + d
      val min = Angle.fromDegrees(scala.math.max(minLimit.toDegrees - d.toDegrees, 0.0))

      RadiusConstraintImpl(max, min)
    }

    /**
     * Returns a filter for coordinates with a distance inside the range
     */
    def targetsFilter(base: Coordinates): SiderealTarget => Boolean = target => {
      val distance = Coordinates.difference(base, target.coordinates).distance
      distance >= minLimit && distance <= maxLimit
    }

  }

  /**
   * Constructs a range between 2 angles. It will produce the correct ordering to create a valid range
   */
  def between(minLimit: Angle, maxLimit: Angle): RadiusConstraint =
    if (minLimit < maxLimit) {
      RadiusConstraintImpl(minLimit, maxLimit)
    } else {
      RadiusConstraintImpl(maxLimit, minLimit)
    }

}