package edu.gemini.spModel.target

sealed trait SpatialProfile extends Serializable
final case class PointSource() extends SpatialProfile
final case class GaussianSource(fwhm: Double) extends SpatialProfile
final case class UniformSource() extends SpatialProfile
