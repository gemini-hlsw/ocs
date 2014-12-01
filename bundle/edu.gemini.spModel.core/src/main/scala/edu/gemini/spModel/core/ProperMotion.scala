package edu.gemini.spModel.core

case class ProperMotion(
  deltaRA: Double,
  deltaDec: Double,
  epoch: Epoch,
  parallax: Double,
  rv: Double)

object ProperMotion {

  val zero = ProperMotion(0, 0, Epoch.J2000, 0, 0)

}

