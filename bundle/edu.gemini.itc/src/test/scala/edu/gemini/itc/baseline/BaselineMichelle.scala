package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.shared.MichelleParameters
import edu.gemini.spModel.core.Wavelength
import edu.gemini.spModel.data.YesNoType
import edu.gemini.spModel.gemini.michelle.MichelleParams.Mask
import edu.gemini.spModel.gemini.michelle.MichelleParams.Filter
import edu.gemini.spModel.gemini.michelle.MichelleParams.Disperser

/**
 * Michelle baseline test fixtures.
 * Michelle is not in use anymore but science wants to keep the ITC functionality alive as a reference.
 */
object BaselineMichelle {

  lazy val Fixtures: List[Fixture[MichelleParameters]] = NBandImaging ++ NBandSpectroscopy ++ QBandSpectroscopy

  private lazy val NBandImaging = Fixture.nBandImgFixtures(List(
    new MichelleParameters(
      Filter.N_PRIME,               // Filter,
      Disperser.MIRROR,             // Grating,
      Wavelength.fromMicrons(12),   // instrumentCentralWavelength,
      Mask.MASK_IMAGING,            // FP_Mask,
      YesNoType.YES                 // polarimetry (enabled only allowed if imaging)
    )
  ))

  private lazy val NBandSpectroscopy = Fixture.nBandSpcFixtures(List(
    new MichelleParameters(
      Filter.N_PRIME,               // Filter,
      Disperser.HIGH_RES,           // Grating,
      Wavelength.fromMicrons(11),   // instrumentCentralWavelength,
      Mask.MASK_2,                  // FP_Mask,
      YesNoType.NO                  // polarimetry (enabled only allowed if imaging)
    )
  ))

  private lazy val QBandSpectroscopy = Fixture.qBandSpcFixtures(List(
    new MichelleParameters(
      Filter.QA,                    // Filter,
      Disperser.LOW_RES_20,         // Grating,
      Wavelength.fromMicrons(18.2), // instrumentCentralWavelength,
      Mask.MASK_4,                  // FP_Mask,
      YesNoType.NO                  // polarimetry (enabled only allowed if imaging)
    )
  ))
}
