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

sealed trait AnalysisMethod {
  val skyAperture: Double
}
final case class AutoAperture(skyAperture: Double) extends AnalysisMethod
final case class UserAperture(diameter: Double, skyAperture: Double) extends AnalysisMethod


// ===== IFU (GMOS & NIFS)

// TODO: Is this an analysis method (instead of the ones above?). If so, should this be reflected here?
sealed trait IfuMethod
final case class IfuSingle(offset: Double) extends IfuMethod
final case class IfuRadial(minOffset: Double, maxOffset: Double) extends IfuMethod
final case class IfuSummed(numX: Int, numY: Int, centerX: Double, centerY: Double) extends IfuMethod


// === Observation Details

final case class ObservationDetails(calculationMethod: CalculationMethod, analysisMethod: AnalysisMethod) {
  def exposureTime   = calculationMethod.exposureTime
  def sourceFraction = calculationMethod.sourceFraction
  def isAutoAperture = analysisMethod.isInstanceOf[AutoAperture]
  def skyAperture    = analysisMethod.skyAperture
}
