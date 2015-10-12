package edu.gemini.spModel.core

import scala.collection.JavaConversions._
import scalaz.Equal

/** Unit that can represent an integrated brightness or a uniform surface brightness per area. */
sealed trait BrightnessUnit {
  val name: String
  def displayValue: String = name
}

sealed abstract class MagnitudeSystem(val name: String) extends BrightnessUnit with Product with Serializable

sealed abstract class SurfaceBrightness(val name: String) extends BrightnessUnit with Product with Serializable

/** Units for integrated brightness values.
  * Note that strictly speaking these are not all magnitudes, only Vega Mag and AB Mag are magnitudes, but
  * for historical reasons all of these units are called magnitudes. A better term for these units would be
  * integrated brightness units.
  */
object MagnitudeSystem {

  case object Vega                extends MagnitudeSystem("Vega")
  case object AB                  extends MagnitudeSystem("AB")
  case object Jy                  extends MagnitudeSystem("Jy")

  // currently not supporte in OT, only available in web app
  case object Watts               extends SurfaceBrightness("W/m²/µm")
  case object ErgsWavelength      extends SurfaceBrightness("erg/s/cm²/Å")
  case object ErgsFrequency       extends SurfaceBrightness("erg/s/cm²/Hz")


  val default: MagnitudeSystem = Vega

  val all: List[MagnitudeSystem] =
    List(Vega, AB, Jy)

  implicit val MagnitudeSystemEqual: Equal[MagnitudeSystem] =
    Equal.equalA


  // ===== LEGACY JAVA SUPPORT =====
  val allAsJava = new java.util.Vector[MagnitudeSystem](all)
  val Default = default // default is a reserved key word in Java!

}

/** Units for uniform surface brightness values. */
object SurfaceBrightness {

  case object Vega                extends SurfaceBrightness("Vega/arcsec²")
  case object AB                  extends SurfaceBrightness("AB/arcsec²")
  case object Jy                  extends SurfaceBrightness("Jy/arcsec²")
  case object Watts               extends SurfaceBrightness("W/m²/µm/arcsec²")
  case object ErgsWavelength      extends SurfaceBrightness("erg/s/cm²/Å/arcsec²")
  case object ErgsFrequency       extends SurfaceBrightness("erg/s/cm²/Hz/arcsec²")

  implicit val SurfaceBrightnessEqual: Equal[SurfaceBrightness] =
    Equal.equalA

}

