package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable.{
  GmosNBlueprintImaging,
  GmosNBlueprintIfu,
  GmosNBlueprintLongslit,
  GmosNBlueprintLongslitNs,
  GmosNBlueprintMos
}
import edu.gemini.model.p1.mutable.{GmosNMOSFpu, GmosNFpuNs, GmosNFpu, GmosNDisperser, GmosNFilter, GmosNFpuIfu}

import edu.gemini.spModel.core.MagnitudeBand

import org.scalacheck._
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.SpecificationLike
import edu.gemini.model.p1.immutable.Altair
import edu.gemini.model.p1.immutable.AltairNone
import edu.gemini.model.p1.immutable.AltairNGS
import edu.gemini.model.p1.immutable.AltairLGS

class GmosNouthBlueprintSpec extends TemplateSpec("GMOS_N_BP.xml") with SpecificationLike with ScalaCheck {

  implicit val ArbitraryFilter: Arbitrary[GmosNFilter] =
    Arbitrary(Gen.oneOf(GmosNFilter.values))

  implicit val ArbitraryDisperser: Arbitrary[GmosNDisperser] =
    Arbitrary(Gen.oneOf(GmosNDisperser.values))

  implicit val ArbitraryIfu: Arbitrary[GmosNFpuIfu] =
    Arbitrary(Gen.oneOf(GmosNFpuIfu.values))

  implicit val ArbitraryFPu: Arbitrary[GmosNFpu] =
    Arbitrary(Gen.oneOf(GmosNFpu.values))

  implicit val ArbitraryFPuNs: Arbitrary[GmosNFpuNs] =
    Arbitrary(Gen.oneOf(GmosNFpuNs.values))

  implicit val ArbitraryMosFPu: Arbitrary[GmosNMOSFpu] =
    Arbitrary(Gen.oneOf(GmosNMOSFpu.values))

  implicit val ArbitraryAltair: Arbitrary[Altair] =
    Arbitrary(Gen.oneOf(
      Gen.const(AltairNone),
      arbitrary[Boolean].map(fieldLens => AltairNGS(fieldLens)),
      Gen.choose(0,3).map(c => AltairLGS(c == 1, c == 2, c == 3))
    ))

  implicit val ArbitraryBlueprintImaging: Arbitrary[GmosNBlueprintImaging] =
    Arbitrary {
      for {
        a  <- arbitrary[Altair]
        f  <- arbitrary[GmosNFilter]
        fs <- arbitrary[Set[GmosNFilter]]
      } yield GmosNBlueprintImaging(a, (fs + f).toList)
    }

  implicit val ArbitraryBlueprintIfu: Arbitrary[GmosNBlueprintIfu] =
    Arbitrary {
      for {
        a <- arbitrary[Altair]
        d <- arbitrary[GmosNDisperser]
        f <- arbitrary[GmosNFilter]
        i <- arbitrary[GmosNFpuIfu]
      } yield GmosNBlueprintIfu(a, d, f, i)
    }

  implicit val ArbitraryBlueprintLongslit: Arbitrary[GmosNBlueprintLongslit] =
    Arbitrary {
      for {
        a <- arbitrary[Altair]
        d <- arbitrary[GmosNDisperser]
        f <- arbitrary[GmosNFilter]
        i <- arbitrary[GmosNFpu]
      } yield GmosNBlueprintLongslit(a, d, f, i)
    }

  implicit val ArbitraryBlueprintLongslitNs: Arbitrary[GmosNBlueprintLongslitNs] =
    Arbitrary {
      for {
        a <- arbitrary[Altair]
        d <- arbitrary[GmosNDisperser]
        f <- arbitrary[GmosNFilter]
        i <- arbitrary[GmosNFpuNs]
      } yield GmosNBlueprintLongslitNs(a, d, f, i)
    }

  implicit val ArbitraryBlueprintMos: Arbitrary[GmosNBlueprintMos] =
    Arbitrary {
      for {
        a <- arbitrary[Altair]
        d <- arbitrary[GmosNDisperser]
        f <- arbitrary[GmosNFilter]
        i <- arbitrary[GmosNMOSFpu]
        n <- arbitrary[Boolean]
        p <- arbitrary[Boolean]
      } yield GmosNBlueprintMos(a, d, f, i, n, p)
    }

  // This is just a sanity check to ensure that expansion works and that referenced library
  // observations exist. It doesn't test the specifics of the blueprint logic.

  "GMOS North Sanity Check" >> {

    "Imaging Blueprint Expansion" ! forAll { (bp: GmosNBlueprintImaging) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) => true }
    }

    "IFU Expansion" ! forAll { (bp: GmosNBlueprintIfu) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) => true }
    }

    "Longslit Expansion" ! forAll { (bp: GmosNBlueprintLongslit) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) => true }
    }

    "Longslit (N&S) Expansion" ! forAll { (bp: GmosNBlueprintLongslitNs) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) => true }
    }

    "MOS Expansion" ! forAll { (bp: GmosNBlueprintMos) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) => true }
    }

  }


}
