package edu.gemini.sp.vcs

import edu.gemini.pot.sp.{DataObjectBlob, ISPNode}
import edu.gemini.sp.vcs.TestingEnvironment._
import edu.gemini.spModel.obscomp.SPNote
import edu.gemini.spModel.rich.pot.sp._
import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConverters._


class ProgramDiffTest {

  import edu.gemini.sp.vcs.Diff._

  private def equalNodes(expected: DiffNode, actual: DiffNode): Boolean = {
    def equalDiff(expected: Diff, actual: Diff): Boolean =
      (expected, actual) match {
        case (InUse(dob0, ch0, detail0), InUse(dob1, ch1, detail1)) =>
          ch0 == ch1 && detail0 == detail1 && DataObjectBlob.same(dob0, dob1)
        case (d0, d1) => d0 == d1
      }

    (  expected.key == actual.key
    && expected.nv == actual.nv
    && equalDiff(expected.diff, actual.diff)
    )
  }

  private def equalDiffs(expected: Seq[DiffNode], actual: Seq[DiffNode]): Boolean =
    (  (expected.size == actual.size)
    && expected.sortBy(_.key).zip(actual.sortBy(_.key)).forall(t => equalNodes(t._1, t._2))
    )

  private def assertEqualDiffs(env: TestingEnvironment, expected: DiffNode*): Unit = {
    val actual = ProgramDiff.compare(env.central.sp, env.cloned.sp.getVersions)
    assertTrue(equalDiffs(expected, actual))
  }

  private def assertNoDiffs(env: TestingEnvironment): Unit = assertEqualDiffs(env)

  @Test def noChangeNoDiff(): Unit =
    withPiTestEnv {
      assertNoDiffs
    }

  private def inUse(n: ISPNode, d: NodeDetail): DiffNode =
    DiffNode(n.getNodeKey, n.getVersion, InUse(n.getDataObject, n.children.map(_.getNodeKey), d))

  private def inUseEmpty(n: ISPNode): DiffNode =
    inUse(n, NodeDetail.Empty)

  private def inUseObs(n: ISPNode, num: Int): DiffNode =
    inUse(n, NodeDetail.Obs(num))

  private def missing(n: ISPNode): DiffNode = DiffNode(n.getNodeKey, n.getVersion, Missing)
  private def removed(n: ISPNode): DiffNode = DiffNode(n.getNodeKey, n.getVersion, Removed)

  @Test def localChangeProducesDiff(): Unit =
    withPiTestEnv { env =>
      import env._

      central.setTitle("My Program")
      assertEqualDiffs(env, inUseEmpty(central.sp))
    }

  @Test def remoteChangeProducesDiff(): Unit =
    withPiTestEnv { env =>
      import env._

      cloned.setTitle("My Program")
      assertEqualDiffs(env, inUseEmpty(central.sp))
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
      val noteDiff = inUseEmpty(note)
      noteDiff.diff match {
        case InUse(dob, Nil, NodeDetail.Empty) =>
          assertEquals("foo", dob.asInstanceOf[SPNote].getNote)
        case _ => fail()
      }
      assertEqualDiffs(env, inUseEmpty(central.sp), inUseEmpty(group), noteDiff)
    }

  @Test def updatedAncestorDoesntPullInDescendant(): Unit =
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
      val groupDiff = inUseEmpty(central.find(group.getNodeKey))
      groupDiff.diff match {
        case InUse(dob, children, NodeDetail.Empty) if children == List(note.getNodeKey) =>
          assertEquals("foo", dob.getTitle)
        case _ => fail()
      }
      assertEqualDiffs(env, inUseEmpty(central.sp), groupDiff)
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

      val diffs = inUseEmpty(central.sp) :: inUseObs(obs, 1) :: comps.map { c =>
        inUseEmpty(central.find(c.getNodeKey))
      }
      assertEqualDiffs(env, diffs: _*)
    }

  @Test def unavailableNodeIsIncluded(): Unit =
    withPiTestEnv { env =>
      import env._

      val note  = cloned.addNote("foo")
      assertEqualDiffs(env, inUseEmpty(central.sp), missing(note))
    }

  @Test def removedNodeNeverPresentInOtherSideIsIncluded(): Unit =
    withPiTestEnv { env =>
      import env._

      // Prog -> Note
      val note = central.addNote("foo")

      // Delete Note
      central.sp.children = Nil

      assertEqualDiffs(env, inUseEmpty(central.sp), removed(note))
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
      assertEqualDiffs(env, inUseEmpty(central.sp))
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

      assertEqualDiffs(env, inUseEmpty(central.sp), removed(note))
    }

}