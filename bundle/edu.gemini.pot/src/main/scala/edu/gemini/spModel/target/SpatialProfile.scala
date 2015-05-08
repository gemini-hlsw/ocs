package edu.gemini.spModel.target

sealed trait SpatialProfile extends Serializable
final case class PointSource() extends SpatialProfile
final case class GaussianSource(fwhm: Double) extends SpatialProfile {
  require (fwhm >= 0.1, "Please use a Gaussian FWHM greater than 0.1")
}
final case class UniformSource() extends SpatialProfile
