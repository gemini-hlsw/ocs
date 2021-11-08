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
import edu.gemini.spModel.obscomp.SPGroup.GroupType
import edu.gemini.phase2.template.factory.impl.ObservationEditor
import edu.gemini.spModel.gemini.gmos.InstGmosSouth
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.gemini.gmos.SeqConfigGmosSouth

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

  "GMOS South Check" >> {

    "Imaging Blueprint Expansion" ! forAll { (bp: GmosSBlueprintImaging) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) =>

        // REL-3987
        checkSingleTemplateGroupWithType(sp, GroupType.TYPE_SCHEDULING)
        groups(sp).flatMap(libs).toSet must_== Set(1)

      }
    }

    "IFU Expansion" ! forAll { (bp: GmosSBlueprintIfu) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) =>

        // REL-3987
        checkSingleTemplateGroupWithType(sp, GroupType.TYPE_FOLDER)
        groups(sp).flatMap(libs).toSet must_== Set(36,37,38)

      }
    }

    "IFU (N&S) Expansion" ! forAll { (bp: GmosSBlueprintIfuNs) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) =>

        // REL-3987
        checkSingleTemplateGroupWithType(sp, GroupType.TYPE_FOLDER)
        groups(sp).flatMap(libs).toSet must_== Set(43, 44, 45)

      }
    }

    "Longslit Expansion" ! forAll { (bp: GmosSBlueprintLongslit) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) =>

        // REL-3987
        checkSingleTemplateGroupWithType(sp, GroupType.TYPE_FOLDER)
        groups(sp).flatMap(libs).toSet must_== Set(2, 3, 4)

      }
    }

    "Longslit (N&S) Expansion" ! forAll { (bp: GmosSBlueprintLongslitNs) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) =>

        // REL-3987
        checkSingleTemplateGroupWithType(sp, GroupType.TYPE_FOLDER)
        groups(sp).flatMap(libs).toSet must_== Set(9, 10, 11)

      }
    }

    "MOS Expansion" ! forAll { (bp: GmosSBlueprintMos) =>
      expand(proposal(bp, List(1), MagnitudeBand.H)) { (p, sp) =>

        // REL-3987
        checkSingleTemplateGroupWithType(sp, GroupType.TYPE_FOLDER)
        groups(sp).flatMap(libs).toSet must_== {
          if (bp.nodAndShuffle) Set(29, 22, 31, 32) ++ (if (bp.preImaging) Set(17, 27) else Set(28))
          else                  Set(20, 21, 22, 23) ++ (if (bp.preImaging) Set(18, 17) else Set(19))
        }
        templateObservations(sp).filter { o =>
          val ed = ObservationEditor[InstGmosSouth](o, InstGmosSouth.SP_TYPE, SeqConfigGmosSouth.SP_TYPE)
          val om = ed.instrumentDataObject.right.toOption.map(_.getFPUnitCustomMask())
          val exp = o.getProgramID().toString().filterNot(_ == '-') + "-NN"
          om == Some(exp)
        } .map(_.getDataObject.asInstanceOf[SPObservation].getLibraryId.toInt).toSet must_== {
          if (bp.nodAndShuffle) Set(29, 22, 31, 32) ++ (if (bp.preImaging) Set(27) else Set(28))
          else                  Set(20, 21, 22, 23) ++ (if (bp.preImaging) Set(18) else Set(19))
        }

      }
    }

  }


}
