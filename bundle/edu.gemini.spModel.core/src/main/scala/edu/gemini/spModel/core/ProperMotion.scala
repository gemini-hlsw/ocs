package edu.gemini.spModel.core

case class ProperMotion(
  deltaRA: Double,
  deltaDec: Double,
  epoch: Epoch = Epoch.J2000,
  parallax: Option[Double] = None,
  rv: Option[Double] = None)

object ProperMotion {

  val zero = ProperMotion(0, 0, Epoch.J2000, None, None)

}

