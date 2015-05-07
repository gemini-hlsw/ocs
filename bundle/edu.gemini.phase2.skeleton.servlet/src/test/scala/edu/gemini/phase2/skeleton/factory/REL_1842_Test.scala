package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable.GracesBlueprint
import edu.gemini.model.p1.immutable.{GracesFiberMode, GracesReadMode}
import edu.gemini.phase2.template.factory.impl.graces.Graces
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.shared.skyobject.Magnitude.Band
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint._
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint.ReadMode._
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint.FiberMode._
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.target.SPTarget

import org.scalacheck.Prop.forAll
import org.scalacheck.{ Arbitrary, Gen }

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz.syntax.id._

class REL_1842_Test extends Specification with ScalaCheck {

  // Tests for P1 to P2 instantiation
  "P1 to P2 instantiation" should {

    implicit val arbp1 = Arbitrary {
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
            case x => failure(x.toString)
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

  // Tests for GroupInitializer
  def incSpec(r: ReadMode, f: FiberMode, rMag: Option[Double], inc: Set[Int], excl: Set[Int]) = {
    val t = rMag.map(m => new SPTarget <| (_.getTarget.putMagnitude(new Magnitude(Band.R, m))))
    val g = Graces(new SpGracesBlueprint(r, f), t)
    val n = s"Graces($r, $f, R-MAG ${rMag.fold("--")(_.toString)})"
    s"Initialized $n" should {
      if (excl.nonEmpty) {
        s"exclude obs ${excl.mkString("{", ",", "}")} from target group" in {
          g.targetGroup.filter(excl) must beEmpty
        }
      }
      s"include obs ${inc.mkString("{",",","}")} in target group" in {
        g.targetGroup.filter(inc).toSet must_== inc
      }
    }
  }

  // R-band selection of acquisitions
  incSpec(FAST, ONE_FIBER, None,     Set(1,2,3,4), Set())
  incSpec(FAST, ONE_FIBER, Some(3),  Set(1),       Set(2,3,4))
  incSpec(FAST, ONE_FIBER, Some(7),  Set(2),       Set(1,3,4))
  incSpec(FAST, ONE_FIBER, Some(15), Set(3),       Set(1,2,4))
  incSpec(FAST, ONE_FIBER, Some(22), Set(4),       Set(1,2,3))

  // Config selection of science obs
  incSpec(FAST,   ONE_FIBER, None, Set(5),  Set(6, 7, 8, 9, 10))
  incSpec(NORMAL, ONE_FIBER, None, Set(6),  Set(5, 7, 8, 9, 10))
  incSpec(SLOW,   ONE_FIBER, None, Set(7),  Set(5, 6, 8, 9, 10))
  incSpec(FAST,   TWO_FIBER, None, Set(8),  Set(5, 6, 7, 9, 10))
  incSpec(NORMAL, TWO_FIBER, None, Set(9),  Set(5, 6, 7, 8, 10))
  incSpec(SLOW,   TWO_FIBER, None, Set(10), Set(5, 6, 7, 8, 9))

}
