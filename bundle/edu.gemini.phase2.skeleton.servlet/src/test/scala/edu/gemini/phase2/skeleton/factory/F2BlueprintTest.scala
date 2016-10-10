package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.core.MagnitudeBand
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary._
import org.specs2.ScalaCheck
import org.specs2.mutable.SpecificationLike

import scala.collection.JavaConverters._

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

  "F2 Long-Slit" should {
    "Include notes, REL-2628" in {
      forAll { (b: Flamingos2BlueprintLongslit) =>
        expand(proposal(b, Nil, MagnitudeBand.R)) { (_, sp) =>
          val notes = List("F2 Long-Slit Notes", "Repeats contain the ABBA offsets", "Use same PA for science and Telluric")
          groups(sp).forall(tg => notes.forall(existsNote(tg, _)))
        }
      }
    }
  }

  "F2 Imaging" should {
    "not include darks, REL-2906" in {
      forAll { (b: Flamingos2BlueprintImaging) =>
        expand(proposal(b, Nil, MagnitudeBand.R)) { (_, sp) =>
          groups(sp).forall( tg =>
            // None of the component is a dark
            !libsMap(tg).exists {
              case (_, obs) => obs.getSeqComponent.getSeqComponents.asScala.exists(_.getType == SPComponentType.OBSERVER_DARK)
            }
          )
        }
      }
    }
  }

}
