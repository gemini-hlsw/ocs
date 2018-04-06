package edu.gemini.spModel.gemini.ghost

import edu.gemini.spModel.core.AlmostEqual._
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GhostStandardResTargets._
import edu.gemini.spModel.gemini.ghost.GhostAsterism._
import edu.gemini.spModel.target.env.{Almosts, Arbitraries, TargetEnvironment}

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.scalacheck.Prop._

import scalaz._
import Scalaz._

class AsterismConvertersSpec extends Specification with ScalaCheck with Arbitraries with Almosts {
  "Asterism conversion" should {
    "Convert back and forth between SingleTarget" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[SingleTarget] ==> {
          val env2Opt = AsterismConvertersSpec.convertBackAndForth(env, AsterismConverters.GhostSingleTargetConverter, AsterismConverters.GhostSingleTargetConverter)
          env2Opt.exists(_ ~= env) should beTrue
        }
      }
    }

    "Convert back and forth between DualTarget" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[DualTarget] ==> {
          val env2Opt = AsterismConvertersSpec.convertBackAndForth(env, AsterismConverters.GhostDualTargetConverter, AsterismConverters.GhostDualTargetConverter)
          env2Opt.exists(_ ~= env) should beTrue
        }
      }
    }

    "Convert back and forth between TargetPlusSky" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[TargetPlusSky] ==> {
          val env2Opt = AsterismConvertersSpec.convertBackAndForth(env, AsterismConverters.GhostTargetPlusSkyConverter, AsterismConverters.GhostTargetPlusSkyConverter)
          env2Opt.exists(_ ~= env) should beTrue
        }
      }
    }

    "Convert back and forth between SkyPlusTarget" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[SkyPlusTarget] ==> {
          val env2Opt = AsterismConvertersSpec.convertBackAndForth(env, AsterismConverters.GhostSkyPlusTargetConverter, AsterismConverters.GhostSkyPlusTargetConverter)
          env2Opt.exists(_ ~= env) should beTrue
        }
      }
    }

    "Convert back and forth between HighResolution" in {
      forAll(genHighResAsterismTargetEnvironment) { env =>
        val env2Opt = AsterismConvertersSpec.convertBackAndForth(env, AsterismConverters.GhostHighResolutionConverter, AsterismConverters.GhostHighResolutionConverter)
        env2Opt.exists(_ ~= env) should beTrue
      }
    }
  }
}

object AsterismConvertersSpec {
  def convertBackAndForth(env: TargetEnvironment, c1: AsterismConverters.GhostAsterismConverter, c2: AsterismConverters.GhostAsterismConverter): Option[TargetEnvironment] = {
    c1.convert(env).flatMap(c2.convert) match {
      case \/-(env2) => env2.some
      case -\/(_)    => None
    }
  }
}