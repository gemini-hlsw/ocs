package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.gems.GemsParameters
import edu.gemini.itc.gsaoi.{GsaoiParameters, GsaoiRecipe}

/**
 * GSAOI baseline test bits and pieces.
 */
object BaselineGsaoi  {

  lazy val Observations =
    for {
      odp  <- Observation.SpectroscopyObservations
      ins  <- config()
      gems <- gems()
    } yield GsaoiObservation(odp, ins, gems)

  lazy val Environments =
    for {
      src <- Environment.NearIRSources
      ocp <- Environment.ObservingConditions
      tep <- Environment.TelescopeConfigurations
      pdp <- Environment.PlottingParameters
    } yield Environment(src, ocp, tep, pdp)

  def executeRecipe(e: Environment, o: GsaoiObservation): Output =
    cookRecipe(w => new GsaoiRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, o.gems, w))

  private def gems() = List(
    new GemsParameters(
      0.3,
      "K"
    )
  )

  private def config() = List(
    new GsaoiParameters(
      "Z_G1101",                                    //String Filter,
      GsaoiParameters.INSTRUMENT_CAMERA,            //String camera,
      GsaoiParameters.BRIGHT_OBJECTS_READ_MODE      //String read mode,
    )

  )


}
