package edu.gemini.ags.gems.mascot

import edu.gemini.spModel.core.{Magnitude, MagnitudeBand}
import scalaz._
import Scalaz._

/**
 * Defines limits for magnitude bands (Do we need upper and lower bounds?)
 */
case class MagLimits private (bmag: Double,
                     vmag: Double,
                     rmag: Double,
                     jmag: Double,
                     hmag: Double,
                     kmag: Double) {

  // preallocate to use in filter
  private lazy val filters = List(new Magnitude(bmag, MagnitudeBand.B), new Magnitude(vmag, MagnitudeBand.V), new Magnitude(rmag, MagnitudeBand.R), new Magnitude(jmag, MagnitudeBand.J), new Magnitude(hmag, MagnitudeBand.H), new Magnitude(kmag, MagnitudeBand.K))

  /**
   * Returns true if the given star is within the mag limits
   */
  def filter(star: Star): Boolean =
    filters.forall { m => star.target.magnitudeIn(m.band).exists(_.value <= m.value) }
}


object MagLimits {

  // value that matches any mag
  val defaultMag = 99

  def apply(): MagLimits = MagLimits(defaultMag, defaultMag, defaultMag, defaultMag, defaultMag, defaultMag)
}
