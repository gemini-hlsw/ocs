package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.flamingos2.{Flamingos2Parameters, Flamingos2Recipe}

/**
 * F2 baseline test bits and pieces.
 */
object BaselineF2 {

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


  def executeRecipe(e: Environment, o: F2Observation): Output =
    cookRecipe(w => new Flamingos2Recipe(e.src, o.odp, e.ocp, o.ins, e.tep, e.pdp, w))

  // F2 imaging observations
  private def imgObs() = for {
    odp  <- Observation.ImagingObservations
    conf <- imagingConfigs()
  } yield F2Observation(odp, conf)

  private def imagingConfigs() = List(
    new Flamingos2Parameters(
      "H_G0803",                          // filter
      Flamingos2Parameters.NOGRISM,       // grism
      "none",                             // FP mask
      "lowNoise")                         // read noise
  )

  // F2 spectroscopy observations
  private def specObs() = for {
    odp  <- Observation.SpectroscopyObservations
    conf <- spectroscopyConfigs()
  } yield F2Observation(odp, conf)

  private def spectroscopyConfigs() = List(
    new Flamingos2Parameters(
      "H_G0803",
      "R3K_G5803",
      "1",
      "medNoise")
  )

}
