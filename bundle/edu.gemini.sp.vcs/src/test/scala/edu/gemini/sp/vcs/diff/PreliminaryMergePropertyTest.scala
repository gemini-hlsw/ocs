package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{DataObjectBlob => DOB, ISPNode, ISPProgram, SPNodeKey}
import edu.gemini.sp.vcs.diff.Diff.{Missing, Present}
import edu.gemini.pot.sp.version._
import edu.gemini.spModel.rich.pot.sp._

import org.junit.Test
import org.scalatest.junit.JUnitSuite

import scalaz._
import Scalaz._


class PreliminaryMergePropertyTest extends JUnitSuite {
  import edu.gemini.sp.vcs.diff.MergePropertyTest.NamedProperty

  private def drawMergeNode(t: Tree[MergeNode]): String = {
    implicit val ShowNode = Show.shows[MergeNode] {
      case Modified(k, _, dob, _) => s"m $k (${dob.getType})"
      case Unmodified(n)          => s"u ${n.key} (${n.getDataObject.getType})"
    }
    t.draw.zipWithIndex.collect { case (s0, n) if n % 2 == 0 => s0 }.mkString("\n")
  }

  case class PropContext(sp: ISPProgram, lp: ISPProgram, rp: ISPProgram, diffs: List[Diff], mergePlan: MergePlan) {
    val startMap = sp.nodeMap
    val startId  = sp.getLifespanId

    def keys(l: List[ISPNode]): Set[SPNodeKey] = l.map(_.key).toSet

    case class EditFacts(p: ISPProgram) {
      val nodeMap     = p.nodeMap
      val id          = p.getLifespanId
      val editedNodes = p.fold(List.empty[ISPNode]) { (lst, n) =>
        if (n.getVersion.clocks.contains(id)) n :: lst else lst
      }
      val editedKeys  = keys(editedNodes)

      val dataObjectEditedNodes = editedNodes.filter { n =>
        startMap.get(n.key).exists { s => !DOB.same(s.getDataObject, n.getDataObject) }
      }

      val childrenEditedNodes = editedNodes.filter { n =>
        startMap.get(n.key).exists { s =>
          s.children.map(_.key).equals(n.children.map(_.key))
        }
      }

      val newNodes    = editedNodes.filterNot(n => startMap.contains(n.key))
      val newKeys     = keys(newNodes)
      val deletedKeys = p.getVersions.keySet &~ nodeMap.keySet
    }

    val local  = new EditFacts(lp)
    val remote = new EditFacts(rp)

    val mergeMap    = mergePlan.update.sFoldRight(Map.empty[SPNodeKey, MergeNode]) { (mn, m) =>
      m + (mn.key -> mn)
    }

    val deletedKeys = mergePlan.delete.map(_.key).toSet

    def modifiedNode(k: SPNodeKey): Modified =
      mergeMap(k) match {
        case m: Modified => m
        case _ => error("expecting modified node but was unmodified: " + k)
      }
  }

  val props = List[NamedProperty[PropContext]] (
    ("all diffs are accounted for in the MergePlan",
      (start, local, remote, pc) => {
        val modCount = pc.mergePlan.update.foldMap {
          case _: Modified   => 1
          case _: Unmodified => 0
        }

        val deleteCount = pc.mergePlan.delete.size
        pc.diffs.size == modCount + deleteCount
      }
    ),

    ("no nodes are duplicated in the update tree",
      (start, local, remote, pc) => {
        val emptyCounts = Map.empty[SPNodeKey, Int].withDefaultValue(0)

        val keyCounts = pc.mergePlan.update.sFoldRight(emptyCounts) { (mn,m) =>
          val count = m(mn.key) + 1
          m.updated(mn.key, count)
        }

        keyCounts.values.forall(_ == 1)
      }
    ),

    ("any node present in both edited versions must be present in the merged result",
      (start, local, remote, pc) => {
        val commonKeys = pc.local.nodeMap.keySet & pc.remote.nodeMap.keySet & pc.diffs.collect {
          case Present(k, _, _, _, _) => k
        }.toSet

        val mergeKeys = pc.mergePlan.update.flatten.collect { case Modified(k,_,_,_) => k }.toSet
        (commonKeys &~ mergeKeys).isEmpty
      }
    ),

    ("any node missing in both edited versions must be missing in the merged result",
      (start, local, remote, pc) => {

        val commonKeys = pc.local.deletedKeys & pc.remote.deletedKeys & pc.diffs.collect {
          case Missing(k, _) => k
        }.toSet

        (commonKeys &~ pc.deletedKeys).isEmpty
      }
    ),

    ("a node that is newer and still present in the local program must appear in merge tree",
      (start, local, remote, pc) => (pc.local.editedKeys &~ pc.mergeMap.keySet).isEmpty
    ),

    ("a node that is older and still present in the remote program must appear in the merge tree",
      (start, local, remote, pc) => (pc.remote.editedKeys &~ pc.mergeMap.keySet).isEmpty
    ),

    ("",
      (start, local, remote, pc) => {
        true
      }
    ),

    ("",
      (start, local, remote, pc) => {
        true
      }
    ),

    ("",
      (start, local, remote, pc) => {
        true
      }
    ),

    ("",
      (start, local, remote, pc) => {
        true
      }
    ),

    ("",
      (start, local, remote, pc) => {
        true
      }
    )
    // ....

  )

  @Test
  def testAllPreliminaryMergeProperties(): Unit = {
    def mkPropContext(start: ISPProgram, local: ISPProgram, remote: ISPProgram): PropContext = {
      val diffs = ProgramDiff.compare(remote, local.getVersions, removedKeys(local))
      val mc    = MergeContext(local, diffs)
      PropContext(start, local, remote, diffs, PreliminaryMerge.merge(mc))
    }

    new MergePropertyTest(mkPropContext).checkAllProperties(props)
  }
}
