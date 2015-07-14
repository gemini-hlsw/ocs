package edu.gemini.ags

import edu.gemini.catalog.api.{SingleBandExtractor, FirstBandExtractor, MagnitudeExtractor}
import edu.gemini.spModel.core.MagnitudeBand

import scalaz._
import Scalaz._

package object api {

  val RLikeBands = NonEmptyList(MagnitudeBand._r, MagnitudeBand.R, MagnitudeBand.UC)

  def agsBandExtractor(band: MagnitudeBand): MagnitudeExtractor = if (band === MagnitudeBand.R) FirstBandExtractor(RLikeBands) else SingleBandExtractor(band)

  /**
   * Default function to find the valid probe bands from a single band.
   * It essentially expands R into r', R and UC while leaving the other bands untouched
   */
  def defaultProbeBands(band: MagnitudeBand): NonEmptyList[MagnitudeBand] = band match {
      case MagnitudeBand.R => RLikeBands
      case _               => NonEmptyList(band)
    }

}
