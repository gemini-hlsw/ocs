package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable.{
  GmosSBlueprintImaging,
  GmosSBlueprintIfu,
  GmosSBlueprintIfuNs,
  GmosSBlueprintLongslit,
  GmosSBlueprintLongslitNs,
  GmosSBlueprintMos
}
import edu.gemini.model.p1.mutable.{GmosSMOSFpu, GmosSFpuNs, GmosSFpu, GmosSFpuIfuNs, GmosSDisperser, GmosSFilter, GmosSFpuIfu}

import edu.gemini.spModel.core.MagnitudeBand

import org.scalacheck._
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.SpecificationLike

class GmosSouthBlueprintSpec extends TemplateSpec("GMOS_S_BP.xml") with SpecificationLike with ScalaCheck {

  implicit val ArbitraryFilter: Arbitrary[GmosSFilter] =
    Arbitrary(Gen.oneOf(GmosSFilter.values))

  implicit val ArbitraryDisperser: Arbitrary[GmosSDisperser] =
    Arbitrary(Gen.oneOf(GmosSDisperser.values))

  implicit val ArbitraryIfu: Arbitrary[GmosSFpuIfu] =
    Arbitrary(Gen.oneOf(GmosSFpuIfu.values))

  implicit val ArbitraryIfuNs: Arbitrary[GmosSFpuIfuNs] =
    Arbitrary(Gen.oneOf(GmosSFpuIfuNs.values))

  implicit val ArbitraryFPu: Arbitrary[GmosSFpu] =
    Arbitrary(Gen.oneOf(GmosSFpu.values))

  implicit val ArbitraryFPuNs: Arbitrary[GmosSFpuNs] =
    Arbitrary(Gen.oneOf(GmosSFpuNs.values))

  implicit val ArbitraryMosFPu: Arbitrary[GmosSMOSFpu] =
    Arbitrary(Gen.oneOf(GmosSMOSFpu.values))

  implicit val ArbitraryBlueprintImaging: Arbitrary[GmosSBlueprintImaging] =
    Arbitrary {
      for {
        f  <- arbitrary[GmosSFilter]
        fs <- arbitrary[Set[GmosSFilter]]
      } yield GmosSBlueprintImaging((fs + f).toList)
    }

  implicit val ArbitraryBlueprintIfu: Arbitrary[GmosSBlueprintIfu] =
    Arbitrary {
      for {
        d <- arbitrary[GmosSDisperser]
        f <- arbitrary[GmosSFilter]
        i <- arbitrary[GmosSFpuIfu]
      } yield GmosSBlueprintIfu(d, f, i)
    }

  implicit val ArbitraryBlueprintIfuNs: Arbitrary[GmosSBlueprintIfuNs] =
    Arbitrary {
      for {
        d <- arbitrary[GmosSDisperser]
        f <- arbitrary[GmosSFilter]
        i <- arbitrary[GmosSFpuIfuNs]
      } yield GmosSBlueprintIfuNs(d, f, i)
    }

  implicit val ArbitraryBlueprintLongslit: Arbitrary[GmosSBlueprintLongslit] =
    Arbitrary {
      for {
        d <- arbitrary[GmosSDisperser]
        f <- arbitrary[GmosSFilter]
        i <- arbitrary[GmosSFpu]
      } yield GmosSBlueprintLongslit(d, f, i)
    }

  implicit val ArbitraryBlueprintLongslitNs: Arbitrary[GmosSBlueprintLongslitNs] =
    Arbitrary {
      for {
        d <- arbitrary[GmosSDisperser]
        f <- arbitrary[GmosSFilter]
        i <- arbitrary[GmosSFpuNs]
      } yield GmosSBlueprintLongslitNs(d, f, i)
    }

  implicit val ArbitraryBlueprintMos: Arbitrary[GmosSBlueprintMos] =
    Arbitrary {
      for {
        d <- arbitrary[GmosSDisperser]
        f <- arbitrary[GmosSFilter]
        i <- arbitrary[GmosSMOSFpu]
        n <- arbitrary[Boolean]
        p <- arbitrary[Boolean]
      } yield GmosSBlueprintMos(d, f, i, n, p)
    }

  // This is just a sanity check to ensure that expansion works and that referenced library
  // observations exist. It doesn't test the specifics of the blueprint logic.

  "GMOS South Sanity Check" >> {

    "Imaging Blueprint Expansion" ! forAll { (bp: GmosSBlueprintImaging) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) => true }
    }

    "IFU Expansion" ! forAll { (bp: GmosSBlueprintIfu) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) => true }
    }

    "IFU (N&S) Expansion" ! forAll { (bp: GmosSBlueprintIfuNs) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) => true }
    }

    "Longslit Expansion" ! forAll { (bp: GmosSBlueprintLongslit) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) => true }
    }

    "Longslit (N&S) Expansion" ! forAll { (bp: GmosSBlueprintLongslitNs) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) => true }
    }

    "MOS Expansion" ! forAll { (bp: GmosSBlueprintMos) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) => true }
    }

  }


}
