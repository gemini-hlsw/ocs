package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.shared.Flamingos2Parameters
import edu.gemini.spModel.gemini.flamingos2.Flamingos2._

/**
 * F2 baseline test fixtures.
 */
object BaselineF2 {

  lazy val Fixtures = KBandImaging ++ KBandSpectroscopy

  private lazy val KBandImaging = Fixture.kBandImgFixtures(List(
    Flamingos2Parameters(
      Filter.OPEN,                        // filter
      Disperser.NONE,                     // grism
      FPUnit.FPU_NONE,                    // FP mask
      None,                               // custom slit width (for custom mask only)
      ReadMode.FAINT_OBJECT_SPEC),        // read mode
    Flamingos2Parameters(
      Filter.H,                           // filter
      Disperser.NONE,                     // grism
      FPUnit.FPU_NONE,                    // FP mask
      None,                               // custom slit width (for custom mask only)
      ReadMode.MEDIUM_OBJECT_SPEC),       // read mode
    Flamingos2Parameters(
      Filter.J_LOW,                       // filter
      Disperser.NONE,                     // grism
      FPUnit.FPU_NONE,                    // FP mask
      None,                               // custom slit width (for custom mask only)
      ReadMode.BRIGHT_OBJECT_SPEC)        // read mode
  ))

  private lazy val KBandSpectroscopy = Fixture.kBandSpcFixtures(List(
    Flamingos2Parameters(
      Filter.J_LOW,
      Disperser.R1200JH,
      FPUnit.LONGSLIT_1,
      None,
      ReadMode.FAINT_OBJECT_SPEC),
    Flamingos2Parameters(
      Filter.H,
      Disperser.R1200HK,
      FPUnit.LONGSLIT_4,
      None,
      ReadMode.MEDIUM_OBJECT_SPEC),
    Flamingos2Parameters(
      Filter.H,
      Disperser.R3000,
      FPUnit.CUSTOM_MASK,
      Some(CustomSlitWidth.CUSTOM_WIDTH_8_PIX),
      ReadMode.BRIGHT_OBJECT_SPEC)
  ))

}
