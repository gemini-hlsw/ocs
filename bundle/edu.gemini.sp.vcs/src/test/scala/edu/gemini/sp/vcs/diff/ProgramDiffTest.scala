package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{SPNodeKey, DataObjectBlob, ISPNode}
import edu.gemini.pot.sp.version._
import edu.gemini.sp.vcs.TestingEnvironment
import edu.gemini.sp.vcs.TestingEnvironment._
import edu.gemini.spModel.obscomp.SPNote
import edu.gemini.spModel.rich.pot.sp._
import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

class ProgramDiffTest {

  /*
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
    val actual = ProgramDiff.compare(env.central.sp, env.cloned.sp.getVersions, removedKeys(env.cloned.sp))
    assertTrue(equalDiffs(expected, actual))
  }

  private def assertNoDiffs(env: TestingEnvironment): Unit = assertEqualDiffs(env)

  @Test def singleNodeMap(): Unit =
    withPiTestEnv { env =>
      val p        = env.central.sp
      val actual   = p.nodeMap
      val expected = Map(p.key -> p)
      assertEquals(expected, actual)
    }

  @Test def simpleNodeMap(): Unit =
    withPiTestEnv { env =>
      val n0 = env.central.addNote("prog note")
      val g  = env.central.addGroup()
      val n1 = env.central.addNote("group note", g.key)

      val p        = env.central.sp
      val actual   = p.nodeMap
      val expected = Map(
        p.key  -> p,
        n0.key -> n0,
        g.key  -> g,
        n1.key -> n1
      )
      assertEquals(expected, actual)
    }

  @Test def deletedDoesNotAppearInNodeMap(): Unit =
    withPiTestEnv { env =>
      val n0 = env.central.addNote("prog note")
      val g  = env.central.addGroup()
      val n1 = env.central.addNote("group note", g.key)

      // Remove the group and its note
      val p      = env.central.sp
      p.children = List(n0)

      val actual   = p.nodeMap
      val expected = Map(
        p.key  -> p,
        n0.key -> n0
      )
      assertEquals(expected, actual)
    }

  @Test def noRemovedKeys(): Unit =
    withPiTestEnv { env =>
      assertTrue(removedKeys(env.cloned.sp).isEmpty)
    }

  @Test def testRemovedKeys(): Unit =
    withPiTestEnv { env =>
      val n0 = env.central.addNote("prog note")
      val g  = env.central.addGroup()
      val n1 = env.central.addNote("group note", g.key)

      // Remove the group and its note
      val p      = env.central.sp
      p.children = List(n0)

      val actual   = removedKeys(p)
      val expected = Set(g.key, n1.key)
      assertEquals(expected, actual)
    }

  @Test def noChangeNoDiff(): Unit =
    withPiTestEnv {
      assertNoDiffs
    }

  private def present(n: ISPNode, d: NodeDetail): Present =
    Present(n.key, n.getVersion, n.getDataObject, n.children.map(_.key), d)

  private def presentEmpty(n: ISPNode): Diff =
    present(n, NodeDetail.Empty)

  private def presentObs(n: ISPNode, num: Int): Diff =
    present(n, NodeDetail.Obs(num))

  private def unknown(n: ISPNode): Diff = Missing(n.key, EmptyNodeVersions)
  private def deleted(n: ISPNode): Diff = Missing(n.key, n.getVersion)

  // In these tests we examine the diffs that would be returned from the local
  // database ("central") when presented with the version information from a
  // remote program ("cloned").

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
      val note = central.addNote("foo", group.key)
      cloned.copyFrom(central)
      assertNoDiffs(env)

      // Edit Note.
      cloned.setNoteText("bar", note.key)

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
      val note = central.addNote("foo", group.key)
      cloned.copyFrom(central)
      assertNoDiffs(env)

      // Edit Group
      central.setTitle("foo", group.key)

      // all nodes present, diff contains "foo" version of group title
      val groupDiff = presentEmpty(central.find(group.key))
      groupDiff match {
        case Present(_, _, dob, children, NodeDetail.Empty) if children == List(note.key) =>
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
      central.setTitle("foo", comps.head.key)

      val diffs = presentEmpty(central.sp) :: presentObs(obs, 1) :: comps.map { c =>
        presentEmpty(central.find(c.key))
      }
      assertEqualDiffs(env, diffs: _*)
    }

  @Test def unknownRemoteNodeIsMissing(): Unit =
    withPiTestEnv { env =>
      import env._

      val note  = cloned.addNote("foo")
      assertEqualDiffs(env, presentEmpty(central.sp), unknown(note))
    }

  @Test def locallyRemovedNodeNeverPresentRemotelyIsIncluded(): Unit =
    withPiTestEnv { env =>
      import env._

      // Prog -> Note
      val note = central.addNote("foo")

      // Delete Note
      central.sp.children = Nil

      assertEqualDiffs(env, presentEmpty(central.sp), deleted(note))
    }

  // Starting with a common program containing a single note, try all
  // combinations of locally and remotely editing (or not) and/or removing (or
  // not) the note.
  @Test def removalTests(): Unit = {
    case class RmTest(pc: TestingEnvironment => TestingEnvironment.ProgContext, edited: Boolean, removed: Boolean) {
      def setup(env: TestingEnvironment, key: SPNodeKey): Unit = {
        val ctx  = pc(env)
        val note = ctx.find(key)
        if (edited) {
          note.title = "edited title"
        }
        if (removed) {
          ctx.sp.children = Nil
        }
      }

      def modifies: Boolean = edited || removed

      override def toString: String = s"edited=$edited, removed=$removed"
    }

    def test(local: RmTest, remote: RmTest): Unit = {
      val name = s"removalTests: local=($local), remote=($remote)"

      withPiTestEnv { env =>
        import env._

        // Prog -> Note
        val note = central.addNote("foo")
        val key  = note.key
        cloned.copyFrom(central)
        assertNoDiffs(env)

        // Make the appropriate modifications, if any.
        local.setup(env,  key)
        remote.setup(env, key)

        val rootDiff = (local.modifies || remote.modifies) option presentEmpty(central.sp)

        val noteDiff =
          if (note.getVersion === env.cloned.sp.getVersions(key))
            (local.removed, remote.removed) match {
              case (false, true)  => some(presentEmpty(note))
              case (true, false)  => some(deleted(note))
              case _              => none
            }
          else
            if (local.removed) some(deleted(note)) else some(presentEmpty(note))

        val expected = rootDiff.toList ++ noteDiff.toList
        val actual   = ProgramDiff.compare(env.central.sp, env.cloned.sp.getVersions, removedKeys(env.cloned.sp))
        assertTrue(s"$name\nexptected=$expected\nactual=$actual", equalDiffs(expected, actual))
      }
    }

    val tf = List(true, false)
    for {
      led <- tf
      lrm <- tf
      red <- tf
      rrm <- tf
    } test(RmTest(_.central, edited = led, removed = lrm),
           RmTest(_.cloned, edited = red, removed = rrm))
  }

  */
}