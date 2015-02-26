package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.niri.{NiriParameters, NiriRecipe}

/**
 * NIRI baseline test bits and pieces.
 */
object BaselineNiri {

  lazy val Observations =
    specObs() ++
    imgObs()

  lazy val Environments =
    for {
      src <- Environment.NearIRSources
      ocp <- Environment.ObservingConditions
      tep <- Environment.TelescopeConfigurations
      pdp <- Environment.PlottingParameters
    } yield Environment(src, ocp, tep, pdp)

  def executeRecipe(e: Environment, o: NiriObservation): Output =
    cookRecipe(w => new NiriRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, o.alt, e.pdp, w))

  // imaging
  private def imgObs() = for {
    odp  <- Observation.ImagingObservations
    alt  <- Environment.AltairConfigurations
    conf <- imagingConfigs()
  } yield NiriObservation(odp, conf, alt)

  private def imagingConfigs() = List(
    new NiriParameters(
      "J",
      "none",
      NiriParameters.F14,
      NiriParameters.HIGH_READ_NOISE,
      NiriParameters.HIGH_WELL_DEPTH,
      NiriParameters.NO_SLIT),

    new NiriParameters(
      "K",
      "none",
      NiriParameters.F32,
      NiriParameters.HIGH_READ_NOISE,
      NiriParameters.HIGH_WELL_DEPTH,
      NiriParameters.NO_SLIT)
  )

  // spectroscopy
  private def specObs() = for {
    odp  <- Observation.SpectroscopyObservations
    conf <- spectroscopyConfigs()
  } yield NiriObservation(odp, conf, Environment.NoAltair)

  private def spectroscopyConfigs() = List(
    new NiriParameters(
      "K",
      "K-grism",
      NiriParameters.F6,                      // only F6 is supported in spectroscopy mode
      NiriParameters.LOW_READ_NOISE,
      NiriParameters.LOW_WELL_DEPTH,
      NiriParameters.SLIT_2_PIX_CENTER)
  )

}
