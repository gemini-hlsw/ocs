package edu.gemini.spModel.gemini.phoenix.blueprint

import edu.gemini.spModel.gemini.phoenix.PhoenixParams.{Filter, Mask}
import edu.gemini.spModel.pio.codec._
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.template.{SpBlueprint, TemplateFolder, SpBlueprintFactory}

import org.scalacheck.{ Properties, Gen, Arbitrary }
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.collection.JavaConverters._
import scalaz._, Scalaz._

object SpPhoenixBlueprintSpec extends Specification with ScalaCheck {

  implicit val ArbitraryPhoenixMask: Arbitrary[Mask] =
    Arbitrary(Gen.oneOf(Mask.values))

  implicit val ArbitraryPhoenixFilter: Arbitrary[Filter] =
    Arbitrary(Gen.oneOf(Filter.values))

  implicit val ArbitraryPhoenixBlueprint: Arbitrary[SpPhoenixBlueprint] =
    Arbitrary {
      for {
        fpu    <- arbitrary[Mask]
        filter <- arbitrary[Filter]
      } yield SpPhoenixBlueprint(fpu, filter)
    }

  "SpPhoenixBlueprint PIO" >> {

    "via ParamSetCodec" ! forAll { (b: SpPhoenixBlueprint) =>
      b.encode("foo").decode[SpPhoenixBlueprint] must_== \/-(b)
    }

    "via SpBlueprint API" ! forAll { (b: SpPhoenixBlueprint) =>
      new SpPhoenixBlueprint(b.toParamSet(null)) must_== b
    }

    "via SpBlueprintFactory (1)" ! forAll { (b: SpPhoenixBlueprint) =>
      SpBlueprintFactory.isSpBlueprintParamSet(b.toParamSet(null))
    }

    "via SpBlueprintFactory (2)" ! forAll { (b: SpPhoenixBlueprint) =>
      SpBlueprintFactory.fromParamSet(b.toParamSet(null)) must_== b
    }

    "via TemplateFolder" ! forAll { (b: SpPhoenixBlueprint) =>
      val tf1 = new TemplateFolder(Map("woozle" -> (b : SpBlueprint)).asJava)
      val tf2 = new TemplateFolder <| (_.setParamSet(tf1.getParamSet(new PioXmlFactory)))
      tf1.getBlueprints must_== tf2.getBlueprints
    }

  }

}
