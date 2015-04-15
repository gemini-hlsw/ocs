package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.michelle.MichelleParameters
import edu.gemini.spModel.gemini.michelle.MichelleParams.Mask

/**
 * Michelle baseline test fixtures.
 * Michelle is not in use anymore but science wants to keep the ITC functionality alive as a reference.
 */
object BaselineMichelle {

  lazy val Fixtures: List[Fixture[MichelleParameters]] = NBandImaging ++ NBandSpectroscopy ++ QBandSpectroscopy

  private lazy val NBandImaging = Fixture.nBandImgFixtures(List(
    new MichelleParameters(
      "Nprime",                           //String Filter,
      "none",                             //String grating,
      12,                                 //instrumentCentralWavelength,
      Mask.MASK_IMAGING,                  //FP_Mask,
      MichelleParameters.ENABLED          //String polarimetry (enabled only allowed if imaging)
    )
  ))

  private lazy val NBandSpectroscopy = Fixture.nBandSpcFixtures(List(
    new MichelleParameters(
      "Nprime",                           //String Filter,
      "medN2",                            //String grating,
      11,                                 //instrumentCentralWavelength,
      Mask.MASK_2,                        //FP_Mask,
      MichelleParameters.DISABLED         //String polarimetry (enabled only allowed if imaging)
    )
  ))

  private lazy val QBandSpectroscopy = Fixture.qBandSpcFixtures(List(
    new MichelleParameters(
      "Qa",                               //String Filter,
      "lowQ",                             //String grating,
      18.2,                               //instrumentCentralWavelength,
      Mask.MASK_4,                        //FP_Mask,
      MichelleParameters.DISABLED         //String polarimetry (enabled only allowed if imaging)
    )
  ))
}
