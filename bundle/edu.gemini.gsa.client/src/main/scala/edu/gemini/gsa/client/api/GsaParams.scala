package edu.gemini.gsa.client.api

import edu.gemini.model.p1.immutable._
import edu.gemini.spModel.core.Coordinates

// To avoid circular dependencies we bridge the p1 Instrument to GSA in the class below
sealed trait GSAInstrument {
  def name: String
}

object GSAInstrument {
  // GSAInstrument is a key on a map, make it into a case class to have correct equals and hash code
  private case class GSAInstrumentImpl(name: String) extends GSAInstrument

  def apply(i: Instrument): GSAInstrument = i match {
      case Instrument.GmosNorth  => GSAInstrumentImpl("GMOS-N")
      case Instrument.Gnirs      => GSAInstrumentImpl("GNIRS")
      case Instrument.Michelle   => GSAInstrumentImpl("")
      case Instrument.Nifs       => GSAInstrumentImpl("NIFS")
      case Instrument.Niri       => GSAInstrumentImpl("NIRI")
      case Instrument.Dssi       => GSAInstrumentImpl("")
      case Instrument.Texes      => GSAInstrumentImpl("")
      case Instrument.Flamingos2 => GSAInstrumentImpl("F2")
      case Instrument.GmosSouth  => GSAInstrumentImpl("GMOS-S")
      case Instrument.Gpi        => GSAInstrumentImpl("GPI")
      case Instrument.Graces     => GSAInstrumentImpl("GRACES")
      case Instrument.Gsaoi      => GSAInstrumentImpl("GSAOI")
      case Instrument.Nici       => GSAInstrumentImpl("")
      case Instrument.Phoenix    => GSAInstrumentImpl("")
      case Instrument.Trecs      => GSAInstrumentImpl("")
      case Instrument.Visitor    => GSAInstrumentImpl("")
  }
}

sealed trait GsaParams {
  /** Instrument used to take the datasets. */
  def instrument: GSAInstrument

  /** Maximum number of results to return. */
  def limit: Int
}

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
      case _                      => None
    }

  private def getGeminiParams(target: Target, blueprint: GeminiBlueprintBase): Option[GsaParams] =
    target match {
      case s: SiderealTarget    => Some(GsaSiderealParams(s.coords, GSAInstrument(blueprint.instrument)))
      case n: NonSiderealTarget => Some(GsaNonSiderealParams(n.name, GSAInstrument(blueprint.instrument)))
      case _                    => None
    }
}

/**
 * Sidereal target search params take a fixed ra/dec coordinate.
 */
case class GsaSiderealParams(coords: Coordinates, instrument: GSAInstrument, limit: Int = 50) extends GsaParams

/**
 * Non-sidereal target search params just take the target name.
 */
case class GsaNonSiderealParams(targetName: String, instrument: GSAInstrument, limit: Int = 50) extends GsaParams