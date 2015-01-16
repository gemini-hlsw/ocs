package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.trecs.{TRecsParameters, TRecsRecipe}

/**
 * TRecs baseline test bits and pieces.
 * TRecs is not in use anymore but science wants to keep the ITC functionality alive as a reference.
 */
object BaselineTRecs {

  lazy val Observations =
    for {
      odp <- Observation.SpectroscopyObservations
      ins <- config()
    } yield TRecsObservation(odp, ins)


  lazy val Environments =
    for {
      src <- Environment.MidIRSources
      ocp <- Environment.ObservingConditions.filter(o => o.getSkyBackground == o.getSkyTransparencyWater)
      tep <- Environment.TelescopeConfigurations
      pdp <- Environment.PlottingParameters
    } yield Environment(src, ocp, tep, pdp)

  def executeRecipe(e: Environment, o: TRecsObservation): Output =
    cookRecipe(w => new TRecsRecipe(e.src, o.odp, e.ocp, o.ins, e.tep, e.pdp, w))

  private def config() = List(
    new TRecsParameters(
      "none",                             //String Filter,
      "KBr",                              //String Instrumentwindow
      "HiRes-10",                         //String grating, ("none") for imaging
      TRecsParameters.HIGH_READ_NOISE,    //String readNoise,
      TRecsParameters.HIGH_WELL_DEPTH,    //String wellDepth,
      "NOT_USED?",                        //String darkCurrent, TODO UNUSED?
      "777",                              //String instrumentCentralWavelength,
      TRecsParameters.SLIT0_21,           //String FP_Mask,
      "1",                                //String spatBinning,
      "1"                                 //String specBinning
    )

  )


}
