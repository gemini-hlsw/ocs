package edu.gemini.spModel.core

import scala.collection.JavaConversions._
import scalaz.Equal

/** Unit that can represent an integrated brightness or a uniform surface brightness per area. */
sealed trait BrightnessUnit extends Product with Serializable {
  val name: String
  def displayValue: String = name
}

sealed abstract class MagnitudeSystem(val name: String) extends BrightnessUnit

sealed abstract class SurfaceBrightness(val name: String) extends BrightnessUnit

/** Units for integrated brightness values.
  * Note that strictly speaking these are not all magnitudes, only Vega Mag and AB Mag are magnitudes, but
  * for historical reasons all of these units are called magnitudes. A better term for these units would be
  * integrated brightness units.
  */
object MagnitudeSystem {

  case object Vega                extends MagnitudeSystem("Vega")
  case object AB                  extends MagnitudeSystem("AB")
  case object Jy                  extends MagnitudeSystem("Jy")

  // currently not supported in OT, only available in ITC web app
  case object Watts               extends MagnitudeSystem("W/m²/µm")
  case object ErgsWavelength      extends MagnitudeSystem("erg/s/cm²/Å")
  case object ErgsFrequency       extends MagnitudeSystem("erg/s/cm²/Hz")


  val default: MagnitudeSystem = Vega

  val all: List[MagnitudeSystem] =
    List(Vega, AB, Jy, Watts, ErgsWavelength, ErgsFrequency)

  // this is used in the OT, for now we only support Vega, AB and Jy in the OT
  val allForOT: List[MagnitudeSystem] =
    List(Vega, AB, Jy)

  def fromString(s: String): Option[MagnitudeSystem] = s match {
    // these strings are currently used to represent MagnitudeSystem values in the ITC web forms
    // in case they need to be changed, please also update the science regression test scripts
    case "MAG"                  => Some(MagnitudeSystem.Vega)
    case "ABMAG"                => Some(MagnitudeSystem.AB)
    case "JY"                   => Some(MagnitudeSystem.Jy)
    case "WATTS"                => Some(MagnitudeSystem.Watts)
    case "ERGS_WAVELENGTH"      => Some(MagnitudeSystem.ErgsWavelength)
    case "ERGS_FREQUENCY"       => Some(MagnitudeSystem.ErgsFrequency)
    case _                      => None
  }

  def unsafeFromString(s: String): MagnitudeSystem = fromString(s).get

  implicit val MagnitudeSystemEqual: Equal[MagnitudeSystem] =
    Equal.equalA

  // ===== LEGACY JAVA SUPPORT =====
  val allForOTAsJava = new java.util.Vector[MagnitudeSystem](allForOT)
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

  def fromString(s: String): Option[SurfaceBrightness] = s match {
    // these strings are currently used to represent SurfaceBrightness values in the ITC web forms
    // in case they need to be changed, please also update the science regression test scripts
    case "MAG_PSA"              => Some(SurfaceBrightness.Vega)
    case "ABMAG_PSA"            => Some(SurfaceBrightness.AB)
    case "JY_PSA"               => Some(SurfaceBrightness.Jy)
    case "WATTS_PSA"            => Some(SurfaceBrightness.Watts)
    case "ERGS_WAVELENGTH_PSA"  => Some(SurfaceBrightness.ErgsWavelength)
    case "ERGS_FREQUENCY_PSA"   => Some(SurfaceBrightness.ErgsFrequency)
    case _                      => None
  }

  def unsafeFromString(s: String): SurfaceBrightness = fromString(s).get

  implicit val SurfaceBrightnessEqual: Equal[SurfaceBrightness] =
    Equal.equalA

}

