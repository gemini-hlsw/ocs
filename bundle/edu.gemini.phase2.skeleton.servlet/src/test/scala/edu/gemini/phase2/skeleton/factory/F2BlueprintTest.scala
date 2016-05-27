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

  "F2 Long-Slit" should {
    "Include notes, REL-2628" in {
      forAll { (b: Flamingos2BlueprintLongslit) =>
        expand(proposal(b, Nil, MagnitudeBand.R)) { (p, sp) =>
          val notes = List("F2 Long-Slit Notes", "Repeats contain the ABBA offsets", "Use same PA for science and Telluric")
          groups(sp).forall(tg => notes.forall(existsNote(tg, _)))
        }
      }
    }
  }

}
