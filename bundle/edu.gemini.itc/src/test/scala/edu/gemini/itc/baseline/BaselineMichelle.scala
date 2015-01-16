package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.michelle.{MichelleParameters, MichelleRecipe}

/**
 * Michelle baseline test bits and pieces.
 * Michelle is not in use anymore but science wants to keep the ITC functionality alive as a reference.
 */
object BaselineMichelle {

  lazy val Observations =
    for {
      odp <- Observation.SpectroscopyObservations
      ins <- config()
    } yield MichelleObservation(odp, ins)

  lazy val Environments =
      for {
        src <- Environment.MidIRSources
        ocp <- Environment.ObservingConditions
        tep <- Environment.TelescopeConfigurations
        pdp <- Environment.PlottingParameters
      } yield Environment(src, ocp, tep, pdp)

  def executeRecipe(e: Environment, o: MichelleObservation): Output =
    cookRecipe(w => new MichelleRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, e.pdp, w))


  private def config() = List(
    // Michelle spectroscopy
    new MichelleParameters(
      "none",                             //String Filter,
      MichelleParameters.LOW_N,           //String grating,
      MichelleParameters.HIGH_READ_NOISE, //String readNoise,
      MichelleParameters.HIGH_WELL_DEPTH, //String wellDepth,
      "NOT_USED?",                        //String darkCurrent, TODO UNUSED?
      "777",                              //String instrumentCentralWavelength,
      MichelleParameters.SLIT0_19,        //String FP_Mask,
      "1",                                //String spatBinning,
      "1",                                //String specBinning
      MichelleParameters.DISABLED         //String polarimetry (enabled only allowed if imaging)
    )

  )


}
