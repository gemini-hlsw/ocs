package edu.gemini.model.p1.dtree.inst

import org.specs2.mutable.SpecificationWithJUnit
import edu.gemini.model.p1.immutable.AltairLGS
import edu.gemini.model.p1.immutable

class GnirsSpec extends SpecificationWithJUnit {
  "The Gnirs decision tree" should {
    "includes X filter in order, REL-630" in {
      val gnirs = new Gnirs.ImagingNode((AltairLGS(true), immutable.GnirsPixelScale.PS_005))
      gnirs.choices must have size(7)
      // Respect wavelength order
      gnirs.choices(0).value() must beEqualTo("Y (1.03um)")
      gnirs.choices(1).value() must beEqualTo("X (1.10um)")
    }
    "include a central wavelength option, REL-1254" in {
      val gnirs = new Gnirs.CentralWavelengthNode((AltairLGS(true), immutable.GnirsPixelScale.PS_005, immutable.GnirsDisperser.D_10, immutable.GnirsCrossDisperser.LXD, immutable.GnirsFpu.values(0)))
      gnirs.choices must have size(2)
      // Respect wavelength order
      gnirs.choices(0).value() must beEqualTo("< 2.5um")
      gnirs.choices(1).value() must beEqualTo(">=2.5um")
    }
  }

}
