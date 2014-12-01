package edu.gemini.spModel.core

case class ProperMotion(
  deltaRA: Double,
  deltaDec: Double,
  epoch: Epoch,
  parallax: Double,
  rv: Double,
  effectiveWavelength: ProperMotion.TrackingWavelength) // TODO: review

object ProperMotion {

  sealed trait TrackingWavelength
  final case class EffectiveWavelength(value: Double) extends TrackingWavelength
  case object Auto extends TrackingWavelength

  val zero = ProperMotion(0, 0, Epoch.J2000, 0, 0, EffectiveWavelength(0))

}

