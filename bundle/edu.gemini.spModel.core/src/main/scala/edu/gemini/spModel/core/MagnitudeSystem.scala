package edu.gemini.spModel.core

import scala.collection.JavaConversions._
import scalaz.Equal

sealed abstract class MagnitudeSystem(val name: String) extends Product with Comparable[MagnitudeSystem] with Serializable {

  // ===== LEGACY JAVA SUPPORT =====
  import MagnitudeSystem._

  def compareTo(other: MagnitudeSystem) = all.indexOf(other) - all.indexOf(this)

}

object MagnitudeSystem {

  case object VEGA   extends MagnitudeSystem("Vega")
  case object AB     extends MagnitudeSystem("AB")
  case object JY     extends MagnitudeSystem("Jy")
//  case object WATTS  extends MagnitudeSystem("W/m²/µm")
//  case object ERGSA  extends MagnitudeSystem("erg/s/cm²/Å")
//  case object ERGSHZ extends MagnitudeSystem("erg/s/cm²/Hz")

  val default:MagnitudeSystem = VEGA

  val all: List[MagnitudeSystem] =
    List(VEGA, AB, JY)

  implicit val MagnitudeSystemEqual: Equal[MagnitudeSystem] =
    Equal.equalA


  // ===== LEGACY JAVA SUPPORT =====
  val allAsJava = new java.util.Vector[MagnitudeSystem](all)
  val DEFAULT = default

  def valueOf(s: String) = all.find(_.name == s).get

}