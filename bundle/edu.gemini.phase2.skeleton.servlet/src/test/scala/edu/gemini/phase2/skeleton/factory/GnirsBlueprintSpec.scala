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

    "Spectroscopy Expansion" ! forAll { (bp: GnirsBlueprintSpectroscopy) =>
      expand(proposal(bp, List(6.5, 10, 21, 25), MagnitudeBand.H)) { (p, sp) => true }
    }

  }

  "GNIRS IFU Spectroscopy" >> {

    "Non-Mag-Specific" ! forAll { (bp: GnirsBlueprintSpectroscopy) =>
      Set(GnirsFpu.LR_IFU, GnirsFpu.HR_IFU)(bp.fpu) ==>
        expand(proposal(bp, List(6.5, 10, 15, 21, 25), MagnitudeBand.H)) { (p, sp) =>
          val isIncluded = groups(sp).flatMap(libs).toSet
          val shouldBeIncluded =
            bp.fpu match {
              case GnirsFpu.LR_IFU => Set(24, 25, 32, 33, 35, 36)
              case GnirsFpu.HR_IFU => Set(24, 26, 34, 35, 37, 38)
              case _               => sys.error("unpossible")
            }
          shouldBeIncluded.forall(isIncluded) must beTrue
        }
    }

    "Mag-Specific" ! forAll { (bp: GnirsBlueprintSpectroscopy) =>
      Set(GnirsFpu.LR_IFU, GnirsFpu.HR_IFU)(bp.fpu) ==>
        expand(proposal(bp, List(6.5, 10, 15, 19, 21), MagnitudeBand.H)) { (p, sp) =>
          val gs = groups(sp)
          gs.length should_== 6 // 1 per mag bucket + 1
          gs forall { g =>
            val ts = p2targets(g)
            ts.length should_== 1 // one target per group
            val acqs = p2mag(ts.head, MagnitudeBand.H) match {
              case Some(h) =>
                if (h < 7) Set(27)
                else if (h < 11.5) Set(28)
                else if (h < 16.0) Set(29)
                else if (h < 20.0) Set(30)
                else Set(31)
              case None => (27 to 31).toSet
            }
            acqs.forall(libs(g).toSet) // all acqs should be there
          }
        }
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