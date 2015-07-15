package edu.gemini.ags.gems.mascot

import edu.gemini.catalog.api.RBandsList
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
  private lazy val filters = List(new Magnitude(bmag, MagnitudeBand.B), new Magnitude(vmag, MagnitudeBand.V), new Magnitude(jmag, MagnitudeBand.J), new Magnitude(hmag, MagnitudeBand.H), new Magnitude(kmag, MagnitudeBand.K))

  /**
   * Returns true if the given star is within the mag limits
   */
  def filter(star: Star): Boolean =
    // requires that B, V, J, H and K are within limits and at least one of r, R, UC are within limits
    filters.forall { m => star.target.magnitudeIn(m.band).exists(_.value <= m.value) } && RBandsList.extract(star.target).isDefined
}


object MagLimits {

  // value that matches any mag
  val defaultMag = 99

  def apply(): MagLimits = MagLimits(defaultMag, defaultMag, defaultMag, defaultMag, defaultMag, defaultMag)
}
