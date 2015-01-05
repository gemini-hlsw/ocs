package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{DataObjectBlob, ISPNode}
import edu.gemini.sp.vcs.TestingEnvironment
import edu.gemini.sp.vcs.TestingEnvironment._
import edu.gemini.spModel.obscomp.SPNote
import edu.gemini.spModel.rich.pot.sp._
import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConverters._


class ProgramDiffTest {

  import edu.gemini.sp.vcs.diff.Diff._

  private def equalDiff(diffTuple: (Diff, Diff)): Boolean =
    diffTuple match {
      case (Present(eKey, eNv, eDob, eChild, eDet), Present(aKey, aNv, aDob, aChild, aDet)) =>
        eKey   == aKey   &&
        eNv    == aNv    &&
        eChild == aChild &&
        eDet   == aDet   &&
        DataObjectBlob.same(eDob, aDob)
      case (eDiff, aDiff) => eDiff == aDiff
    }

  private def equalDiffs(e: Seq[Diff], a: Seq[Diff]): Boolean =
    (e.size == a.size) && e.sortBy(_.key).zip(a.sortBy(_.key)).forall(equalDiff)


  private def assertEqualDiffs(env: TestingEnvironment, expected: Diff*): Unit = {
    val actual = ProgramDiff.compare(env.central.sp, env.cloned.sp.getVersions)
    assertTrue(equalDiffs(expected, actual))
  }

  private def assertNoDiffs(env: TestingEnvironment): Unit = assertEqualDiffs(env)

  @Test def noChangeNoDiff(): Unit =
    withPiTestEnv {
      assertNoDiffs
    }

  private def present(n: ISPNode, d: NodeDetail): Present =
    Present(n.getNodeKey, n.getVersion, n.getDataObject, n.children.map(_.getNodeKey), d)

  private def presentEmpty(n: ISPNode): Present =
    present(n, NodeDetail.Empty)

  private def presentObs(n: ISPNode, num: Int): Present =
    present(n, NodeDetail.Obs(num))

  private def missing(n: ISPNode): Missing = Missing(n.getNodeKey, n.getVersion)

  @Test def localChangeProducesDiff(): Unit =
    withPiTestEnv { env =>
      import env._

      central.setTitle("My Program")
      assertEqualDiffs(env, presentEmpty(central.sp))
    }

  @Test def remoteChangeProducesDiff(): Unit =
    withPiTestEnv { env =>
      import env._

      cloned.setTitle("My Program")
      assertEqualDiffs(env, presentEmpty(central.sp))
    }

  @Test def updatedChildPullsInAncestors(): Unit =
    withPiTestEnv { env =>
      import env._

      // Prog -> Group -> Note (in both versions)
      val group = central.addGroup()
      val note = central.addNote("foo", group.getNodeKey)
      cloned.copyFrom(central)
      assertNoDiffs(env)

      // Edit Note.
      cloned.setNoteText("bar", note.getNodeKey)

      // all nodes present, diff contains "foo" version of note.
      val noteDiff = presentEmpty(note)
      noteDiff match {
        case Present(_, _, dob, Nil, NodeDetail.Empty) =>
          assertEquals("foo", dob.asInstanceOf[SPNote].getNote)
        case _ => fail()
      }
      assertEqualDiffs(env, presentEmpty(central.sp), presentEmpty(group), noteDiff)
    }

  @Test def updatedAncestorDoesNotPullInDescendant(): Unit =
    withPiTestEnv { env =>
      import env._

      // Prog -> Group -> Note (in both versions)
      val group = central.addGroup()
      val note = central.addNote("foo", group.getNodeKey)
      cloned.copyFrom(central)
      assertNoDiffs(env)

      // Edit Group
      central.setTitle("foo", group.getNodeKey)

      // all nodes present, diff contains "foo" version of group title
      val groupDiff = presentEmpty(central.find(group.getNodeKey))
      groupDiff match {
        case Present(_, _, dob, children, NodeDetail.Empty) if children == List(note.getNodeKey) =>
          assertEquals("foo", dob.getTitle)
        case _ => fail()
      }
      assertEqualDiffs(env, presentEmpty(central.sp), groupDiff)
    }

  @Test def updatedObservationBringsEntireObservation(): Unit =
    withPiTestEnv { env =>
      import env._

      // Prog -> Obs -> {Conditions, Target}
      val obs = central.addObservation()
      cloned.copyFrom(central)
      assertNoDiffs(env)

      val comps = obs.getChildren.asScala.toList
      central.setTitle("foo", comps.head.getNodeKey)

      val diffs = presentEmpty(central.sp) :: presentObs(obs, 1) :: comps.map { c =>
        presentEmpty(central.find(c.getNodeKey))
      }
      assertEqualDiffs(env, diffs: _*)
    }

  @Test def unavailableNodeIsIncluded(): Unit =
    withPiTestEnv { env =>
      import env._

      val note  = cloned.addNote("foo")
      assertEqualDiffs(env, presentEmpty(central.sp), missing(note))
    }

  @Test def removedNodeNeverPresentInOtherSideIsIncluded(): Unit =
    withPiTestEnv { env =>
      import env._

      // Prog -> Note
      val note = central.addNote("foo")

      // Delete Note
      central.sp.children = Nil

      assertEqualDiffs(env, presentEmpty(central.sp), missing(note))
    }

  @Test def removedNodeKnownToBothSidesIsNotIncluded(): Unit =
    withPiTestEnv { env =>
      import env._

      // Prog -> Note
      val note = central.addNote("foo")
      cloned.copyFrom(central)
      assertNoDiffs(env)

      // Delete Note
      central.sp.children = Nil

      // Note version never updated so it is the same in both sides.  Only the
      // parent node (with its updated child list) need be sent.
      assertEqualDiffs(env, presentEmpty(central.sp))
    }

  @Test def modifiedThenRemovedNodeKnownToBothSidesIsIncluded(): Unit =
    withPiTestEnv { env =>
      import env._

      // Prog -> Note
      val note = central.addNote("foo")
      cloned.copyFrom(central)
      assertNoDiffs(env)

      // Update, then delete note
      central.setNoteText("bar", note.getNodeKey)
      central.sp.children = Nil

      assertEqualDiffs(env, presentEmpty(central.sp), missing(note))
    }

}