package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._
import edu.gemini.itc.michelle.{MichelleParameters, MichelleRecipe}

/**
 * Michelle baseline test fixtures.
 * Michelle is not in use anymore but science wants to keep the ITC functionality alive as a reference.
 */
object BaselineMichelle {

  lazy val Fixtures: List[Fixture[MichelleParameters]] = NBandImaging ++ NBandSpectroscopy ++ QBandSpectroscopy

  def executeRecipe(f: Fixture[MichelleParameters]): Output =
    cookRecipe(w => new MichelleRecipe(f.src, f.odp, f.ocp, f.ins, f.tep, f.pdp, w))

  private lazy val NBandImaging = Fixture.nBandImgFixtures(List(
    new MichelleParameters(
      "Nprime",                           //String Filter,
      "none",                             //String grating,
      "12",                               //String instrumentCentralWavelength,
      "none",                             //String FP_Mask,
      "1",                                //String spatBinning, // TODO: always 1 can we remove this?
      "1",                                //String specBinning  // TODO: always 1 can we remove this?
      MichelleParameters.ENABLED         //String polarimetry (enabled only allowed if imaging)
    )
  ))

  private lazy val NBandSpectroscopy = Fixture.nBandSpcFixtures(List(
    new MichelleParameters(
      "Nprime",                           //String Filter,
      "medN2",                            //String grating,
      "11",                               //String instrumentCentralWavelength,
      "slit0.38",                         //String FP_Mask,
      "1",                                //String spatBinning, // TODO: always 1 can we remove this?
      "1",                                //String specBinning  // TODO: always 1 can we remove this?
      MichelleParameters.DISABLED         //String polarimetry (enabled only allowed if imaging)
    )
  ))

  private lazy val QBandSpectroscopy = Fixture.qBandSpcFixtures(List(
    new MichelleParameters(
      "Qa",                               //String Filter,
      "lowQ",                             //String grating,
      "18.2",                             //String instrumentCentralWavelength,
      "slit0.76",                         //String FP_Mask,
      "1",                                //String spatBinning, // TODO: always 1 can we remove this?
      "1",                                //String specBinning  // TODO: always 1 can we remove this?
      MichelleParameters.DISABLED         //String polarimetry (enabled only allowed if imaging)
    )
  ))
}
