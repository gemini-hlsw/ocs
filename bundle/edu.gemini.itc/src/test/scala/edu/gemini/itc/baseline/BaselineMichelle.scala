package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.michelle.MichelleParameters

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
      "12",                               //String instrumentCentralWavelength,
      "none",                             //String FP_Mask,
      MichelleParameters.ENABLED          //String polarimetry (enabled only allowed if imaging)
    )
  ))

  private lazy val NBandSpectroscopy = Fixture.nBandSpcFixtures(List(
    new MichelleParameters(
      "Nprime",                           //String Filter,
      "medN2",                            //String grating,
      "11",                               //String instrumentCentralWavelength,
      "slit0.38",                         //String FP_Mask,
      MichelleParameters.DISABLED         //String polarimetry (enabled only allowed if imaging)
    )
  ))

  private lazy val QBandSpectroscopy = Fixture.qBandSpcFixtures(List(
    new MichelleParameters(
      "Qa",                               //String Filter,
      "lowQ",                             //String grating,
      "18.2",                             //String instrumentCentralWavelength,
      "slit0.76",                         //String FP_Mask,
      MichelleParameters.DISABLED         //String polarimetry (enabled only allowed if imaging)
    )
  ))
}
