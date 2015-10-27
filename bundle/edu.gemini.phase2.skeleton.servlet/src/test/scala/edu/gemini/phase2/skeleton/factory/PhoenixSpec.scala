package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable.PhoenixBlueprint
import edu.gemini.model.p1.mutable.{PhoenixFilter, PhoenixFocalPlaneUnit}
import edu.gemini.spModel.gemini.phoenix.PhoenixParams.{Filter, Mask}
import edu.gemini.spModel.gemini.phoenix.blueprint.SpPhoenixBlueprint

import org.scalacheck.{ Properties, Gen, Arbitrary }
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz._, Scalaz._

object SpPhonixTemplateSpec extends Specification with ScalaCheck {

  implicit val ArbitraryP1PhoenixMask: Arbitrary[PhoenixFocalPlaneUnit] =
    Arbitrary(Gen.oneOf(PhoenixFocalPlaneUnit.values))

  implicit val ArbitraryP1PhoenixFilter: Arbitrary[PhoenixFilter] =
    Arbitrary(Gen.oneOf(PhoenixFilter.values))

  implicit val ArbitraryP1PhoenixBlueprint: Arbitrary[PhoenixBlueprint] =
    Arbitrary {
      for {
        fpu    <- arbitrary[PhoenixFocalPlaneUnit]
        filter <- arbitrary[PhoenixFilter]
      } yield PhoenixBlueprint(fpu, filter)
    }

  "Phase1 Conversion" should {
    "Always Succeed" ! forAll { (b: PhoenixBlueprint) =>
      SpBlueprintFactory.create(b) match {
        case Right(SpPhoenixBlueprint(m, f)) => (m.name must_== b.fpu.name) and (f.name must_== b.filter.name)
        case x => failure(x.toString)
      }
    }

  }

}
