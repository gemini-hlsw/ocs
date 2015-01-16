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
  private lazy val filters = List(new Magnitude(bmag, MagnitudeBand.B).some, new Magnitude(vmag, MagnitudeBand.V).some, new Magnitude(rmag, MagnitudeBand.R).some, new Magnitude(jmag, MagnitudeBand.J).some, new Magnitude(hmag, MagnitudeBand.H).some, new Magnitude(kmag, MagnitudeBand.K).some)

  /**
   * Returns true if the given star is within the mag limits
   */
  def filter(star: Star): Boolean =
    filters.map(f => star.target.magnitudeIn(f.get.band) <= f).map(Tags.Conjunction).suml
}


object MagLimits {

  // value that matches any mag
  val defaultMag = 99

  def apply(): MagLimits = MagLimits(defaultMag, defaultMag, defaultMag, defaultMag, defaultMag, defaultMag)
}
