package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable.GracesBlueprint
import edu.gemini.model.p1.mutable.{GracesFiberMode, GracesReadMode}
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint._
import edu.gemini.spModel.pio.xml.PioXmlFactory

import org.scalacheck.Prop._
import org.scalacheck.{ Arbitrary, Gen }

import org.specs2.ScalaCheck
import org.specs2.mutable.SpecificationLike

class REL_1842_Test extends TemplateSpec("GRACES_BP.xml") with SpecificationLike with ScalaCheck {

  // Tests for P1 to P2 instantiation
  "P1 to P2 instantiation" should {

    implicit val arbp1:Arbitrary[GracesBlueprint] = Arbitrary {
      for {
        r <- Gen.oneOf(GracesReadMode.values)
        f <- Gen.oneOf(GracesFiberMode.values)
      } yield GracesBlueprint(f, r)
    }

    def checkPreservation[A](prop: String, f: GracesBlueprint => A, g: SpGracesBlueprint => A) =
      s"Preserve $prop" !
        forAll { (p1: GracesBlueprint) =>
          SpBlueprintFactory.create(p1) match {
            case Right(b: SpGracesBlueprint) => f(p1) must_== g(b)
            case x => sys.error(x.toString)
          }
        }

    checkPreservation("ReadMode",  _.readMode.name,  _.getReadMode.name)
    checkPreservation("FiberMode", _.fiberMode.name, _.getFiberMode.name)
  }

  // Tests for Pio
  "P2 PIO" should {

    implicit val arbp2 = Arbitrary {
      for {
        r <- Gen.oneOf(ReadMode.values)
        f <- Gen.oneOf(FiberMode.values)
      } yield new SpGracesBlueprint(r, f)
    }

    def checkPreservation[A](prop: String, f: SpGracesBlueprint => A) =
      s"Preserve $prop" !
        forAll { (bp1: SpGracesBlueprint) =>
          val bp2 = new SpGracesBlueprint(bp1.toParamSet(new PioXmlFactory))
          f(bp1) must_== f(bp2)
        }

    checkPreservation("ReadMode",  _.getReadMode)
    checkPreservation("FiberMode", _.getFiberMode)
    checkPreservation("Universal Equality", identity)

  }

  // See updated expansion tests in REL_2492_Test

}
