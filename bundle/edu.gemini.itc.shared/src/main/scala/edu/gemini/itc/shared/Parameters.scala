package edu.gemini.itc.shared

import edu.gemini.spModel.core.{BrightnessUnit, MagnitudeBand}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{WaterVapor, CloudCover, SkyBackground, ImageQuality}
import edu.gemini.spModel.target.{UniformSource, SpectralDistribution, SpatialProfile}

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
                     redshift: Double) {

  val isUniform = profile match {
    case UniformSource => true
    case _             => false
  }
}

// ==== Calculation method

// TODO: We can probably get away with only IntegrationTime and S2N methods.
// TODO: The difference between spectroscopy and imaging can/should be deduced from the instrument settings!

sealed trait CalculationMethod {
  val fraction: Double
  val isIntTime: Boolean
  def isS2N: Boolean = !isIntTime
  val isImaging: Boolean
  def isSpectroscopy: Boolean = !isImaging
}
sealed trait Imaging extends CalculationMethod {
  val isImaging = true
}
sealed trait Spectroscopy extends CalculationMethod {
  val isImaging = false
}
final case class ImagingSN(exposures: Int, time: Double, fraction: Double) extends Imaging {
  val isIntTime = false
}
final case class ImagingInt(sigma: Double, expTime: Double, fraction: Double) extends Imaging {
  val isIntTime = true
}
final case class SpectroscopySN(exposures: Int, time: Double, fraction: Double) extends Spectroscopy {
  val isIntTime = false
}


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


// ===== RESULTS
final case class Parameters(source: SourceDefinition, observation: ObservationDetails, conditions: ObservingConditions, telescope: TelescopeDetails)

