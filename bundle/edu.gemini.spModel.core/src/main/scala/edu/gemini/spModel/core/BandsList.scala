package edu.gemini.spModel.core

import scalaz._
import Scalaz._

/**
 * Defines a list of bands
 * It is used, e.g. to extract a magnitude from a target
 */
sealed trait BandsList {
  val bands: List[MagnitudeBand]
  def extract(t: SiderealTarget) = bands.map(t.magnitudeIn).find(_.isDefined).flatten
  def bandSupported(b: MagnitudeBand) = bands.contains(b)
}

/**
 * Extracts the first valid R Band Magnitude if available
 */
case object RBandsList extends BandsList {
  val instance = this

  val bands = List(MagnitudeBand._r, MagnitudeBand.R, MagnitudeBand.UC)
}

/**
 * Extractor for Nici containing 4 bands
 */
case object NiciBandsList extends BandsList {
  val bands = List(MagnitudeBand._r, MagnitudeBand.R, MagnitudeBand.UC, MagnitudeBand.V)
}

/**
 * Extracts a single band from a target if available
 */
case class SingleBand(band: MagnitudeBand) extends BandsList {
  val bands = List(band)
}

case object NoBands extends BandsList {
  val bands = Nil
}

object BandsList {
  implicit val equals = Equal.equal[BandsList]((a, b) => a.bands === b.bands)

  /**
   * Returns a commonly used convention for bands list given a particular band.
   * This is commonly in catalogs that can provide multiple representations of the R band
   */
  def bandList(band: MagnitudeBand):BandsList = band match {
    case MagnitudeBand.R  => RBandsList
    case MagnitudeBand._r => RBandsList
    case MagnitudeBand.UC => RBandsList
    case _                => SingleBand(band)
  }

}
