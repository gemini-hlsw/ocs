package edu.gemini.itc.operation

import edu.gemini.itc.base.Instrument
import edu.gemini.itc.shared.{AutoAperture, ObservationDetails, SourceDefinition, UserAperture}

/**
 * Base trait for spectroscopy slits.
 * A slit in this context defines the rectangular aperture used for spectroscopy calculations and is relevant
 * for the slit throughput, i.e. the flux (or the number of electrons) that arrive on the CCD.
 */
sealed trait Slit {

  def pixelSize: Double

  def width: Double
  def length: Double
  def area: Double

  // Ignore partial pixels for throughput calculations.
  // The values here are rounded to the next full pixel, so depending on future usage of these values this
  // may or may not be what is needed. Please use with care. This current implementation follows the original
  // implementation.
  def widthPixels: Int        = Math.max(1, (width / pixelSize).round).toInt
  def lengthPixels: Int       = Math.max(1, (length / pixelSize).round).toInt
  def areaPixels: Int         = widthPixels * lengthPixels

}

/** Slit that is 1-pixel long for calculating the peak pixel flux. */
final case class OnePixelSlit(width: Double, pixelSize: Double) extends Slit {

  // width = slit width in arcseconds
  // pixelSize = pixel size in arcseconds
  val length                  = pixelSize        // slit length in arcseconds
  val area                    = width * length   // slit area in square arcseconds

  override val widthPixels    = Math.max(1, (width / pixelSize).round).toInt
  override val lengthPixels   = 1
  override val areaPixels     = widthPixels * lengthPixels

}

/** Slit that covers exactly one arcsec². This is used for auto apertures on uniform sources. */
final case class OneArcsecSlit(width: Double, pixelSize: Double) extends Slit {

  val area                    = 1.0
  val length                  = area / width

}

/** Arbitrary aperture slits are defined by their mask width and the user defined slit length. */
final case class RectangleSlit(width: Double, length: Double, pixelSize: Double) extends Slit {

  // The area should/could be width*length, the way it is calculated here mimics some rounding differences
  // (full vs partial pixels) from the original code that I am keeping in place for now; maybe this can
  // can be simplified in the future.
  val area                    = width * lengthPixels * pixelSize

}

object Slit {

  /** Creates an aperture/slit for the given values. */
  def apply(width: Double, lengthPixels: Double, pixelSize: Double): Slit =
    RectangleSlit(width, lengthPixels * pixelSize, pixelSize)

  /** Creates an aperture/slit for the given configuration. */
  def apply(src: SourceDefinition, obs: ObservationDetails, instrument: Instrument, slitWidth: Double, imageQuality: Double): Slit =
    obs.analysisMethod match {

      case UserAperture(slitLength, _)      =>
        // user aperture slits are defined by mask width and the user defined slit length
        RectangleSlit(slitWidth, slitLength, instrument.getPixelSize)

      case AutoAperture(_) if src.isUniform =>
        // auto aperture slits for uniform sources are defined by mask width and an area of 1 arcsec²
        OneArcsecSlit(slitWidth, instrument.getPixelSize)

      case AutoAperture(_)                  =>
        // auto aperture slits for non-uniform sources are defined by mask width and the image quality
        RectangleSlit(slitWidth, 1.4 * imageQuality, instrument.getPixelSize)
    }

}
