// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.p2checker.rules

import edu.gemini.p2checker.api.IRule
import edu.gemini.p2checker.rules.ghost.GhostRule
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.data.config.DefaultParameter
import edu.gemini.spModel.gemini.ghost.{Ghost, GhostMixin, SeqConfigGhost}


object GhostSpec extends RuleSpec {

  val ruleSet: IRule =
    GhostRule

  val AtTheLimit: Double =
    GhostRule.CosmicRayExposureRule.limitSeconds.toDouble

  val TooLong: Double =
    GhostRule.CosmicRayExposureRule.limitSeconds + 1.0

  "Exposure time rule" should {

    "not warn if instrument component red exposure time is not long" in {
      expectNoneOf(GhostRule.CosmicRayExposureRule.id) {
        advancedSetup[Ghost](GhostMixin.SP_TYPE) { (_, _, g, _) =>
          g.setRedExposureTime(AtTheLimit)
        }
      }
    }

    "warn if instrument component red exposure time is too long" in {
      expectAllOf(GhostRule.CosmicRayExposureRule.id) {
        advancedSetup[Ghost](GhostMixin.SP_TYPE) { (_, _, g, _) =>
          g.setRedExposureTime(TooLong)
        }
      }
    }

    "warn if instrument component blue exposure time is too long" in {
      expectAllOf(GhostRule.CosmicRayExposureRule.id) {
        advancedSetup[Ghost](GhostMixin.SP_TYPE) { (_, _, g, _) =>
          g.setBlueExposureTime(TooLong)
        }
      }
    }

    "warn if sequence step has long exposure time" in {
      expectAllOf(GhostRule.CosmicRayExposureRule.id) {
        advancedSetup[Ghost](GhostMixin.SP_TYPE) { (p, o, _, f) =>

          val seq = f.createSeqComponent(p, SeqConfigGhost.SP_TYPE, null)

          seq.getDataObject match {
            case sc: SeqConfigGhost =>
              val sysConfig = sc.getSysConfig
              val red       = DefaultParameter.getInstance(Ghost.RED_EXPOSURE_TIME_PROP, TooLong)
              sysConfig.putParameter(red)

              sc.setSysConfig(sysConfig)
              seq.setDataObject(sc)
          }

          seq.addSeqComponent(
            f.createSeqComponent(p, SPComponentType.OBSERVER_OBSERVE, null)
          )

          o.getSeqComponent.addSeqComponent(seq)
        }
      }
    }
  }

}
