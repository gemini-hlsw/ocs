package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable.GracesBlueprint
import edu.gemini.model.p1.mutable.{GracesFiberMode, GracesReadMode}
import edu.gemini.pot.sp.{ISPProgram, ISPTemplateGroup}
import edu.gemini.shared.skyobject.Magnitude.Band
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint._
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.target.SPTarget

import org.scalacheck.Prop.forAll
import org.scalacheck.{ Arbitrary, Gen }

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class REL_1842_Test extends TemplateSpec("GRACES_BP.xml") with Specification with ScalaCheck {

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

  // Magnitude buckets
  sealed trait Bucket
  case object A extends Bucket
  case object B extends Bucket
  case object C extends Bucket
  case object D extends Bucket

  object Bucket {
    def all = List(A, B, C, D)
    def fromTarget(t: SPTarget): Option[Bucket] =
      t.getTarget.getMagnitude(Band.R).asScalaOpt.map(_.getBrightness).map { m =>
             if (m <= 6.5) A
        else if (m <= 10)  B
        else if (m <= 21)  C
        else               D
      }
  }

  // Return the Buckets for the targets in this template group. The returned list should always
  // have size = 1 but we will check that in a test below.
  def groupBuckets(g: ISPTemplateGroup): List[Option[Bucket]] =
    p2targets(g).map(Bucket.fromTarget)

  // Return the Bucket for the first target in the given group. We establish by testing groupBuckets
  // above that this will return the one and only bucket for this group.
  def groupBucket(g: ISPTemplateGroup): Option[Bucket] =
    groupBuckets(g).headOption.flatten

  // A map from Bucket to group. We establish that this is a 1:1 mapping in the tests below.
  def bucketMap(sp: ISPProgram): Map[Option[Bucket], ISPTemplateGroup] =
    groups(sp)
      .map { g => groupBucket(g) -> g }
      .toMap

  // Our common tests for GRACES blueprint expansion
  def gracesTest(rm: GracesReadMode, fm: GracesFiberMode, incl: Set[Int], excl: Set[Int]): Unit =
    expand(proposal(GracesBlueprint(fm, rm), (0.0 to 25.0 by 0.5).toList, MagnitudeBand.R)) { (p, sp) =>
      s"GRACES Blueprint Expansion $fm $rm " >> {

        "All targets in a given group should be in the same target brightness bucket." in {
          groups(sp).map(groupBuckets).forall(_.distinct.size must_== 1)
        }

        "There should be a template group for each target brightness bucket." in {
          val found = groups(sp).map(groupBucket).toSet
          (None :: Bucket.all.map(Some(_))).forall(found)
        }

        // Acquisitions determined by target brightness
        val map = bucketMap(sp)
        checkLibs("A group (acq)", map(Some(A)), Set(1),   Set(2,3,4))
        checkLibs("B group (acq)", map(Some(B)), Set(2),   Set(1,3,4))
        checkLibs("C group (acq)", map(Some(C)), Set(3),   Set(1,2,4))
        checkLibs("D group (acq)", map(Some(D)), Set(4),   Set(1,2,3))
        checkLibs("Missing R-band group (acq)", map(None), Set(1,2,3,4), Set())

        // Science determined by mode
        checkLibs("A group (sci)", map(Some(A)), incl, excl)
        checkLibs("B group (sci)", map(Some(B)), incl, excl)
        checkLibs("C group (sci)", map(Some(C)), incl, excl)
        checkLibs("D group (sci)", map(Some(D)), incl, excl)
        checkLibs("Missing R-band group (sci)", map(None), incl, excl)

      }
    }

  gracesTest(GracesReadMode.FAST,   GracesFiberMode.ONE_FIBER, Set(5),  Set(6, 7, 8, 9, 10))
  gracesTest(GracesReadMode.NORMAL, GracesFiberMode.ONE_FIBER, Set(6),  Set(5, 7, 8, 9, 10))
  gracesTest(GracesReadMode.SLOW,   GracesFiberMode.ONE_FIBER, Set(7),  Set(5, 6, 8, 9, 10))
  gracesTest(GracesReadMode.FAST,   GracesFiberMode.TWO_FIBER, Set(8),  Set(5, 6, 7, 9, 10))
  gracesTest(GracesReadMode.NORMAL, GracesFiberMode.TWO_FIBER, Set(9),  Set(5, 6, 7, 8, 10))
  gracesTest(GracesReadMode.SLOW,   GracesFiberMode.TWO_FIBER, Set(10), Set(5, 6, 7, 8, 9))

}
