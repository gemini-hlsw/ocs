package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable._
import edu.gemini.spModel.core.MagnitudeBand
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary._
import org.specs2.ScalaCheck
import org.specs2.mutable.SpecificationLike

class F2BlueprintTest extends TemplateSpec("F2_BP.xml") with SpecificationLike with ScalaCheck {

  implicit val ArbitraryDisperser: Arbitrary[Flamingos2Disperser] =
    Arbitrary(Gen.oneOf(Flamingos2Disperser.values))

  implicit val ArbitraryFpu: Arbitrary[Flamingos2Fpu] =
    Arbitrary(Gen.oneOf(Flamingos2Fpu.values))

  implicit val ArbitraryFilter: Arbitrary[Flamingos2Filter] =
    Arbitrary(Gen.oneOf(Flamingos2Filter.values))

  implicit val ArbitraryFlamingos2BlueprintLongslit: Arbitrary[Flamingos2BlueprintLongslit] = Arbitrary {
    for {
      d  <- arbitrary[Flamingos2Disperser]
      f  <- arbitrary[List[Flamingos2Filter]]
      if f.nonEmpty // There must be at least one filter
      fu <- arbitrary[Flamingos2Fpu]
    } yield Flamingos2BlueprintLongslit(d, f, fu)
  }

  implicit val ArbitraryFlamingos2BlueprintImaging: Arbitrary[Flamingos2BlueprintImaging] = Arbitrary {
    for {
      f  <- arbitrary[List[Flamingos2Filter]]
      if f.nonEmpty // There must be at least one filter
    } yield Flamingos2BlueprintImaging(f)
  }

  implicit val ArbitraryFlamingos2BlueprintMos: Arbitrary[Flamingos2BlueprintMos] =
    Arbitrary {
      for {
        d  <- arbitrary[Flamingos2Disperser]
        fs <- Gen.nonEmptyListOf(arbitrary[Flamingos2Filter])
        p  <- arbitrary[Boolean]
      } yield Flamingos2BlueprintMos(d, fs, p)
    }

  "F2 Imaging" should {
    "include all notes" in {
      forAll { (b: Flamingos2BlueprintImaging) =>
        expand(proposal(b, Nil, MagnitudeBand.R)) { (_, sp) =>
          val notes = List(
            "F2 Imaging Notes",
            "Imaging flats",
            "Detector readout modes",
            "Libraries")
          groups(sp).forall(tg => notes.forall(existsNote(tg, _)))
        }
      }
    }
  }

  "F2 Long-Slit" should {
    "include all notes" in {
      forAll { (b: Flamingos2BlueprintLongslit) =>
        expand(proposal(b, Nil, MagnitudeBand.R)) { (_, sp) =>
          val notes = List(
            "F2 Long-Slit Notes",
            "Use the same PA for science target and telluric",
            "Repeats contain the ABBA offsets",
            "Detector readout modes",
            "Libraries")
          groups(sp).forall(tg => notes.forall(existsNote(tg, _)))
        }
      }
    }
  }

  "F2 MOS" should {
    "include all notes" in {
      forAll { (b: Flamingos2BlueprintMos) =>
        expand(proposal(b, Nil, MagnitudeBand.R)) { (_, sp) =>
          val notes = List(
            "F2 MOS Notes",
            "Use the same PA for the MOS science target and telluric",
            "Detector readout modes",
            "Libraries",
            "MOS Arcs and flats",
            "MOS slits: only use slit widths of 4 pixels (0.72 arcsec) or larger. Slit length no less than 5 arcsec.")
          groups(sp).forall(tg => notes.forall(existsNote(tg, _)))
        }
      }
    }

    "include MOS observations" in {
      forAll { (b: Flamingos2BlueprintMos) =>
        val incl = Range.inclusive(if (b.preImaging) 31 else 32, 39).toSet
        val excl = if (b.preImaging) Set.empty[Int] else Set(31)
        expand(proposal(b, Nil, MagnitudeBand.R)) { (_, sp) =>
          groups(sp).forall { tg =>
            val ls = libs(tg)
            ls.filter(incl) == incl && !ls.exists(excl)
          }
        }
      }
    }
  }

  // REL-3661: As of 2019B, F2 imaging can include darks.
//  "F2 Imaging" should {
//    "not include darks, REL-2906" in {
//      forAll { (b: Flamingos2BlueprintImaging) =>
//        expand(proposal(b, Nil, MagnitudeBand.R)) { (_, sp) =>
//          groups(sp).forall( tg =>
//            // None of the component is a dark
//            !libsMap(tg).exists {
//              case (_, obs) => obs.getSeqComponent.getSeqComponents.asScala.exists(_.getType == SPComponentType.OBSERVER_DARK)
//            }
//          )
//        }
//      }
//    }
//  }
}
