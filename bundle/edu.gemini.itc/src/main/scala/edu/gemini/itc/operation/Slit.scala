package edu.gemini.itc.operation

import edu.gemini.itc.base.{Instrument, SpectroscopyInstrument}
import edu.gemini.itc.shared.{AutoAperture, UserAperture, ObservationDetails, SourceDefinition}

/**
 * Base trait for spectroscopy slits. A slit in this context defines the rectangular aperture
 * used for the ITC calculations.
 */
trait Slit {

  def width: Double
  def length: Double
  def area: Double

  def widthPixels: Double
  def lengthPixels: Double
  def areaPixels: Double

}

/** User aperture slit is defined by mask width and the user defined slit length. */
final case class UserApertureSlit(width: Double, length: Double, pixelSize: Double) extends Slit {

  def widthPixels = (width / pixelSize).round   // TODO: make sure this is at least 1 ?
  def lengthPixels = (length / pixelSize).round // TODO: make sure this is at least 1 ?

  def area = width * lengthPixels * pixelSize   // TODO: this should be width * length
  def areaPixels = widthPixels * lengthPixels   // TODO: ok? rounded..

}

/** Auto aperture slit is defined by mask width and its area of 1 arcsec2. */
final case class AutoApertureSlit(width: Double, pixelSize: Double) extends Slit {

  val area = 1.0
  val length = area / width

  val widthPixels: Double = (width / pixelSize).round   // TODO: turn into int, make sure > 0; combine with code from generic slit
  val lengthPixels: Double = (length / pixelSize).round  // TODO: turn into int
  val areaPixels = widthPixels * lengthPixels

}

object Slit {

  def apply(width: Double, lengthPixels: Double, pixelSize: Double): Slit =
    UserApertureSlit(width, lengthPixels * pixelSize, pixelSize)

  def apply(src: SourceDefinition, obs: ObservationDetails, instrument: Instrument, slitWidth: Double, imageQuality: Double): Slit =
    if (src.isUniform && obs.isAutoAperture)
      AutoApertureSlit(slitWidth, instrument.getPixelSize)
    else
        UserApertureSlit(
          slitWidth,
          slitLength(src, obs, imageQuality),
          instrument.getPixelSize
        )

  private def slitLength(src: SourceDefinition, obs: ObservationDetails, imageQuality: Double) = obs.analysisMethod match {
    case UserAperture(diameter, _) => diameter
    case AutoAperture(_)           => 1.4 * imageQuality
  }

}
