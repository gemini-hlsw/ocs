package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{DataObjectBlob => DOB, ISPNode, ISPProgram, SPNodeKey}
import edu.gemini.shared.util.VersionComparison
import VersionComparison.Newer
import edu.gemini.sp.vcs.diff.NodeDetail.Obs
import edu.gemini.spModel.rich.pot.sp._

import org.junit.Test
import org.scalatest.junit.JUnitSuite

import scalaz._


class PreliminaryMergePropertyTest extends JUnitSuite {
  import edu.gemini.sp.vcs.diff.MergePropertyTest.NamedProperty

  private def keys(l: List[ISPNode]): Set[SPNodeKey] = l.map(_.key).toSet

  case class EditFacts(p: ISPProgram, startMap: Map[SPNodeKey, ISPNode]) {
    val nodeMap     = p.nodeMap
    val id          = p.getLifespanId
    val editedNodes = p.fold(List.empty[ISPNode]) { (lst, n) =>
      if (n.getVersion.clocks.contains(id)) n :: lst else lst
    }
    val editedKeys  = keys(editedNodes)

    def isEdited(n: ISPNode): Boolean = editedKeys.contains(n.key)

    def isParentEdited(n: ISPNode): Boolean = Option(n.getParent).exists(isEdited)

    def containsRemotelyEditedNode(n: ISPNode): Boolean =
      isEdited(n) || n.children.exists(containsRemotelyEditedNode)


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

  case class PropContext(sp: ISPProgram, lp: ISPProgram, rp: ISPProgram, diffs: MergePlan, mergePlan: MergePlan) {
    val startMap = sp.nodeMap
    val startId  = sp.getLifespanId

    val local  = new EditFacts(lp, startMap)
    val remote = new EditFacts(rp, startMap)

    val mergeContext = MergeContext(lp, diffs)

    def compare(k: SPNodeKey): VersionComparison =
      mergeContext.local.version(k).compare(mergeContext.remote.version(k))

    val mergeMap     = mergePlan.update.sFoldRight(Map.empty[SPNodeKey, MergeNode]) { (mn, m) =>
      m + (mn.key -> mn)
    }

    val modifiedKeys = mergePlan.update.flatten.collect {
      case Modified(k, _, _, _) => k
    }.toSet

    val unmodifiedKeys = mergePlan.update.flatten.collect {
      case Unmodified(k) => k
    }.toSet

    val deletedKeys = mergePlan.delete.map(_.key).toSet
  }

  def mpKeys(mp: MergePlan): Set[SPNodeKey] = {
    val modKeys = mp.update.foldRight(Set.empty[SPNodeKey]) { (mn, s) => s + mn.key }
    val delKeys = mp.delete.map(_.key)
    modKeys ++ delKeys
  }

  def emptySet(pc: PropContext, s: Set[_]): Boolean = {
    if (s.nonEmpty) {
      Console.err.println(MergeNode.draw(pc.mergePlan.update))
      Console.err.println("# " + s.mkString("{", ", ", "}"))
    }
    s.isEmpty
  }

  val props = List[NamedProperty[PropContext]] (
    ("all diffs are accounted for in the MergePlan",
      (start, local, remote, pc) => {
        val diffKeys  = mpKeys(pc.diffs)
        val mergeKeys = mpKeys(pc.mergePlan)

        val result = (diffKeys & mergeKeys) == diffKeys

        if (!result) {
          Console.err.println(MergeNode.draw(pc.mergePlan.update))
          Console.err.println("# diff - merge = " + (diffKeys &~ mergeKeys).mkString("{", ", ", "}"))
          Console.err.println("# merge - diff = " + (mergeKeys &~ diffKeys).mkString("{", ", ", "}"))
          val badKeys = (diffKeys &~ mergeKeys) ++ (mergeKeys &~ diffKeys)
          badKeys.foreach { k =>
            Console.err.println("# " + k)
            Console.err.println("\tin diff deleted?  " + pc.diffs.delete.exists(m => m.key == k))
            Console.err.println("\tin diff update?   " + pc.diffs.update.flatten.exists(mn => mn.key == k))
            Console.err.println("\tin merge deleted? " + pc.mergePlan.delete.exists(m => m.key == k))
            Console.err.println("\tin merge update?  " + pc.mergePlan.update.flatten.exists(mn => mn.key == k))
          }
        }

        result
      }
    ),

    ("no nodes are duplicated in the update tree",
      (start, local, remote, pc) => {
        val emptyCounts = Map.empty[SPNodeKey, Int].withDefaultValue(0)

        val keyCounts = pc.mergePlan.update.sFoldRight(emptyCounts) { (mn,m) =>
          val count = m(mn.key) + 1
          m.updated(mn.key, count)
        }

        val dups = keyCounts.filter { case (_,v) => v != 1 }
        if (dups.nonEmpty) {
          Console.err.println(MergeNode.draw(pc.mergePlan.update))
          Console.err.println("# Duplicates")
          dups.foreach(k => Console.err.println("\t" + k))
        }

        dups.isEmpty
      }
    ),

    ("any node present in both edited versions must be present in the merged result",
      (start, local, remote, pc) => {
        val commonKeys = pc.local.nodeMap.keySet & pc.diffs.update.sFoldRight(Set.empty[SPNodeKey]) { (mn, s) => s + mn.key }
        val mergeKeys  = pc.mergePlan.update.flatten.map(_.key).toSet
        val result = (commonKeys & mergeKeys) == commonKeys

        if (!result) {
          Console.err.println(MergeNode.draw(pc.mergePlan.update))
          Console.err.println("# common - merge = " + (commonKeys &~ mergeKeys).mkString("{", ", ", "}"))
          val badKeys = (commonKeys &~ mergeKeys) ++ (mergeKeys &~ commonKeys)
          badKeys.foreach { k =>
            Console.err.println("# " + k)
            Console.err.println("\tin diff deleted?  " + pc.diffs.delete.exists(m => m.key == k))
            Console.err.println("\tin diff update?   " + pc.diffs.update.flatten.exists(mn => mn.key == k))
            Console.err.println("\tin local nodeMap? " + pc.local.nodeMap.contains(k))
            Console.err.println("\tin merge deleted? " + pc.mergePlan.delete.exists(m => m.key == k))
            Console.err.println("\tin merge update?  " + pc.mergePlan.update.flatten.exists(mn => mn.key == k))
          }
        }

        result
      }
    ),

    ("any node missing in both edited versions must be missing in the merged result",
      (start, local, remote, pc) => {
        val commonKeys = pc.local.deletedKeys & pc.diffs.delete.map(_.key)
        emptySet(pc, commonKeys &~ pc.deletedKeys)
      }
    ),

    ("a node that is newer and still present in the local program must appear in merge tree",
      (start, local, remote, pc) => emptySet(pc, pc.local.editedKeys &~ pc.mergeMap.keySet)
    ),

    ("a node that is older and still present in the remote program must appear in the merge tree",
      (start, local, remote, pc) => emptySet(pc, pc.remote.editedKeys &~ pc.mergeMap.keySet)
    ),

    ("a locally deleted but remotely edited node must be present in the merged program",
      (start, local, remote, pc) => {
        val delLocalModRemote = pc.local.deletedKeys & pc.remote.editedKeys
        emptySet(pc, delLocalModRemote &~ pc.mergeMap.keySet)
      }
    ),

    ("a remotely deleted but locally edited node must be present in the merged program",
      (start, local, remote, pc) => {
        val delRemoteModLocal = pc.remote.deletedKeys & pc.local.editedKeys
        emptySet(pc, delRemoteModLocal &~ pc.mergeMap.keySet)
      }
    ),

    ("a locally deleted node inside a remotely edited parent must be present in the merged program",
      (start, local, remote, pc) => {
        val localDeletedRemotePresent = pc.local.deletedKeys & pc.remote.nodeMap.keySet
        val inEditedRemoteParent = localDeletedRemotePresent.filter { k =>
          pc.remote.isParentEdited(pc.remote.nodeMap(k))
        }

        emptySet(pc, inEditedRemoteParent &~ pc.mergeMap.keySet)
      }
    ),

    ("a locally resurrected node not inside a remotely edited parent must contain a node that is remotely edited",
      (start, local, remote, pc) => {
        val localDeletedRemotePresent = pc.local.deletedKeys & pc.remote.nodeMap.keySet
        val inEditedRemoteParent = localDeletedRemotePresent.filter { k =>
          pc.remote.isParentEdited(pc.remote.nodeMap(k))
        }

        val locallyResurrected = (pc.local.deletedKeys & pc.mergeMap.keySet) &~ inEditedRemoteParent
        emptySet(pc, locallyResurrected.map(pc.remote.nodeMap.apply).filterNot(pc.remote.containsRemotelyEditedNode))
      }
    ),

    ("a locally deleted node whose remote version contains no updates must stay deleted",
      (start, local, remote, pc) => {
        val localDeletions = pc.local.deletedKeys & pc.remote.nodeMap.keySet

        val mergeDeletions = localDeletions.filterNot { k =>
          val r = pc.remote.nodeMap(k)
          pc.remote.isParentEdited(r) || pc.remote.containsRemotelyEditedNode(r)
        }

        emptySet(pc, mergeDeletions &~ pc.mergePlan.delete.map(_.key))
      }
    ),

    ("a remotely deleted node whose local version contains no updates must stay deleted",
      (start, local, remote, pc) => {
        val remoteDeletions = pc.remote.deletedKeys & pc.local.nodeMap.keySet

        val mergeDeletions = remoteDeletions.filterNot { k =>
          val r = pc.local.nodeMap(k)
          pc.local.isParentEdited(r) || pc.local.containsRemotelyEditedNode(r)
        }

        emptySet(pc, mergeDeletions &~ pc.mergePlan.delete.map(_.key))
      }
    ),

    ("for any strictly newer local node, the matching Modified merge node data object must be the same",
      (start, local, remote, pc) =>
        pc.local.dataObjectEditedNodes.filter(n => pc.compare(n.key) == Newer).forall { n =>
          pc.mergeMap.get(n.key).exists {
            case Modified(_, _, dob, _) => DOB.same(n.getDataObject, dob)
            case Unmodified(_)          => false
          }
        }
    ),

    ("for any edited remote node, the matching Modified merge node data object must be the same",
      (start, local, remote, pc) =>
        pc.remote.dataObjectEditedNodes.filterNot(n => pc.compare(n.key) == Newer).forall { n =>
          pc.mergeMap.get(n.key).exists {
            case Modified(_, _, dob, _) => DOB.same(n.getDataObject, dob)
            case Unmodified(_)          => false
          }
        }
    ),

    ("all unmodified nodes in the merge tree correspond to existing local program nodes",
      (start, local, remote, pc) => emptySet(pc, pc.unmodifiedKeys &~ pc.local.nodeMap.keySet)
    ),

    ("no unmodified node contains a modified node",
      (start, local, remote, pc) => {
        val unmodifiedNodes = pc.unmodifiedKeys.toList.map(pc.local.nodeMap.apply)

        // all keys corresponding to Unmodified merge nodes or their descendants
        // in the local program
        val allUnmodified   = (Set.empty[SPNodeKey]/:unmodifiedNodes) { _ ++ _.keySet }

        emptySet(pc, allUnmodified & pc.modifiedKeys)
      }
    ),

    ("no duplicates after renumbering observations",
      (start, local, remote, pc) => {
        val empty = Map.empty[Int, SPNodeKey]
        val (localOnly, remoteOnly) = pc.mergePlan.update.foldObservations((empty, empty)) { case (m, i, _, (lo, ro)) =>
          val key = m.key
          (pc.local.p.getVersions.contains(key), pc.remote.p.getVersions.contains(key)) match {
            case (true, false) => (lo + (i -> key), ro)
            case (false, true) => (lo, ro + (i -> key))
            case _             => (lo, ro)
          }
        }

        localOnly.isEmpty || remoteOnly.isEmpty || {
          val localKeys = localOnly.values.toSet
          val remoteMax = remoteOnly.keySet.max

          ObsNumberCorrection(pc.mergeContext).apply(pc.mergePlan) match {
            case -\/(Unmergeable(msg)) =>
              // This might start to fail if we update the generator to produce
              // events or datasets in observation exec logs.
              Console.err.println(s"Unmergeable: $msg")
              false

            case \/-(mp) =>
              val t        = mp.update
              val emptyMap = Map.empty[Int, Set[SPNodeKey]].withDefaultValue(Set.empty[SPNodeKey])
              val obsMap   = t.foldRight(emptyMap) {
                case (Modified(k, _, _, Obs(n)), m) => m.updated(n, m(n) + k)
                case (_, m)                         => m
              }

              val renumberedKeys = (Set.empty[SPNodeKey]/:((remoteMax + 1) to remoteMax + localKeys.size)) { (s, i) =>
                s ++ obsMap(i)
              }

              obsMap.values.forall(_.size == 1) && (localKeys == renumberedKeys)
          }
        }
      }
    )
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
