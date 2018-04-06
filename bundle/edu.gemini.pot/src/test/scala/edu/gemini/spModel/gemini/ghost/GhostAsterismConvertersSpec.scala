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

class GhostAsterismConvertersSpec extends Specification with ScalaCheck with Arbitraries with Almosts {
  "Asterism conversion" should {
    "Convert back and forth between SingleTarget" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[SingleTarget] ==> {
          val env2Opt = GhostAsterismConvertersSpec.convertBackAndForth(env, GhostAsterismConverters.GhostSingleTargetConverter, GhostAsterismConverters.GhostSingleTargetConverter)
          env2Opt.exists(_ ~= env) should beTrue
        }
      }
    }

    "Convert back and forth between DualTarget" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[DualTarget] ==> {
          val env2Opt = GhostAsterismConvertersSpec.convertBackAndForth(env, GhostAsterismConverters.GhostDualTargetConverter, GhostAsterismConverters.GhostDualTargetConverter)
          env2Opt.exists(_ ~= env) should beTrue
        }
      }
    }

    "Convert back and forth between TargetPlusSky" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[TargetPlusSky] ==> {
          val env2Opt = GhostAsterismConvertersSpec.convertBackAndForth(env, GhostAsterismConverters.GhostTargetPlusSkyConverter, GhostAsterismConverters.GhostTargetPlusSkyConverter)
          env2Opt.exists(_ ~= env) should beTrue
        }
      }
    }

    "Convert back and forth between SkyPlusTarget" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[SkyPlusTarget] ==> {
          val env2Opt = GhostAsterismConvertersSpec.convertBackAndForth(env, GhostAsterismConverters.GhostSkyPlusTargetConverter, GhostAsterismConverters.GhostSkyPlusTargetConverter)
          env2Opt.exists(_ ~= env) should beTrue
        }
      }
    }

//    "Convert back and forth between HighResolution" in {
//      forAll(GhostGens.genHighResAsterismTargetEnvironment) { env =>
//        val env2Opt = AsterismConvertersSpec.convertBackAndForth(env, AsterismConverters.GhostHighResolutionConverter, AsterismConverters.GhostHighResolutionConverter)
//        env2Opt.exists(_ ~= env) should beTrue
//      }
//    }
  }
}

object GhostAsterismConvertersSpec {
  def convertBackAndForth(env: TargetEnvironment, c1: GhostAsterismConverters.GhostAsterismConverter, c2: GhostAsterismConverters.GhostAsterismConverter): Option[TargetEnvironment] = {
    c1.convert(env).flatMap(c2.convert) match {
      case \/-(env2) => env2.some
      case -\/(_)    => None
    }
  }
}