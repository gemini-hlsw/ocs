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
      odp  <- Observation.SpectroscopyObservations
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
      NifsParameters.IFU,       // FP Mask
      NifsParameters.SINGLE_IFU,// IFU method
      "0",                      // offset
      "0.0",                    // min offset
      "0.0",                    // max offset
      "0",                      // num X
      "0",                      // num Y
      "0.0",                    // center x
      "0.0"                     // center y
    ),
    new NifsParameters(
      NifsParameters.HK_G0603,
      NifsParameters.H_G5604,
      NifsParameters.LOW_READ_NOISE,
      "2.2",                    // dark current: TODO: unused, remove
      "2.2",                    // central wavelength
      NifsParameters.IFU,       // FP Mask
      NifsParameters.SUMMED_APERTURE_IFU, // IFU method
      "0",                      // offset
      "0.0",                    // min offset
      "0.0",                    // max offset
      "2",                      // num X
      "5",                      // num Y
      "0.0",                    // center x
      "0.0"                     // center y
    )
  )

}
