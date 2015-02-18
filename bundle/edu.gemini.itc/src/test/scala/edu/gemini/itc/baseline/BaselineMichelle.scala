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
      "777",                              //String instrumentCentralWavelength,
      MichelleParameters.SLIT0_19,        //String FP_Mask,
      "1",                                //String spatBinning,
      "1",                                //String specBinning
      MichelleParameters.DISABLED         //String polarimetry (enabled only allowed if imaging)
    )

  )

//  lazy val Fixtures = List(
//    (new SourceDefinitionParameters(
//      GaussianSource(1.0e-3, BrightnessUnit.MAG, 1.0),
//      BlackBody(10000.0),
//      WavebandDefinition.U,
//      0.0
//    ),
//      new ObservationDetailsParameters(
//        ImagingSN(0, 1800.0, 0.5),
//        AutoAperture(5.0)
//      ),
//      new MichelleParameters(
//        "Nprime",                           //String Filter,
//        "none",                             //String grating,
//        "12",                               //String instrumentCentralWavelength,
//        "none",                             //String FP_Mask,
//        "1",                                //String spatBinning, // TODO: always 1 can we remove this?
//        "1",                                //String specBinning  // TODO: always 1 can we remove this?
//        MichelleParameters.DISABLED         //String polarimetry (enabled only allowed if imaging)
//      )
//      )
//  )


}
