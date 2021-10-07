package edu.gemini.p2checker.rules

import edu.gemini.p2checker.rules.gmos.GmosRule
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.gemini.gmos._
import edu.gemini.spModel.obsclass.ObsClass

/**
 * Some GMOS tests.
 */
final class GmosSpec extends RuleSpec {

  val ruleSet = new GmosRule()

  // === REL-2143: Don't allow E2V CCDs for GMOS-S programs after 2014A.

  "No E2V for GMOS-S after 2014A rule" should {

    import GmosCommonType.DetectorManufacturer._
    import SPComponentType._

    val E2VErrId = "GmosRule_POST_2014A_GMOS_S_WITH_E2V_RULE"

    "give no error for unknown semester with E2V" in {
      expectNoneOf(E2VErrId) { setup[InstGmosSouth](INSTRUMENT_GMOSSOUTH) { d =>
        d.setDetectorManufacturer(E2V)
      }}
    }

    "give no error for semester 2012A with E2V" in {
      expectNoneOf(E2VErrId) { setup[InstGmosSouth](INSTRUMENT_GMOSSOUTH, "GS-2012A-Q-34") { d =>
        d.setDetectorManufacturer(E2V)
      }}
    }

    "give no error for semester 2014A with E2V" in {
      expectNoneOf(E2VErrId) { setup[InstGmosSouth](INSTRUMENT_GMOSSOUTH, "GS-2014A-Q-34") { d =>
        d.setDetectorManufacturer(E2V)
      }}
    }

    "give an error for semester 2014B with E2V" in {
      expectAllOf(E2VErrId) { setup[InstGmosSouth](INSTRUMENT_GMOSSOUTH, "GS-2014B-Q-34") { d =>
        d.setDetectorManufacturer(E2V)
      }}
    }

    "give an error for semester 2017A with E2V" in {
      expectAllOf(E2VErrId) { setup[InstGmosSouth](INSTRUMENT_GMOSSOUTH, "GS-2017A-Q-34") { d =>
        d.setDetectorManufacturer(E2V)
      }}
    }

    "give an error for calibration for semester 2015A with E2V" in {
      expectAllOf(E2VErrId) { setup[InstGmosSouth](INSTRUMENT_GMOSSOUTH, "GS-CAL20150613") { d =>
        d.setDetectorManufacturer(E2V)
      }}
    }

    "give an error for engineering for semester 2015A with E2V" in {
      expectAllOf(E2VErrId) { setup[InstGmosSouth](INSTRUMENT_GMOSSOUTH, "GS-ENG20150222") { d =>
        d.setDetectorManufacturer(E2V)
      }}
    }

    "give no error for semester 2015A with Hamamatsu" in {
      expectNoneOf(E2VErrId) { setup[InstGmosSouth](INSTRUMENT_GMOSSOUTH, "GS-2015A-Q-34") { d =>
        d.setDetectorManufacturer(HAMAMATSU)
      }}
    }

    "not affect GMOS-N for semester 2015A with E2V" in {
      expectNoneOf(E2VErrId) { setup[InstGmosNorth](INSTRUMENT_GMOS, "GN-2015A-Q-34") { d =>
        d.setDetectorManufacturer(E2V)
      }}
    }
  }

  "GMOS N&S Configuration Rule" should {

    import GmosSouthType.DisperserSouth._
    import GmosSouthType.FPUnitSouth._

    import SPComponentType._

    val rule = "GmosRule_N_S_FPU_SPECTROSCOPIC_RULE"

    "give no error for properly configured N&S" in {
      expectNoneOf(rule) {
        setup[InstGmosSouth](INSTRUMENT_GMOSSOUTH) { d =>
          d.setUseNS(true)
          d.setDisperser(R150_G5326)
          d.setFPUnit(NS_1)
        }
      }
    }

    "warn for improperly configured N&S" in {
      expectAllOf(rule) {
        setup[InstGmosSouth](INSTRUMENT_GMOSSOUTH) { d =>
          d.setUseNS(true)
          d.setDisperser(R150_G5326)
          d.setFPUnit(LONGSLIT_1)
        }
      }
    }

    "warn even for calibrations" in {
      expectAllOf(rule) {
        advancedSetup[InstGmosSouth](INSTRUMENT_GMOSSOUTH) { (p, o, d, f) =>
          d.setUseNS(true)
          d.setDisperser(R150_G5326)
          d.setFPUnit(LONGSLIT_1)

          addObserve(ObsClass.PARTNER_CAL, p, o, f)
        }
      }
    }
  }


}

