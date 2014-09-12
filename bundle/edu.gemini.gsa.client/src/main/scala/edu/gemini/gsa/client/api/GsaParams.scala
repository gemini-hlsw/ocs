package edu.gemini.gsa.client.api

import edu.gemini.model.p1.immutable._

sealed trait GsaParams {
  /** Instrument used to take the datasets. */
  def instrument: Instrument

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
      case _ => None
    }

  private def getGeminiParams(target: Target, blueprint: GeminiBlueprintBase): Option[GsaParams] =
    target match {
      case s: SiderealTarget    => Some(GsaSiderealParams(s.coords, blueprint.instrument))
      case n: NonSiderealTarget => Some(GsaNonSiderealParams(n.name, blueprint.instrument))
      case _                    => None
    }
}

/**
 * Sidereal target search params take a fixed ra/dec coordinate.
 */
case class GsaSiderealParams(coords: Coordinates, instrument: Instrument, limit: Int = 50) extends GsaParams

/**
 * Non-sidereal target search params just take the target name.
 */
case class GsaNonSiderealParams(targetName: String, instrument: Instrument, limit: Int = 50) extends GsaParams