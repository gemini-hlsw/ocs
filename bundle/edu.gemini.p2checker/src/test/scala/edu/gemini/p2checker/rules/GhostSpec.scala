// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.p2checker.rules

import edu.gemini.p2checker.api.IRule
import edu.gemini.p2checker.rules.ghost.GhostRule
import edu.gemini.p2checker.rules.ghost.GhostRule.CoordinatesOutOfFOVRule
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.core.AngularVelocity
import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.core.Declination
import edu.gemini.spModel.core.DeclinationAngularVelocity
import edu.gemini.spModel.core.ProperMotion
import edu.gemini.spModel.core.RightAscension
import edu.gemini.spModel.core.RightAscensionAngularVelocity
import edu.gemini.spModel.core.SiderealTarget
import edu.gemini.spModel.core.Target
import edu.gemini.spModel.data.config.DefaultParameter
import edu.gemini.spModel.gemini.ghost.GhostAsterism
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GhostTarget
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GuideFiberState
import edu.gemini.spModel.gemini.ghost.GhostAsterism.createEmptyAsterism
import edu.gemini.spModel.gemini.ghost.{Ghost, GhostMixin, SeqConfigGhost}
import edu.gemini.spModel.obs.SchedulingBlock
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.target.SPCoordinates
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.TargetObsComp

import java.time._
import java.util.TimeZone

object GhostSpec extends RuleSpec {

  val ruleSet: IRule =
    GhostRule

  "CoordinatesOutOfRange rule" should {
    val highPmBase: GhostTarget = {
      val t = SiderealTarget(
        name         = "Zero",
        coordinates  = Coordinates.zero,
        properMotion = Some(ProperMotion(
          RightAscensionAngularVelocity(AngularVelocity(10000.0)),
          DeclinationAngularVelocity(AngularVelocity(0.0))
        )),
        redshift     = None,
        parallax     = None,
        magnitudes   = Nil,
        spectralDistribution = None,
        spatialProfile       = None
      )
      GhostTarget(new SPTarget(t), GuideFiberState.Enabled)
    }

    val Jan1_2023: SchedulingBlock = {
      val ldt  = LocalDateTime.of(2023, Month.JANUARY, 1, 0, 0, 0)
      val when = ldt.atZone(ZoneId.of("UTC")).toInstant
      new SchedulingBlock(when.toEpochMilli, SchedulingBlock.Duration.Explicit(1000))
    }

    def modTargetEnvironment(
      o: ISPObservation
    )(
      f: TargetEnvironment => TargetEnvironment
    ): Unit =
      o.findObsComponentByType(TargetObsComp.SP_TYPE).foreach { oc =>
        val toc = oc.getDataObject.asInstanceOf[TargetObsComp]
        toc.setTargetEnvironment(f(toc.getTargetEnvironment))
        oc.setDataObject(toc)
      }

    def modObs(
      o: ISPObservation
    )(
      f: SPObservation => Unit
    ): Unit = {
      val obs = o.getDataObject.asInstanceOf[SPObservation]
      f(obs)
      o.setDataObject(obs)
    }

    "warn if the base position moves out of range of an IFU" in {
      expectAllOf(CoordinatesOutOfFOVRule.id) {
        advancedSetup[Ghost](GhostMixin.SP_TYPE) { (_, o, _, _) =>
          modTargetEnvironment(o) { env =>
            env.setAsterism(
              // SFU2 is ~ 230" away from PM corrected base on Jan1_2023
              GhostAsterism.TargetPlusSky(highPmBase, SPCoordinates.zero, None)
            )
          }
          modObs(o)(_.setSchedulingBlockSome(Jan1_2023))
        }
      }
    }

    "warn if the separation between the IFU probes is not at least 102 arcsec" in {
      expectAllOf(CoordinatesOutOfFOVRule.id) {
        advancedSetup[Ghost](GhostMixin.SP_TYPE) { (_, o, _, _) =>
          modTargetEnvironment(o) { env =>
            // IFU1 / IFU2 separation just under 102" on Jan1_2023
            val coords = Coordinates(RightAscension.fromAngle(Angle.fromArcsecs(127)), Declination.zero)
            env.setAsterism(
              GhostAsterism.TargetPlusSky(highPmBase, new SPCoordinates(coords), None)
            )
          }
          modObs(o)(_.setSchedulingBlockSome(Jan1_2023))
        }
      }
    }

    "no warning if the separation between the IFU probes over 102 and both are in range of base" in {
      expectNoneOf(CoordinatesOutOfFOVRule.id) {
        advancedSetup[Ghost](GhostMixin.SP_TYPE) { (_, o, _, _) =>
          modTargetEnvironment(o) { env =>
            // IFU1 / IFU2 separation just under 128" on Jan1_2023
            val coords = Coordinates(RightAscension.fromAngle(Angle.fromArcsecs(100)), Declination.zero)
            env.setAsterism(
              GhostAsterism.TargetPlusSky(highPmBase, new SPCoordinates(coords), None)
            )
          }
          modObs(o)(_.setSchedulingBlockSome(Jan1_2023))
        }
      }
    }
  }

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
