package edu.gemini.gsa.client.api

import edu.gemini.model.p1.immutable._
import edu.gemini.spModel.core.Coordinates

import scalaz._
import Scalaz._

// To avoid circular dependencies we bridge the p1 Instrument to GSA in the class below
sealed trait GSAInstrument {
  def name: String
}

object GSAInstrument {
  // GSAInstrument is a key on a map, make it into a case class to have correct equals and hash code
  private case class GSAInstrumentImpl(name: String) extends GSAInstrument

  def apply(i: Instrument): Option[GSAInstrument] = i match {
      case Instrument.Ghost      => GSAInstrumentImpl("GHOST").some
      case Instrument.GmosNorth  => GSAInstrumentImpl("GMOS-N").some
      case Instrument.Gnirs      => GSAInstrumentImpl("GNIRS").some
      case Instrument.Nifs       => GSAInstrumentImpl("NIFS").some
      case Instrument.Niri       => GSAInstrumentImpl("NIRI").some
      case Instrument.Flamingos2 => GSAInstrumentImpl("F2").some
      case Instrument.GmosSouth  => GSAInstrumentImpl("GMOS-S").some
      case Instrument.Gpi        => GSAInstrumentImpl("GPI").some
      case Instrument.Graces     => GSAInstrumentImpl("GRACES").some
      case Instrument.Gsaoi      => GSAInstrumentImpl("GSAOI").some
      case Instrument.Alopeke    => GSAInstrumentImpl("ALOPEKE").some
      case Instrument.Igrins     => GSAInstrumentImpl("IGRINS").some
      case Instrument.Zorro      => GSAInstrumentImpl("ZORRO").some
      case _                     => none // Instruments not in GSA
  }
}

sealed trait GsaParams

/**
 * Sidereal target search params take a fixed ra/dec coordinate.
 */
case class GsaSiderealParams(coords: Coordinates, instrument: GSAInstrument) extends GsaParams

/**
 * Non-sidereal target search params just take the target name.
 */
case class GsaNonSiderealParams(targetName: String, instrument: GSAInstrument) extends GsaParams

/**
 * GSA doesn't support all instruments
 */
case object GsaUnsupportedParams extends GsaParams

object GsaParams {
  /**
   * Extracts parameters from the given observation, if possible.  If there is
   * no target or blueprint, or if the target is ToO, then there are no
   * parameters matching that observation.
   */
  def get(obs: Observation): Option[GsaParams] =
    for {
      t <- obs.target
      b <- obs.blueprint
      q <- get(t, b)
    } yield q

  /**
   * Extracts the parameters for the given target (if not ToO) and blueprint.
   */
  def get(target: Target, blueprint: BlueprintBase): Option[GsaParams] =
    blueprint match {
      case g: GeminiBlueprintBase => getGeminiParams(target, g)
      case _                      => none
    }

  private def getGeminiParams(target: Target, blueprint: GeminiBlueprintBase): Option[GsaParams] =
    GSAInstrument(blueprint.instrument).fold(GsaUnsupportedParams.some : Option[GsaParams]) { i =>
      target match {
        case s: SiderealTarget    => GsaSiderealParams(s.coords, i).some
        case n: NonSiderealTarget => GsaNonSiderealParams(n.name, i).some
        case _                    => none
      }
    }
}

