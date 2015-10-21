package edu.gemini.spModel.core

sealed trait SpatialProfile extends Serializable
case object PointSource extends SpatialProfile
case object UniformSource extends SpatialProfile
final case class GaussianSource(fwhm: Double) extends SpatialProfile
