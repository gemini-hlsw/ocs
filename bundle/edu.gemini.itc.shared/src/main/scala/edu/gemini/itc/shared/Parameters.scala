package edu.gemini.itc.shared

import edu.gemini.spModel.core.{SpectralDistribution, UniformSource, SpatialProfile, Redshift, BrightnessUnit, MagnitudeBand}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{WaterVapor, CloudCover, SkyBackground, ImageQuality}

// ==== Observing conditions

final case class ObservingConditions(
                      iq: ImageQuality,
                      cc: CloudCover,
                      wv: WaterVapor,
                      sb: SkyBackground,
                      airmass: Double)

// ==== Source definition
final case class SourceDefinition(
                     profile: SpatialProfile,
                     distribution: SpectralDistribution,
                     norm: Double,
                     units: BrightnessUnit,
                     normBand: MagnitudeBand,
                     redshift: Redshift) {

  val isUniform = profile match {
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
  def sourceFraction: Double
}

sealed trait S2NMethod extends CalculationMethod {
  def exposures: Int
}

sealed trait IntMethod extends CalculationMethod {
  def sigma: Double
}

final case class ImagingS2N(
                    exposures: Int,
                    exposureTime: Double,
                    sourceFraction:
                    Double) extends Imaging with S2NMethod

final case class ImagingInt(
                    sigma: Double,
                    exposureTime: Double,
                    sourceFraction: Double) extends Imaging with IntMethod

final case class SpectroscopyS2N(
                    exposures: Int,
                    exposureTime: Double,
                    sourceFraction: Double) extends Spectroscopy with S2NMethod


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
final case class IfuSummed(skyFibres: Int, numX: Int, numY: Int, centerX: Double, centerY: Double) extends IfuMethod
final case class IfuSum(skyFibres: Int, num: Double, isIfu2: Boolean) extends IfuMethod

// === Observation Details

final case class ObservationDetails(calculationMethod: CalculationMethod, analysisMethod: AnalysisMethod) {
  def exposureTime   = calculationMethod.exposureTime
  def sourceFraction = calculationMethod.sourceFraction
  def isAutoAperture = analysisMethod.isInstanceOf[AutoAperture]
}
