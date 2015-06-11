package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.Conflict.{Moved, ResurrectedLocalDelete, ReplacedRemoteDelete}
import edu.gemini.pot.sp.{Conflict, DataObjectBlob, ISPNode, SPNodeKey}
import edu.gemini.shared.util.VersionComparison
import edu.gemini.shared.util.VersionComparison._
import edu.gemini.sp.vcs2.MergeNode._
import edu.gemini.spModel.rich.pot.sp._

import scalaz.\&/.{Both, This, That}
import scalaz._
import Scalaz._

/** Produces a preliminary [[MergePlan]]. Before using it to complete a merge
  * however, various special case corrections (e.g., observation renumbering)
  * must be applied to the plan. */
object PreliminaryMerge {

  def merge(mc: MergeContext): TryVcs[MergePlan] =
    tree(mc).map { t =>
      val mergedKeys  = t.foldRight(Set.empty[SPNodeKey]) { (mn, s) => s + mn.key }
      val allKeys     = mc.remote.diffMap.keySet ++ mc.remote.plan.delete.map(_.key)
      val deletedKeys = allKeys &~ mergedKeys
      val allMissing  = deletedKeys.map { k => Missing(k, mc.local.version(k).sync(mc.remote.version(k))) }

      MergePlan(t, allMissing)
    }

  def tree(mc: MergeContext): TryVcs[Tree[MergeNode]] = {

    def isNewLocal(l: ISPNode)          = !mc.remote.isKnown(l.key)
    def isNewRemote(r: Tree[MergeNode]) = !mc.local.isKnown(r.key)

    def isUpdated(k: SPNodeKey, pc0: ProgContext, pc1: ProgContext): Boolean =
      pc0.version(k).compare(pc1.version(k)) match {
        case Newer | Conflicting => true
        case _                   => false
      }

    def isUpdatedLocal(l: ISPNode)          = isUpdated(l.key, mc.local, mc.remote)
    def isUpdatedRemote(r: Tree[MergeNode]) = isUpdated(r.key, mc.remote, mc.local)

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
          // The only way they can both be defined and yet different is if one
          // or the other (or both) local and remote sides have an updated
          // parent and child version.  In this case remote wins when updated.
          val rParentUp = rParentKey.flatMap(mc.remote.get).exists(isUpdatedRemote)
          val rChildUp  = mc.remote.get(childKey).exists(isUpdatedRemote)
          if (rParentUp && rChildUp) Some(rKey) else Some(lKey)
      }
    }

    def keep(k: SPNodeKey, pc: ProgContext): Boolean = pc.parent(k) == mergeParent(k)
    def keepLocalChild(l: ISPNode): Boolean          = keep(l.key, mc.local)
    def keepRemoteChild(r: Tree[MergeNode]): Boolean = keep(r.key, mc.remote)

    def containsUpdatedLocal(l: ISPNode): Boolean =
      isUpdatedLocal(l) || l.children.exists(containsUpdatedLocal)

    def containsUpdatedRemote(r: Tree[MergeNode]): Boolean =
      isUpdatedRemote(r) || r.subForest.exists(containsUpdatedRemote)

    def containsMissingRemoteUpdatedLocal(l: ISPNode): Boolean =
      !mc.remote.isPresent(l.key) && (
        isUpdatedLocal(l) || l.children.exists(containsMissingRemoteUpdatedLocal)
      )

    def containsMissingLocalUpdatedRemote(r: Tree[MergeNode]): Boolean =
      !mc.local.isPresent(r.key) && (
        isUpdatedRemote(r) || r.subForest.exists(containsMissingLocalUpdatedRemote)
      )


    type ChildFilter = PartitionedChildren => PartitionedChildren

    // Filter for local nodes that are newer.
    val newer: ChildFilter = pc => {
      // Keep all local-only children not in a different updated parent remotely.
      val local = pc.local.filter(keepLocalChild)

      // In general we don't want the remote nodes -- we're deleting them.
      // If deleting something that contains an update we haven't seen though,
      // we want to keep it anyway.
      val remote = pc.remote.filter(containsMissingLocalUpdatedRemote)

      PartitionedChildren(local, pc.both, remote)
    }

    // Filter for local nodes that resurrect remotely deleted nodes.  Keep
    // all children that have been updated locally and which don't belong to
    // some other node.
    val deletedRemotely: ChildFilter = pc =>
      pc.copy(local = pc.local.filter { child =>
        keepLocalChild(child) && containsUpdatedLocal(child)
      })

    // Filter for local nodes that are older than their remote counterparts.
    // This is basically the opposite of "newer".
    val older: ChildFilter = pc => {
      val local  = pc.local.filter(containsMissingRemoteUpdatedLocal)
      val remote = pc.remote.filter(keepRemoteChild)
      PartitionedChildren(local, pc.both, remote)
    }

    // Filter for remote nodes that have been deleted locally.  The opposite of
    // "deletedRemotely".
    val deletedLocally: ChildFilter = pc =>
      pc.copy(remote = pc.remote.filter { child =>
        keepRemoteChild(child) && containsUpdatedRemote(child)
      })

    // Filter for conflicting edits.  When conflicting, the only local-only
    // children we keep are those that contain some local update and which
    // don't belong in some other node (we let any remote deletions through)..
    val conflicting: ChildFilter = pc => {
      val local  = pc.local.filter { child =>
        keepLocalChild(child) && containsUpdatedLocal(child)
      }

      val remote = pc.remote.filter { child =>
        keepRemoteChild(child)
      }

      PartitionedChildren(local, pc.both, remote)
    }

    def toNode(mod: MergeNode, lcs: List[ISPNode], rcs: List[Tree[MergeNode]], f: ChildFilter): Tree[MergeNode] = {
      // Filter the children according to the version information.
      val pc = f(PartitionedChildren.part(lcs, rcs))

      // Merge the local only, both, and remote only children.
      val lMerged = pc.local.map { lc =>
        mc.remote.get(lc.key).fold(go(This(lc))) { rc => go(Both(lc, rc)) }
      }

      val bMerged = pc.both.map { case (lc, rc) => go(Both(lc,rc)) }

      val rMerged = pc.remote.map { rc =>
        mc.local.get(rc.key).fold(go(That(rc))) { lc => go(Both(lc, rc)) }
      }

      // Order the children.
      def ordering[B](lst: List[B])(fk: B => SPNodeKey): Map[SPNodeKey, Int] =
        lst.zipWithIndex.map { case (b, i) => fk(b) -> i}.toMap

      val localOrder  = ordering(lcs)(_.key)
      val remoteOrder = ordering(rcs)(_.key)

      val sortedLocal  = (lMerged ++ bMerged).sortBy(c => localOrder(c.key))
      val sortedRemote = rMerged.sortBy(c => remoteOrder(c.key))

      // TODO: insert the remote children into sortedLocal according to some
      // TODO: heuristic instead of just appending?
      val newChildren = sortedLocal ++ sortedRemote

      // Compute the new version for this node, which is nominally the
      // combination of the local and remote versions. If the children don't
      // match what we have locally though (that is, they have been updated by
      // the merge), then be sure that the local version is updated.

      def sameChildren: Boolean = {
        val localNode      = mc.local.get(mod.key)
        val localChildKeys = localNode.map(_.children.map(_.key)) | Nil
        val newChildKeys   = newChildren.map(_.key)
        localChildKeys == newChildKeys
      }

      val k          = mod.key
      val sv         = mc.syncVersion(k)
      def svIncr     = sv.incr(mc.local.prog.getLifespanId)
      val svIsNewer  = sv.compare(mc.local.version(k)) === VersionComparison.Newer
      val newVersion = if (svIsNewer || sameChildren) sv else svIncr

      val mod2 = mod match {
        case m: Modified   => m.copy(nv = newVersion)
        case _: Unmodified => mod
      }

      Tree.node(mod2, newChildren.toStream)
    }


    def go(lr: ISPNode \&/ Tree[MergeNode]): Tree[MergeNode] = {
      val incr0: Tree[MergeNode] => Tree[MergeNode] = identity

      def incr1(n: Tree[MergeNode]): Tree[MergeNode] =
        n.rootLabel match {
          case m: Modified   =>
            Tree.node(m.copy(nv = m.nv.incr(mc.local.prog.getLifespanId)), n.subForest)
          case _             =>
            n
        }

      lr match {
        case This(l) =>
          val (incr, filt) = if (isNewLocal(l)) (incr0, newer)  // New local node
                             else (incr1 _, deletedRemotely)    // Resurrected local node
          incr(toNode(modified(l), l.children, Nil, filt))

        case That(r) =>
          val (incr, filt) = if (isNewRemote(r)) (incr0, older) // New remote node
                             else (incr1 _, deletedLocally)     // Resurrected remote node
          incr(toNode(r.rootLabel, Nil, r.subForest.toList, filt))


        case Both(l, r) => r.rootLabel match {
          case _: Unmodified => r
          case m: Modified   =>
            val lc = l.children
            val rc = r.subForest.toList
            l.getVersion.compare(m.nv) match {
              case Same        => toNode(r.rootLabel, lc, rc, identity)
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
        } yield n1.toTree
      }
    }

    // Given the root of the tree and the set of keys that have been replaced or
    // resurrected, find the roots of subtrees with the conflict issue and just
    // add the note to the roots. For example, if an observation is replaced we
    // don't want to see conflict notes on every node in the observation, but
    // rather just on the observation node itself.
    def addNotes(in: Tree[MergeNode], ks: Set[SPNodeKey], nf: SPNodeKey => Conflict.Note): TryVcs[Tree[MergeNode]] = {
      def goAdd(r: Tree[MergeNode]): TryVcs[Tree[MergeNode]] =
        if (ks.contains(r.key))
          for {
            t0 <- r.mModifyLabel(_.withConflictNote(nf(r.key)))
            t1 <- visitChildren(t0, goSkip)
          } yield t1
        else
          visitChildren(r, goAdd)

      // If this node is in the set, we skip the note we would otherwise have
      // added.  If not, from that point down we will go back to adding the note
      def goSkip(r: Tree[MergeNode]): TryVcs[Tree[MergeNode]] =
        visitChildren(r, if (ks.contains(r.key)) goSkip else goAdd)

      def visitChildren(t: Tree[MergeNode], fun: Tree[MergeNode] => TryVcs[Tree[MergeNode]]): TryVcs[Tree[MergeNode]] =
        t.subForest.map(fun).sequenceU.map(children => Tree.node(t.rootLabel, children))

      goAdd(in)
    }

    def addReplacedRemoteDelete(in: Tree[MergeNode]): TryVcs[Tree[MergeNode]] = {
      val mergeSurvivors = in.keySet
      val remoteDeleted  = mc.remote.diff.plan.delete.collect {
        case Missing(k, _) if mc.remote.isKnown(k) => k
      }
      val replaced = mergeSurvivors & remoteDeleted

      // check for empty to avoid an unnecessary traversal.  would work anyway
      if (replaced.isEmpty) TryVcs(in)
      else addNotes(in, replaced, new ReplacedRemoteDelete(_))
    }

    def addResurrectedLocalDelete(in: Tree[MergeNode]): TryVcs[Tree[MergeNode]] = {
      val mergeSurvivors = in.keySet
      val localDeleted   = mc.remote.diff.plan.update.keySet.filter(mc.local.isDeleted)
      val resurrected    = mergeSurvivors & localDeleted

      // check for empty to avoid an unnecessary traversal.  would work anyway
      if (resurrected.isEmpty) TryVcs(in)
      else addNotes(in, resurrected, new ResurrectedLocalDelete(_))
    }

    def addMoved(in: Tree[MergeNode]): TryVcs[Tree[MergeNode]] = {
      // moved = List: (old parent, child, new parent)
      val moved = in.foldTree(List.empty[(SPNodeKey, SPNodeKey, SPNodeKey)]) { (newParent, lst) =>
        (lst/:newParent.subForest) { (lst0, child) =>
          mc.local.parent(child.key).fold(lst0) { oldParentKey =>
            val isMoved = (oldParentKey =/= newParent.key) &&
                            isUpdated(oldParentKey, mc.local, mc.remote)
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
        } yield n1.toTree
      }
    }

    def addConflicts(in: Tree[MergeNode]): TryVcs[Tree[MergeNode]] =
      for {
        t0 <- addDataObjectConflicts(in)
        t1 <- addReplacedRemoteDelete(t0)
        t2 <- addResurrectedLocalDelete(t1)
        t3 <- addMoved(t2)
      } yield t3

    addConflicts(go(Both(mc.local.prog, mc.remote.plan.update)))
  }
}