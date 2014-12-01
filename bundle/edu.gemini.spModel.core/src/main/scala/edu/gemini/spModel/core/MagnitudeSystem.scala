package edu.gemini.spModel.core

import scalaz.Equal

sealed abstract class MagnitudeSystem(val name: String) extends Product with Serializable

object MagnitudeSystem {

  case object VEGA extends MagnitudeSystem("Vega")
  case object AB   extends MagnitudeSystem("AB")
  case object JY   extends MagnitudeSystem("Jy")

  val all: List[MagnitudeSystem] =
    List(VEGA, AB, JY)

  implicit val MagnitudeSystemEqual: Equal[MagnitudeSystem] =
    Equal.equalA

}