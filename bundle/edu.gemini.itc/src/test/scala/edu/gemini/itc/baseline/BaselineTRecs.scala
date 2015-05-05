package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.trecs.TRecsParameters
import edu.gemini.spModel.core.Wavelength

/**
 * TRecs baseline test fixtures.
 * TRecs is not in use anymore but science wants to keep the ITC functionality alive as a reference.
 */
object BaselineTRecs {

  lazy val Fixtures = NBandSpectroscopy

  // NOTE: For TRecs sky background value must be equal to water vapor value.
  private lazy val TRecsObservingConditions = Fixture.ObservingConditions.filter(o => o.getSkyBackground == o.getSkyTransparencyWater)

  private lazy val NBandSpectroscopy = Fixture.nBandSpcFixtures(List(
    new TRecsParameters(
      "N",                                //String Filter,
      "KBr",                              //String cryostat window
      "HiRes-10",                         //String grating, ("none") for imaging
      Wavelength.fromMicrons(12),         //instrumentCentralWavelength
      TRecsParameters.SLIT0_21            //String FP_Mask,
    )
  ), TRecsObservingConditions)


}
