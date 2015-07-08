package edu.gemini.ags

import edu.gemini.catalog.api.MagnitudeExtractor
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.core.Target.SiderealTarget

package object api {

  val RLikeBands = List(MagnitudeBand._r, MagnitudeBand.R, MagnitudeBand.UC)

  /**
   * For a given target set of probe bands build a MagnitudeExtractor that returns the first magnitude on the target
   */
  def magnitudeExtractor(probeBands: List[MagnitudeBand]): MagnitudeExtractor = (st: SiderealTarget) => probeBands.flatMap(st.magnitudeIn).headOption // Picks the first available magnitude on the target

  /**
   * Default function to find the valid probe bands from a single band.
   * It essentially expands R into r', R and UC while leaving the other bands untouched
   */
  def defaultProbeBands(band: MagnitudeBand): List[MagnitudeBand] = band match {
      case MagnitudeBand.R => RLikeBands
      case _               => List(band)
    }

}
