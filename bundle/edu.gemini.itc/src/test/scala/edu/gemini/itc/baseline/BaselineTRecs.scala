package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.shared.TRecsParameters
import edu.gemini.spModel.core.Wavelength
import edu.gemini.spModel.gemini.trecs.TReCSParams.Disperser
import edu.gemini.spModel.gemini.trecs.TReCSParams.Filter
import edu.gemini.spModel.gemini.trecs.TReCSParams.Mask
import edu.gemini.spModel.gemini.trecs.TReCSParams.WindowWheel

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
      Filter.N,                           // Filter,
      WindowWheel.KBR,                    // Cryostat window
      Disperser.HIGH_RES,                 // Grating
      Wavelength.fromMicrons(12),         // instrumentCentralWavelength
      Mask.MASK_1                         // FPU,
    )
  ), TRecsObservingConditions)


}
