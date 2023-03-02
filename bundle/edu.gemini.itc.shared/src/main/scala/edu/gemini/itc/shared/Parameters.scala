package edu.gemini.itc.shared

import edu.gemini.shared.util.immutable.ImEither
import edu.gemini.spModel.core.{BrightnessUnit, MagnitudeBand, Redshift, SpatialProfile, SpectralDistribution, UniformSource}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, ImageQuality, SkyBackground, WaterVapor}

import scalaz._
import Scalaz._

// ==== Observing conditions

final case class ExactIq private (toArcsec: Double)

object ExactIq {

  def fromArcsec(d: Double): Option[ExactIq] =
    if ((0.05 <= d) && (d <= 5.0)) Some(ExactIq(d)) else None

  def unsafeFromArcsec(d: Double): ExactIq =
    fromArcsec(d).get

  def fromArcsecOrException(d: Double): ExactIq =
    fromArcsec(d)
      .getOrElse(throw new IllegalArgumentException("ExactIQ must be in the range 0.05 - 5.0 arcsec"))

  implicit def EqualExactIq: Equal[ExactIq] =
    Equal.equalBy(_.toArcsec)

}

final case class ExactCc private (toExtinction: Double)

object ExactCc {

  def fromExtinction(d: Double): Option[ExactCc] =
    if ((0.00 <= d) && (d <= 5.0)) Some(ExactCc(d)) else None

  def unsafeFromExtinction(d: Double): ExactCc =
    fromExtinction(d).get

  def fromExtinctionOrException(d: Double): ExactCc =
    fromExtinction(d)
      .getOrElse(throw new IllegalArgumentException("ExactCC must be in the range 0.0 - 5.0"))

  implicit def EqualExactCc: Equal[ExactCc] =
    Equal.equalBy(_.toExtinction)

}

final case class ObservingConditions(
                      iq: ExactIq \/ ImageQuality,
                      cc: ExactCc \/ CloudCover,
                      wv: WaterVapor,
                      sb: SkyBackground,
                      airmass: Double) {

  import edu.gemini.shared.util.immutable.ScalaConverters._

  def javaIq: ImEither[ExactIq, ImageQuality] =
    iq.asImEither

  def javaCc: ImEither[ExactCc, CloudCover] =
    cc.asImEither

  def ccExtinction: Double =
    cc.fold(_.toExtinction, _.getExtinction)

}

// ==== Source definition
final case class SourceDefinition(
                     profile: SpatialProfile,
                     distribution: SpectralDistribution,
                     norm: Double,
                     units: BrightnessUnit,
                     normBand: MagnitudeBand,
                     redshift: Redshift) {

  val isUniform: Boolean = profile match {
    case UniformSource => true
    case _             => false
  }
}

// ==== Calculation method

// The calculation method denotes if we are doing spectroscopy or imaging and if we do integration (only for imaging)
// or signal to noise calculations. The difference between imaging and spectroscopy is defined by the instrument
// parameters and should/could be derived from there in the future.

sealed trait Imaging
sealed trait Spectroscopy

sealed trait CalculationMethod {
  def exposureTime: Double
  def coadds: Option[Int]
  /*
     It seems that using .getOrElse is not straightforward to use from Java,
     so we can use this "coaddsOrElse" to simplify it.
   */
  def coaddsOrElse(d: Int): Int = coadds.getOrElse(d)
  def sourceFraction: Double
  def offset: Double
}

sealed trait S2NMethod extends CalculationMethod {
  def exposures: Int
}

sealed trait IntMethod extends CalculationMethod {
  def sigma: Double
}

sealed trait ExpMethod extends CalculationMethod {
  def sigma: Double
}

sealed trait SpecIntMethod extends CalculationMethod {
  def sigma: Double
  def wavelength: Double
}

// Return the Signal-to-Noise given the number of exposures, exposure time, etc.
final case class ImagingS2N(
                    exposures: Int,
                    coadds: Option[Int],
                    exposureTime: Double,
                    sourceFraction: Double,
                    offset: Double) extends Imaging with S2NMethod

// Return the number of exposures (Integration Time) given the exposure time, desired S/N, etc.
final case class ImagingInt(
                    sigma: Double,
                    exposureTime: Double,
                    coadds: Option[Int],
                    sourceFraction: Double,
                    offset: Double) extends Imaging with IntMethod

// Return the exposure time and number of exposures given the desired S/N
final case class ImagingExp(
                    sigma: Double,
                    exposureTime: Double,
                    coadds: Option[Int],
                    sourceFraction: Double,
                    offset: Double) extends Imaging with ExpMethod

final case class SpectroscopyS2N(
                    exposures: Int,
                    coadds: Option[Int],
                    exposureTime: Double,
                    sourceFraction: Double,
                    offset: Double) extends Spectroscopy with S2NMethod

// Return the spectroscopic integration time (exposure time & number of exposures) given the desired S/N
final case class SpectroscopyInt(
                    sigma: Double,
                    wavelength: Double,
                    coadds: Option[Int],
                    exposureTime: Double,
                    exposures: Int,
                    sourceFraction: Double,
                    offset: Double) extends Spectroscopy with SpecIntMethod


// ==== Analysis method

sealed trait AnalysisMethod

sealed trait ApertureMethod extends AnalysisMethod {
  val skyAperture: Double   // area assumed to be on sky
}
final case class AutoAperture(skyAperture: Double) extends ApertureMethod
final case class UserAperture(diameter: Double, skyAperture: Double) extends ApertureMethod

// IFU analysis is currently supported by GMOS and NIFS
sealed trait IfuMethod extends AnalysisMethod {
  val skyFibres: Int        // # fibres (area) assumed to be on sky
}
final case class IfuSingle(skyFibres: Int, offset: Double) extends IfuMethod
final case class IfuRadial(skyFibres: Int, minOffset: Double, maxOffset: Double) extends IfuMethod
final case class IfuSummed(skyFibres: Int, numX: Int, numY: Int, centerX: Double, centerY: Double) extends IfuMethod // Sum of NIFS IFU elements in rectangular aperture
final case class IfuSum(skyFibres: Int, num: Double, isIfu2: Boolean) extends IfuMethod // Sum of GMOS fibers in circular aperture

// === Observation Details

final case class ObservationDetails(calculationMethod: CalculationMethod, analysisMethod: AnalysisMethod) {
  def exposureTime: Double    = calculationMethod.exposureTime
  def coadds: Option[Int] = calculationMethod.coadds
  def sourceFraction: Double  = calculationMethod.sourceFraction
  def offset: Double = calculationMethod.offset
  def isAutoAperture: Boolean = analysisMethod.isInstanceOf[AutoAperture]
}
