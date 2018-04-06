package edu.gemini.spModel.gemini.ghost

import edu.gemini.spModel.core.AlmostEqual._
import edu.gemini.spModel.gemini.ghost.AsterismConverters._
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GhostStandardResTargets._
import edu.gemini.spModel.gemini.ghost.GhostAsterism._
import edu.gemini.spModel.target.env.{Almosts, Arbitraries, TargetEnvironment}

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.scalacheck.Prop._

import scalaz._
import Scalaz._

// TODO:GHOST Fill these in with more test cases? Right now, we only check the lossless conversions, complete fails,
// and expected successes without delving deeper into the correctness of the conversions in the latter case.
// I am currently unsure if there is a way to test conversions that move information into UserTargets without
// duplicating the actual conversion code, or testing with specific, concrete examples.
object AsterismConvertersSpec extends Specification with ScalaCheck with Arbitraries with Almosts {

  "Asterism conversion" should {
    "Convert SingleTarget to itself losslessly" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[SingleTarget] ==> {
          GhostSingleTargetConverter.convert(env).exists(_ ~= env) should beTrue
        }
      }
    }

    "Convert DualTarget to itself losslessly" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[DualTarget] ==> {
          GhostDualTargetConverter.convert(env).exists(_ ~= env) should beTrue
        }
      }
    }

    "Convert TargetPlusSky to itself losslessly" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[TargetPlusSky] ==> {
          GhostTargetPlusSkyConverter.convert(env).exists(_ ~= env) should beTrue
        }
      }
    }

    "Convert SkyPlusTarget to itself losslessly" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[SkyPlusTarget] ==> {
          GhostSkyPlusTargetConverter.convert(env).exists(_ ~= env) should beTrue
        }
      }
    }

    "Convert between TargetPlusSky and SkyPlusTarget losslessly" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[SkyPlusTarget] ==> {
          convertBackAndForth(env, GhostTargetPlusSkyConverter, GhostSkyPlusTargetConverter).exists(_ ~= env) should beTrue
        }
      }
    }

    "Convert between SkyPlusTarget and TargetPlusSky losslessly" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[TargetPlusSky] ==> {
          convertBackAndForth(env, GhostSkyPlusTargetConverter, GhostTargetPlusSkyConverter).exists(_ ~= env) should beTrue
        }
      }
    }

    "Convert between TargetPlusSky and HighResolution losslessly" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[TargetPlusSky] ==> {
          convertBackAndForth(env, GhostHighResolutionConverter, GhostTargetPlusSkyConverter).exists(_ ~= env) should beTrue
        }
      }
    }

    "Convert between SkyPlusTarget and HighResolution losslessly" in {
      forAll(genStandardResAsterismTargetEnvironment) { env =>
        env.getAsterism.asInstanceOf[StandardResolution].targets.isInstanceOf[SkyPlusTarget] ==> {
          convertBackAndForth(env, GhostHighResolutionConverter, GhostSkyPlusTargetConverter).exists(_ ~= env) should beTrue
        }
      }
    }

    "Convert HighResolution to itself losslessly" in {
      forAll(genHighResAsterismTargetEnvironment) { env =>
        GhostHighResolutionConverter.convert(env).exists(_ ~= env) should beTrue
      }
    }

    "Be able to convert any GHOST asterism to any other" in {
      forAll(genGhostAsterismTargetEnvironment) { env =>
        GhostSingleTargetConverter.convert(env)   should not(beEmpty)
        GhostDualTargetConverter.convert(env)     should not(beEmpty)
        GhostTargetPlusSkyConverter.convert(env)  should not(beEmpty)
        GhostSkyPlusTargetConverter.convert(env)  should not(beEmpty)
        GhostHighResolutionConverter.convert(env) should not(beEmpty)
      }
    }

    "Not convert any Single asterism to a GHOST asterism" in {
      forAll(genSingleAsterismTargetEnvironment) { env =>
        GhostSingleTargetConverter.convert(env)   should beEmpty
        GhostDualTargetConverter.convert(env)     should beEmpty
        GhostTargetPlusSkyConverter.convert(env)  should beEmpty
        GhostSkyPlusTargetConverter.convert(env)  should beEmpty
        GhostHighResolutionConverter.convert(env) should beEmpty
      }
    }
  }

  private def convertBackAndForth(env: TargetEnvironment, c1: GhostAsterismConverter, c2: GhostAsterismConverter): Option[TargetEnvironment] =
    c1.convert(env).flatMap(c2.convert)
}