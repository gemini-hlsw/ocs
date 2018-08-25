package edu.gemini.itc.shared

import edu.gemini.spModel.core.{SpectralDistribution, UniformSource, SpatialProfile, Redshift, BrightnessUnit, MagnitudeBand}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{WaterVapor, CloudCover, SkyBackground, ImageQuality}

// ==== Observing conditions

final case class ObservingConditions(
                      iq: ImageQuality,
                      cc: CloudCover,
                      wv: WaterVapor,
                      sb: SkyBackground,
                      airmass: Double,
                      exactiq: Double,
                      exactcc: Double)

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

final case class ImagingS2N(
                    exposures: Int,
                    coadds: Option[Int],
                    exposureTime: Double,
                    sourceFraction: Double,
                    offset: Double) extends Imaging with S2NMethod

final case class ImagingInt(
                    sigma: Double,
                    exposureTime: Double,
                    coadds: Option[Int],
                    sourceFraction: Double,
                    offset: Double) extends Imaging with IntMethod

final case class SpectroscopyS2N(
                    exposures: Int,
                    coadds: Option[Int],
                    exposureTime: Double,
                    sourceFraction: Double,
                    offset: Double) extends Spectroscopy with S2NMethod


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
