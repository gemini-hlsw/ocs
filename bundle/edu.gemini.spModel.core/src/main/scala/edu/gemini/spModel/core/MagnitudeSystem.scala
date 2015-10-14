package edu.gemini.spModel.core

import scala.collection.JavaConversions._
import scalaz.Equal

sealed abstract class MagnitudeSystem(val name: String) extends Product with Serializable

object MagnitudeSystem {

  case object Vega   extends MagnitudeSystem("Vega")
  case object AB     extends MagnitudeSystem("AB")
  case object Jy     extends MagnitudeSystem("Jy")

  val default: MagnitudeSystem = Vega

  val all: List[MagnitudeSystem] =
    List(Vega, AB, Jy)

  implicit val MagnitudeSystemEqual: Equal[MagnitudeSystem] =
    Equal.equalA


  // ===== LEGACY JAVA SUPPORT =====
  val allAsJava = new java.util.Vector[MagnitudeSystem](all)
  val Default = default // default is a reserved key word in Java!

}