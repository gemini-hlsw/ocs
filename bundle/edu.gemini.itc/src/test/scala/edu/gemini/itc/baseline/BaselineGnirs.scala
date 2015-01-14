package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.gnirs.{GnirsParameters, GnirsRecipe}

/**
 * GNIRS baseline test bits and pieces.
 */
object BaselineGnirs {

  lazy val Observations =
    // GNIRS spectroscopy observations
    for {
      odp <- Observation.SpectroscopyObservations
      ins <- spectroscopyParams()
    } yield GnirsObservation(odp, ins)

  lazy val Environments =
    for {
      src <- Environment.NearIRSources
      ocp <- Environment.ObservingConditions
      tep <- Environment.TelescopeConfigurations
      pdp <- Environment.PlottingParameters
    } yield Environment(src, ocp, tep, pdp)

  def executeRecipe(e: Environment, o: GnirsObservation): Output =
    cookRecipe(w => new GnirsRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, e.pdp, w))


  private def spectroscopyParams() = List(
    new GnirsParameters(
      GnirsParameters.LONG_CAMERA,
      GnirsParameters.G10,
      GnirsParameters.LOW_READ_NOISE,
      GnirsParameters.X_DISP_ON,
      "4.7",
      "2.4",
      GnirsParameters.SLIT0_1),

    new GnirsParameters(
      GnirsParameters.SHORT_CAMERA,
      GnirsParameters.G110,
      GnirsParameters.HIGH_READ_NOISE,
      GnirsParameters.X_DISP_OFF,
      "4.7",
      "2.4",
      GnirsParameters.SLIT0_2),

    new GnirsParameters(
      GnirsParameters.LONG_CAMERA,
      GnirsParameters.G32,
      GnirsParameters.LOW_READ_NOISE,
      GnirsParameters.X_DISP_ON,
      "4.7",
      "2.6",
      GnirsParameters.SLIT0_675),

    new GnirsParameters(
      GnirsParameters.SHORT_CAMERA,
      GnirsParameters.G32,
      GnirsParameters.HIGH_READ_NOISE,
      GnirsParameters.X_DISP_OFF,
      "4.7",
      "2.6",
      GnirsParameters.SLIT3_0)

  )


}
