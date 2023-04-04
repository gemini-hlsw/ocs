package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable._
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.target.env.AsterismType
import edu.gemini.spModel.template.TemplateGroup
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary._
import org.specs2.ScalaCheck
import org.specs2.mutable.SpecificationLike

class GhostBlueprintTest extends TemplateSpec("GHOST_BP.xml") with SpecificationLike with ScalaCheck {

  implicit val ArbitraryGhostResolutionMode: Arbitrary[GhostResolutionMode] =
    Arbitrary(Gen.oneOf(GhostResolutionMode.values))

  implicit val ArbitraryGhostTargetMode: Arbitrary[GhostTargetMode] =
    Arbitrary(Gen.oneOf(GhostTargetMode.values))

  implicit val ArbitraryGhostBlueprint: Arbitrary[GhostBlueprint] =
    Arbitrary {
      for {
        r <- arbitrary[GhostResolutionMode]
        t <- arbitrary[GhostTargetMode]
      } yield GhostBlueprint(r, t)
    }

  "Ghost" should {

    "include the science observation" in {
      forAll { (b: GhostBlueprint) =>
        expand(proposal(b, Nil, MagnitudeBand.R)) { (_, sp) =>
          groups(sp).forall { tg =>
            libs(tg) == Set(1)
          }
        }
      }
    }

    "put the asterism type in the GHOST component" in {
      forAll { (b: GhostBlueprint) =>
        expand(proposal(b, Nil, MagnitudeBand.R)) { (_, sp) =>
          templateObservations(sp).forall { o =>
            AsterismType.forObservation(o) match {
              case AsterismType.Single                              =>
                false
              case AsterismType.GhostSingleTarget                   =>
                b.resolutionMode == GhostResolutionMode.Standard && b.targetMode == GhostTargetMode.Single
              case AsterismType.GhostDualTarget                     =>
                b.resolutionMode == GhostResolutionMode.Standard && b.targetMode == GhostTargetMode.Dual
              case AsterismType.GhostTargetPlusSky                  =>
                b.resolutionMode == GhostResolutionMode.Standard && b.targetMode == GhostTargetMode.TargetAndSky
              case AsterismType.GhostSkyPlusTarget                  =>
                b.resolutionMode == GhostResolutionMode.Standard && b.targetMode == GhostTargetMode.SkyAndTarget
              case AsterismType.GhostHighResolutionTargetPlusSky    =>
                b.resolutionMode == GhostResolutionMode.High
              case AsterismType.GhostHighResolutionTargetPlusSkyPrv =>
                b.resolutionMode == GhostResolutionMode.PrecisionRadialVelocity
            }
          }
        }
      }
    }
  }

}
