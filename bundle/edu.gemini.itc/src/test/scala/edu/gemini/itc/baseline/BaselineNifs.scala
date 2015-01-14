package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.nifs.{NifsParameters, NifsRecipe}

/**
 * NIFS baseline test bits and pieces.
 */
object BaselineNifs {

  lazy val Observations =
    for {
      odp  <- Observation.ImagingObservations
      alt  <- Environment.AltairConfigurations
      conf <- configs()
    } yield NifsObservation(odp, conf, alt)

  lazy val Environments =
    for {
      src <- Environment.NearIRSources
      ocp <- Environment.ObservingConditions
      tep <- Environment.TelescopeConfigurations
      pdp <- Environment.PlottingParameters
    } yield Environment(src, ocp, tep, pdp)

  def executeRecipe(e: Environment, o: NifsObservation): Output =
    cookRecipe(w => new NifsRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, o.alt, e.pdp, w))

  private def configs() = List(
    new NifsParameters(
      NifsParameters.HK_G0603,
      NifsParameters.K_G5605,
      NifsParameters.LOW_READ_NOISE,
      "2.1",
      "2.1",
      NifsParameters.IFU,
      "singleIFU",
      "0",
      "0",
      "0.3",
      "nifsNorth")
  )

}
