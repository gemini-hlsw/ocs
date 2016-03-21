package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable.GracesBlueprint
import edu.gemini.model.p1.mutable.{GracesFiberMode, GracesReadMode}
import edu.gemini.pot.sp.{ISPProgram, ISPTemplateGroup}
import edu.gemini.shared.skyobject.Magnitude.Band
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.target.SPTarget

import org.specs2.ScalaCheck
import org.specs2.mutable.SpecificationLike

class REL_2429_Test extends TemplateSpec("GRACES_BP.xml") with SpecificationLike with ScalaCheck {

  // Magnitude buckets
  sealed trait Bucket
  case object A extends Bucket
  case object B extends Bucket

  object Bucket {
    def all = List(A, B)
    def fromTarget(t: SPTarget): Option[Bucket] =
      (t.getNewMagnitude(MagnitudeBand.R) orElse
       t.getNewMagnitude(MagnitudeBand.V)).map(_.value).map {
        m => if (m > 10) A else B
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
  def gracesTest(rm: GracesReadMode, fm: GracesFiberMode, inclA: Int, inclB: Int, m: MagnitudeBand): Unit =
    expand(proposal(GracesBlueprint(fm, rm), (0.0 to 25.0 by 0.5).toList, m)) { (p, sp) =>
      s"GRACES Blueprint Expansion $fm $rm mag in $m" >> {

        "All targets in a given group should be in the same target brightness bucket." in {
          groups(sp).map(groupBuckets).forall(_.distinct.size must_== 1)
        }

        "There should be a template group for each target brightness bucket." in {
          val found = groups(sp).map(groupBucket).toSet
          (None :: Bucket.all.map(Some(_))).forall(found)
        }

        List("How to prepare your program", "GRACES set-up") foreach { note =>
          s"Note '$note' should be included" in {
            groups(sp).forall(existsNote(_, note))
          }
        }

        // Exclude can be computed in this case
        val excl = (1 to 8).toSet - inclA - inclB

        // Science determined by mode and brightness
        val map = bucketMap(sp)
        checkLibs("A group (sci)", map(Some(A)), Set(inclA), excl)
        checkLibs("B group (sci)", map(Some(B)), Set(inclB), excl)
        checkLibs("Missing R-band group (sci)", map(None), Set(inclA, inclB), excl)

      }
    }

  List(MagnitudeBand.R, MagnitudeBand.V) foreach { m =>
    gracesTest(GracesReadMode.FAST,   GracesFiberMode.ONE_FIBER, 1, 2, m)
    gracesTest(GracesReadMode.NORMAL, GracesFiberMode.ONE_FIBER, 1, 2, m)
    gracesTest(GracesReadMode.SLOW,   GracesFiberMode.ONE_FIBER, 3, 4, m)
    gracesTest(GracesReadMode.FAST,   GracesFiberMode.TWO_FIBER, 5, 6, m)
    gracesTest(GracesReadMode.NORMAL, GracesFiberMode.TWO_FIBER, 5, 6, m)
    gracesTest(GracesReadMode.SLOW,   GracesFiberMode.TWO_FIBER, 7, 8, m)
  }

}
