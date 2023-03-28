// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.p2checker.rules

import edu.gemini.p2checker.api.IRule
import edu.gemini.p2checker.rules.igrins2.Igrins2Rule
import edu.gemini.spModel.gemini.igrins2.{Igrins2, Igrins2Mixin}

object Igrins2Spec extends RuleSpec {

  val ruleSet: IRule =
    Igrins2Rule

  "Exposure time rule" should {

    "not warn if instrument component exposure time is in range" in {
      expectNoneOf(Igrins2Rule.ExposureTimeRule.TooShort, Igrins2Rule.ExposureTimeRule.TooLong) {
        advancedSetup[Igrins2](Igrins2Mixin.SP_TYPE) { (_, _, g, _) =>
          g.setExposureTime(2.0)
        }
      }
    }

    "warn if instrument component exposure time is too long" in {
      expectAllOf(Igrins2Rule.ExposureTimeRule.TooLong) {
        advancedSetup[Igrins2](Igrins2.SP_TYPE) { (_, _, g, _) =>
          g.setExposureTime(2000)
        }
      }
    }

    "warn if instrument component exposure time is too short" in {
      expectAllOf(Igrins2Rule.ExposureTimeRule.TooShort) {
        advancedSetup[Igrins2](Igrins2.SP_TYPE) { (_, _, g, _) =>
          g.setExposureTime(0.1)
        }
      }
    }
  }

}
