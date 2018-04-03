package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable.{ GnirsBlueprintSpectroscopy, AltairNGS, AltairNone, AltairLGS, Altair, GnirsBlueprintImaging}
import edu.gemini.model.p1.mutable.{GnirsCentralWavelength, GnirsFpu, GnirsCrossDisperser, GnirsDisperser, GnirsFilter, GnirsPixelScale}
import edu.gemini.spModel.core.MagnitudeBand

import org.scalacheck._
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.SpecificationLike

class GnirsBlueprintSpec extends TemplateSpec("GNIRS_BP.xml") with SpecificationLike with ScalaCheck {
  import GnirsBlueprintSpec._

  // This is just a sanity check to ensure that expansion works and that referenced library
  // observations exist. It doesn't test the specifics of the blueprint logic.

  "GNIRS Sanity Check" >> {

    "Imaging Blueprint Expansion" ! forAll { (bp: GnirsBlueprintImaging) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) => true }
    }

    "Imaging Spectroscopy Expansion" ! forAll { (bp: GnirsBlueprintSpectroscopy) =>
      expand(proposal(bp, List(6.5, 10, 21, 25), MagnitudeBand.H)) { (p, sp) => true }
    }

  }

}

object GnirsBlueprintSpec {

  implicit val ArbitraryAltair: Arbitrary[Altair] =
    Arbitrary(Gen.oneOf(List(
      AltairNone,
      AltairNGS(true),
      AltairNGS(false),
      AltairLGS(false, false, false),
      AltairLGS(true,  false, false),
      AltairLGS(false, true,  false),
      AltairLGS(false, false, true)
    )))

  implicit val ArbitraryPixelScale: Arbitrary[GnirsPixelScale] =
    Arbitrary(Gen.oneOf(GnirsPixelScale.values))

  implicit val ArbitraryFilter: Arbitrary[GnirsFilter] =
    Arbitrary(Gen.oneOf(GnirsFilter.values))

  implicit val ArbitraryDisperser: Arbitrary[GnirsDisperser] =
    Arbitrary(Gen.oneOf(GnirsDisperser.values))

  implicit val ArbitraryCrossDisperser: Arbitrary[GnirsCrossDisperser] =
    Arbitrary(Gen.oneOf(GnirsCrossDisperser.values))

  implicit val ArbitraryFpu: Arbitrary[GnirsFpu] =
    Arbitrary(Gen.oneOf(GnirsFpu.values))

  implicit val ArbitraryCentralWavelength: Arbitrary[GnirsCentralWavelength] =
    Arbitrary(Gen.oneOf(GnirsCentralWavelength.values))

  implicit val ArbitraryBlueprintImaging: Arbitrary[GnirsBlueprintImaging] =
    Arbitrary {
      for {
        a <- arbitrary[Altair]
        p <- arbitrary[GnirsPixelScale]
        f <- arbitrary[GnirsFilter]
      } yield GnirsBlueprintImaging(a, p, f)
    }

  implicit val ArbitraryBlueprintSpectroscopy: Arbitrary[GnirsBlueprintSpectroscopy] =
    Arbitrary {
      for {
        a <- arbitrary[Altair]
        p <- arbitrary[GnirsPixelScale]
        d <- arbitrary[GnirsDisperser]
        c <- arbitrary[GnirsCrossDisperser]
        f <- arbitrary[GnirsFpu]
        w <- arbitrary[GnirsCentralWavelength]
      } yield GnirsBlueprintSpectroscopy(a, p, d, c, f ,w)
    }

}