package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.Conflict.Moved
import edu.gemini.pot.sp.{DataObjectBlob, ISPNode, SPNodeKey}
import edu.gemini.shared.util.VersionComparison._
import edu.gemini.sp.vcs2.MergeNode._
import edu.gemini.spModel.rich.pot.sp._

import scalaz.\&/.{Both, That, This}
import scalaz._
import Scalaz._
import scalaz.Tree.Node

/** Produces a preliminary [[MergePlan]]. Before using it to complete a merge
  * however, various special case corrections (e.g., observation renumbering)
  * must be applied to the plan. */
object PreliminaryMerge {

  def merge(mc: MergeContext): TryVcs[MergePlan] =
    tree(mc).map { t =>
      val mergedKeys  = t.sFoldRight(Set.empty[SPNodeKey]) { (mn, s) => s + mn.key }
      val allKeys     = mc.remote.diffMap.keySet ++ mc.remote.plan.delete.map(_.key)
      val deletedKeys = allKeys &~ mergedKeys
      val allMissing  = deletedKeys.map { k => Missing(k, mc.local.version(k).sync(mc.remote.version(k))) }

      MergePlan(t, allMissing)
    }

  def tree(mc: MergeContext): TryVcs[Tree[MergeNode]] = {

    def isVersonUpdated(k: SPNodeKey, pc0: ProgContext, pc1: ProgContext): Boolean =
      pc0.version(k).updates(pc1.version(k))

    def isUpdatedLocal(l: ISPNode)          = isVersonUpdated(l.key, mc.local, mc.remote)
    def isUpdatedRemote(r: Tree[MergeNode]) = isVersonUpdated(r.key, mc.remote, mc.local)

    // Defines the rules for determining which parent wins in case of ambiguities.
    def mergeParent(childKey: SPNodeKey): Option[SPNodeKey] = {
      val lParentKey = mc.local.parent(childKey)
      val rParentKey = mc.remote.parent(childKey)

      (lParentKey, rParentKey) match {
        case (None, None)                              => None
        case (Some(lKey), None)                        => Some(lKey)
        case (None, Some(rKey))                        => Some(rKey)
        case (Some(lKey), Some(rKey)) if lKey === rKey => Some(lKey)
        case (Some(lKey), Some(rKey))                  =>
          if (rParentKey.flatMap(mc.remote.get).exists(isUpdatedRemote)) Some(rKey)
          else Some(lKey)
      }
    }

    def keep(k: SPNodeKey, pc: ProgContext): Boolean = pc.parent(k) == mergeParent(k)
    def keepLocalChild(l: ISPNode): Boolean          = keep(l.key, mc.local)
    def keepRemoteChild(r: Tree[MergeNode]): Boolean = keep(r.key, mc.remote)

    def containsUpdatedLocal(l: ISPNode): Boolean =
      isUpdatedLocal(l) || l.children.exists(containsUpdatedLocal)

    def containsUpdatedRemote(r: Tree[MergeNode]): Boolean =
      isUpdatedRemote(r) || r.subForest.exists(containsUpdatedRemote)

    def containsMissingRemoteYetUpdatedLocal(l: ISPNode): Boolean =
      !mc.remote.isPresent(l.key) && (
        isUpdatedLocal(l) || l.children.exists(containsMissingRemoteYetUpdatedLocal)
      )

    def containsMissingLocalYetUpdatedRemote(r: Tree[MergeNode]): Boolean =
      !mc.local.isPresent(r.key) && (
        isUpdatedRemote(r) || r.subForest.exists(containsMissingLocalYetUpdatedRemote)
      )

    sealed trait ChildMergeStrategy {
      def filter(pc: PartitionedChildren): PartitionedChildren
      def sort(merged: List[Tree[MergeNode]], lcs: List[ISPNode], rcs: List[Tree[MergeNode]]): List[Tree[MergeNode]]
    }

    sealed trait LocalMergeStrategy extends ChildMergeStrategy {
      final def sort(merged: List[Tree[MergeNode]], lcs: List[ISPNode], rcs: List[Tree[MergeNode]]): List[Tree[MergeNode]] =
        SortHeuristic.sort(merged, lcs.map(_.key), rcs.map(_.key))(_.key)
    }

    sealed trait RemoteMergeStrategy extends ChildMergeStrategy {
      final def sort(merged: List[Tree[MergeNode]], lcs: List[ISPNode], rcs: List[Tree[MergeNode]]): List[Tree[MergeNode]] =
        SortHeuristic.sort(merged, rcs.map(_.key), lcs.map(_.key))(_.key)
    }

    // Strategy for when there is no edit to either side.
    val same = new RemoteMergeStrategy {
      def filter(pc: PartitionedChildren): PartitionedChildren = pc
    }

    // Strategy for local nodes that are newer.
    val newer = new LocalMergeStrategy {
      def filter(pc: PartitionedChildren): PartitionedChildren = {
        // Keep all local-only children not in a different updated parent remotely.
        val local = pc.local.filter(keepLocalChild)

        // In general we don't want the remote nodes -- we're deleting them.
        // If deleting something that contains an update we haven't seen though,
        // we want to keep it anyway.
        val remote = pc.remote.filter(containsMissingLocalYetUpdatedRemote)

        PartitionedChildren(local, pc.both, remote)
      }
    }

    // Strategy for local nodes that resurrect remotely deleted nodes.  Keep
    // all children that have been updated locally and which don't belong to
    // some other node.
    val deletedRemotely = new LocalMergeStrategy {
      def filter(pc: PartitionedChildren): PartitionedChildren =
        pc.copy(local = pc.local.filter { child =>
          keepLocalChild(child) && containsUpdatedLocal(child)
        })
    }

    // Strategy for local nodes that are older than their remote counterparts.
    val older = new RemoteMergeStrategy {
      def filter(pc: PartitionedChildren): PartitionedChildren = {
        val local  = pc.local.filter(containsMissingRemoteYetUpdatedLocal)
        PartitionedChildren(local, pc.both, pc.remote)
      }
    }

    // Strategy for remote nodes that have been deleted locally.  The opposite
    // of "deletedRemotely".
    val deletedLocally = new RemoteMergeStrategy {
      def filter(pc: PartitionedChildren): PartitionedChildren =
        pc.copy(remote = pc.remote.filter { child =>
          keepRemoteChild(child) && containsUpdatedRemote(child)
        })
    }

    // Strategy for conflicting edits.  When conflicting, the only local-only
    // children we keep are those that contain some local update and which
    // don't belong in some other node (we let any remote deletions through)..
    val conflicting = new RemoteMergeStrategy {
      def filter(pc: PartitionedChildren): PartitionedChildren = {
        val local = pc.local.filter { child =>
          (keepLocalChild(child) && containsUpdatedLocal(child)) ||
          mc.remote.parent(child.key).flatMap(mc.remote.get).exists(p => !isUpdatedRemote(p))
        }

        val remote = pc.remote.filterNot { child =>
          mc.local.isDeleted(child.key) && !containsUpdatedRemote(child)
        }

        PartitionedChildren(local, pc.both, remote)
      }
    }


    def toNode(mod: MergeNode, lcs: List[ISPNode], rcs: List[Tree[MergeNode]], s: ChildMergeStrategy): Tree[MergeNode] = {
      // Filter the children according to the version information.
      val pc = s.filter(PartitionedChildren.part(lcs, rcs))

      // Merge the local only, both, and remote only children.
      val lMerged = pc.local.map { lc =>
        mc.remote.get(lc.key).fold(go(This(lc))) { rc => go(Both(lc, rc)) }
      }

      val bMerged = pc.both.map { case (lc, rc) => go(Both(lc,rc)) }

      val rMerged = pc.remote.map { rc =>
        mc.local.get(rc.key).fold(go(That(rc))) { lc => go(Both(lc, rc)) }
      }

      // Combine and order the children.
      val newChildren  = s.sort(lMerged ++ rMerged ++ bMerged, lcs, rcs)

      // Compute the new version for this node, which is nominally the
      // combination of the local and remote versions. If the children don't
      // match though (that is, they have been updated by the merge), then be
      // sure that the local version is updated.

      val k            = mod.key
      val syncVersion  = mc.syncVersion(k)
      def incrVersion  = syncVersion.incr(mc.local.prog.getLifespanId)
      val newChildKeys = newChildren.map(_.key)

      def updatesLocalVersion: Boolean   = syncVersion.updates(mc.local.version(k))
      def updatesRemoteVersion: Boolean  = syncVersion.updates(mc.remote.version(k))
      def updatesLocalChildren: Boolean  = newChildKeys =/= (mc.local.get(k).map(_.children.map(_.key)) | Nil)
      def updatesRemoteChildren: Boolean = newChildKeys =/= (mc.remote.get(k).map(_.subForest.toList.map(_.key)) | Nil)

      val newVersion =
        if ((!updatesLocalVersion && updatesLocalChildren) || (!updatesRemoteVersion && updatesRemoteChildren))
          incrVersion
        else
          syncVersion

      val mod2 = mod match {
        case m: Modified   => m.copy(nv = newVersion)
        case _: Unmodified => mod
      }

      Node(mod2, newChildren.toStream)
    }


    def go(lr: ISPNode \&/ Tree[MergeNode]): Tree[MergeNode] = {
      val incr0: Tree[MergeNode] => Tree[MergeNode] = identity

      def incr1(n: Tree[MergeNode]): Tree[MergeNode] =
        n.rootLabel match {
          case m: Modified   =>
            Node(m.copy(nv = m.nv.incr(mc.local.prog.getLifespanId)), n.subForest)
          case _             =>
            n
        }

      lr match {
        case This(l) =>
          val (incr, filt) = if (isUpdatedLocal(l)) (incr0, newer)  // New or updated local node
                             else (incr1 _, deletedRemotely)        // Resurrected local node
          incr(toNode(modified(l), l.children, Nil, filt))

        case That(r) =>
          val (incr, filt) = if (isUpdatedRemote(r)) (incr0, older) // New or updated remote node
                             else (incr1 _, deletedLocally)         // Resurrected remote node
          incr(toNode(r.rootLabel, Nil, r.subForest.toList, filt))


        case Both(l, r) => r.rootLabel match {
          case _: Unmodified => r
          case m: Modified   =>
            val lc = l.children
            val rc = r.subForest.toList
            l.getVersion.compare(m.nv) match {
              case Same        => toNode(r.rootLabel, lc, rc, same)
              case Newer       => toNode(modified(l), lc, rc, newer)
              case Older       => toNode(r.rootLabel, lc, rc, older)
              case Conflicting => toNode(r.rootLabel, lc, rc, conflicting)
            }
        }
      }
    }

    def addDataObjectConflicts(in: Tree[MergeNode]): TryVcs[Tree[MergeNode]] = {
      val commonKeys = mc.remote.diffMap.keySet & mc.local.nodeMap.keySet

      def dobConflicts(k: SPNodeKey): Boolean =
        mc.local.version(k).compare(mc.remote.version(k)) === Conflicting && {
          val local  = mc.local.nodeMap(k).getDataObject
          val remote = mc.remote.diffMap(k).rootLabel match {
            case Modified(_, _, dob, _, _) => Some(dob)
            case Unmodified(_)             => None
          }
          remote.exists(dob => !DataObjectBlob.same(local, dob))
        }

      val conflicts = commonKeys.collect { case k if dobConflicts(k) =>
        k -> mc.local.nodeMap(k).getDataObject
      }

      (TryVcs(in)/:conflicts) { case(tryTree,(k,dob)) =>
        for {
          t  <- tryTree
          n0 <- t.loc.findNode(k)
          n1 <- n0.addDataObjectConflict(dob)
          n2 <- n1.incr(mc.local.prog.getLifespanId)
        } yield n2.toTree
      }
    }

    def addMoved(in: Tree[MergeNode]): TryVcs[Tree[MergeNode]] = {
      // moved = List: (old parent, child, new parent)
      val moved = in.foldTree(List.empty[(SPNodeKey, SPNodeKey, SPNodeKey)]) { (newParent, lst) =>
        (lst/:newParent.subForest) { (lst0, child) =>
          mc.local.parent(child.key).fold(lst0) { oldParentKey =>
            val isMoved = (oldParentKey =/= newParent.key) &&
                            isVersonUpdated(oldParentKey, mc.local, mc.remote)
            if (isMoved) (oldParentKey, child.key, newParent.key) :: lst0
            else lst0
          }
        }
      }

      (TryVcs(in)/:moved) { case (tryTree, (oldParent, child, newParent)) =>
        for {
          t  <- tryTree
          n0 <- t.loc.findNode(oldParent)
          n1 <- n0.addConflictNote(new Moved(child, newParent))
          n2 <- n1.incr(mc.local.prog.getLifespanId)
        } yield n2.toTree
      }
    }

    def addConflicts(in: Tree[MergeNode]): TryVcs[Tree[MergeNode]] =
      for {
        t0 <- addDataObjectConflicts(in)
        t1 <- addMoved(t0)
      } yield t1

    addConflicts(go(Both(mc.local.prog, mc.remote.plan.update)))
  }
}