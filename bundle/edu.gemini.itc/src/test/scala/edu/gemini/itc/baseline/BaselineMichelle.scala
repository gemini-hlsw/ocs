package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.shared.MichelleParameters
import edu.gemini.spModel.data.YesNoType
import edu.gemini.spModel.gemini.michelle.MichelleParams.{Disperser, Filter, Mask}
import edu.gemini.spModel.core.WavelengthConversions._

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
      12.microns,                   // central wavelength,
      Mask.MASK_IMAGING,            // FP_Mask,
      YesNoType.YES                 // polarimetry (enabled only allowed if imaging)
    )
  ))

  private lazy val NBandSpectroscopy = Fixture.nBandSpcFixtures(List(
    new MichelleParameters(
      Filter.N_PRIME,               // Filter,
      Disperser.HIGH_RES,           // Grating,
      11.microns,                   // central wavelength,
      Mask.MASK_2,                  // FP_Mask,
      YesNoType.NO                  // polarimetry (enabled only allowed if imaging)
    )
  ))

  private lazy val QBandSpectroscopy = Fixture.qBandSpcFixtures(List(
    new MichelleParameters(
      Filter.QA,                    // Filter,
      Disperser.LOW_RES_20,         // Grating,
      18.2.microns,                 // central wavelength,
      Mask.MASK_4,                  // FP_Mask,
      YesNoType.NO                  // polarimetry (enabled only allowed if imaging)
    )
  ))
}
