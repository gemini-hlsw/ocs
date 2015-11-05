package edu.gemini.p2checker.rules

import edu.gemini.p2checker.rules.gmos.GmosRule
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.gemini.gmos._

/**
 * Some GMOS tests.
 */
final class GmosSpec extends RuleSpec {

  val ruleSet = new GmosRule()

  // === REL-2143: Don't allow E2V CCDs for GMOS-S programs after 2015B.

  "No E2V for GMOS-S after 2015B rule" should {

    val E2VErrId = "GmosRule_POST_2015B_GMOS_S_WITH_E2V_RULE"

    "give no error for unknown semester with E2V" in {
      expectNoneOf(E2VErrId) { setup[InstGmosSouth]("", SPComponentType.INSTRUMENT_GMOSSOUTH) { d =>
        d.setDetectorManufacturer(GmosCommonType.DetectorManufacturer.E2V)
      }}
    }

    "give no error for semester 2012A with E2V" in {
      expectNoneOf(E2VErrId) { setup[InstGmosSouth]("GS-2012A-Q-34", SPComponentType.INSTRUMENT_GMOSSOUTH) { d =>
        d.setDetectorManufacturer(GmosCommonType.DetectorManufacturer.E2V)
      }}
    }

    "give no error for semester 2015B with E2V" in {
      expectNoneOf(E2VErrId) { setup[InstGmosSouth]("GS-2015B-Q-34", SPComponentType.INSTRUMENT_GMOSSOUTH) { d =>
        d.setDetectorManufacturer(GmosCommonType.DetectorManufacturer.E2V)
      }}
    }

    "give an error for semester 2016A with E2V" in {
      expectAllOf(E2VErrId) { setup[InstGmosSouth]("GS-2016A-Q-34", SPComponentType.INSTRUMENT_GMOSSOUTH) { d =>
        d.setDetectorManufacturer(GmosCommonType.DetectorManufacturer.E2V)
      }}
    }

    "give an error for semester 2017A with E2V" in {
      expectAllOf(E2VErrId) { setup[InstGmosSouth]("GS-2017A-Q-34", SPComponentType.INSTRUMENT_GMOSSOUTH) { d =>
        d.setDetectorManufacturer(GmosCommonType.DetectorManufacturer.E2V)
      }}
    }

    "give no error for semester 2017A with Hamamatsu" in {
      expectNoneOf(E2VErrId) { setup[InstGmosSouth]("GS-2017A-Q-34", SPComponentType.INSTRUMENT_GMOSSOUTH) { d =>
        d.setDetectorManufacturer(GmosCommonType.DetectorManufacturer.HAMAMATSU)
      }}
    }

    "not affect GMOS-N for semester 2016A with E2V" in {
      expectNoneOf(E2VErrId) { setup[InstGmosNorth]("GN-2016A-Q-34", SPComponentType.INSTRUMENT_GMOS) { d =>
        d.setDetectorManufacturer(GmosCommonType.DetectorManufacturer.E2V)
      }}
    }
  }

}

