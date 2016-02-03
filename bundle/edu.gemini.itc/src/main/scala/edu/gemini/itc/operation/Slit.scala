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

  def widthPixels: Double       // TODO: use int?
  def lengthPixels: Double      // TODO: use int?
  def areaPixels: Double        // TODO: use int?

}

/** Slit that covers exactly one pixel². This is used to calculate the peak pixel flux for a single pixel. */
final case class OnePixelSlit(pixelSize: Double) extends Slit {

  val width         = pixelSize
  val length        = pixelSize
  val area          = width * length

  val widthPixels   = 1.0
  val lengthPixels  = 1.0
  val areaPixels    = 1.0

}

/** Slit that covers exactly one arcsec². This is used for auto apertures on uniform sources. */
final case class OneArcsecSlit(width: Double, pixelSize: Double) extends Slit {

  val area        = 1.0
  val length      = area / width

  val widthPixels: Double = (width / pixelSize).round   // TODO: turn into int, make sure > 0; combine with code from generic slit
  val lengthPixels: Double = (length / pixelSize).round  // TODO: turn into int
  val areaPixels = widthPixels * lengthPixels

}

/** Arbitrary aperture slits are defined by their mask width and the user defined slit length. */
final case class RectangleSlit(width: Double, length: Double, pixelSize: Double) extends Slit {

  val widthPixels: Double   = (width / pixelSize).round   // TODO: make sure this is at least 1 ?
  val lengthPixels: Double  = (length / pixelSize).round // TODO: make sure this is at least 1 ?

  val area          = width * lengthPixels * pixelSize   // TODO: this should be width * length
  val areaPixels    = widthPixels * lengthPixels   // TODO: ok? rounded..

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
