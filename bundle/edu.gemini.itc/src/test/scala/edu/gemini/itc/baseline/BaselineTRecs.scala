package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.shared.TRecsParameters
import edu.gemini.spModel.gemini.trecs.TReCSParams.{Disperser, Filter, Mask, WindowWheel}
import edu.gemini.spModel.core.WavelengthConversions._

/**
 * TRecs baseline test fixtures.
 * TRecs is not in use anymore but science wants to keep the ITC functionality alive as a reference.
 */
object BaselineTRecs {

  lazy val Fixtures = NBandSpectroscopy

  // NOTE: For TRecs sky background value must be equal to water vapor value.
  private lazy val TRecsObservingConditions = Fixture.ObservingConditions.filter(o => o.sb.getPercentage == o.wv.getPercentage)

  private lazy val NBandSpectroscopy = Fixture.nBandSpcFixtures(List(
    new TRecsParameters(
      Filter.N,                           // Filter,
      WindowWheel.KBR,                    // Cryostat window
      Disperser.HIGH_RES,                 // Grating
      12.microns,                         // central wavelength
      Mask.MASK_1                         // FPU,
    )
  ), TRecsObservingConditions)


}
