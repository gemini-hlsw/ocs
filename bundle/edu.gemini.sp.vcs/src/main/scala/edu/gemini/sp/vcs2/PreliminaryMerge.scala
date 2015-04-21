package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.Conflict.{Moved, ResurrectedLocalDelete, ReplacedRemoteDelete}
import edu.gemini.pot.sp.{Conflict, DataObjectBlob, ISPNode, SPNodeKey}
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

    def isUpdated(k: SPNodeKey, pc0: ProgContext, pc1: ProgContext): Boolean =
      pc0.version(k).compare(pc1.version(k)) match {
        case Newer | Conflicting => true
        case _                   => false
      }

    def isUpdatedLocal(l: ISPNode)          = isUpdated(l.key, mc.local, mc.remote)
    def isUpdatedRemote(r: Tree[MergeNode]) = isUpdated(r.key, mc.remote, mc.local)

    def isUpdatedLocalParent(r: Tree[MergeNode]): Boolean =
      mc.local.parent(r.key).flatMap(mc.local.get).exists(isUpdatedLocal)

    def isUpdatedRemoteParent(l: ISPNode): Boolean =
      mc.remote.parent(l.key).flatMap(mc.remote.get).exists(isUpdatedRemote)

    def isDeletedRemoteButUpdatedLocal(l: ISPNode): Boolean =
      mc.remote.isDeleted(l.key) && (
        isUpdatedLocal(l) || l.children.exists(isDeletedRemoteButUpdatedLocal)
      )

    def isDeletedLocalButUpdatedRemote(r: Tree[MergeNode]): Boolean =
      mc.local.isDeleted(r.key) && (
        isUpdatedRemote(r) || r.subForest.exists(isDeletedLocalButUpdatedRemote)
      )


    type ChildFilter = PartitionedChildren => PartitionedChildren

    val newer: ChildFilter = pc => {
      // Keep all local-only children not in a different updated parent remotely.
      // That is, when the local and remote disagree about where to put a node,
      // the remote version wins.
      val local = pc.local.filterNot(isUpdatedRemoteParent)

      // Keep those remote-only nodes deleted locally (i.e., not elsewhere
      // locally) and that have a descendant which is modified remotely.
      // Otherwise we're newer so they are deleted.
      val remote = pc.remote.filter(isDeletedLocalButUpdatedRemote)

      PartitionedChildren(local, pc.both, remote)
    }

    // Deleted remotely is only used when the local node is not updated
    // (otherwise `newer` is used). Since the node has not been updated but has
    // been deleted remotely, we get rid of all children that don't have a
    // descendant which has been updated locally.
    val deletedRemotely: ChildFilter = pc =>
      pc.copy(local = pc.local.filter(isDeletedRemoteButUpdatedLocal))

    // Keep only those local children that are deleted remotely (i.e., not
    // elsewhere remotely) and that have a descendant which is modified locally.
    // All remote children are kept.
    val older: ChildFilter = pc =>
      pc.copy(local = pc.local.filter(isDeletedRemoteButUpdatedLocal))

    // Deleted locally is only used when the remote node is not updated
    // (otherwise `older` is used).  Since the node has not been updated but has
    // been deleted locally, we git rid of all children that don't have a
    // descendant which has been updated remotely.
    val deletedLocally: ChildFilter = pc =>
      pc.copy(remote = pc.remote.filter(isDeletedLocalButUpdatedRemote))

    // When conflicting, we remove any local-only nodes that are not new,
    // deleted remotely but updated locally or that are in some other updated
    // parent in the remote version.
    val conflicting: ChildFilter = pc =>
      pc.copy(local = pc.local.filter { lc =>
        !mc.remote.isKnown(lc.key) ||
        isDeletedRemoteButUpdatedLocal(lc) ||
        mc.remote.parent(lc.key).flatMap(mc.remote.get).exists(p => !isUpdatedRemote(p))
      })


    def toNode(mod: MergeNode, lcs: List[ISPNode], rcs: List[Tree[MergeNode]], f: ChildFilter): Tree[MergeNode] = {
      // Update the node versions to incorporate local and remote edits.
      val mod2 = mod match {
        case m: Modified   => m.copy(nv = mc.syncVersion(m.key))
        case _: Unmodified => mod
      }

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
      Tree.node(mod2, (sortedLocal ++ sortedRemote).toStream)
    }

    def go(lr: ISPNode \&/ Tree[MergeNode]): Tree[MergeNode] =
      lr match {
        case This(l)    =>
          toNode(modified(l), l.children, Nil, if (isUpdatedLocal(l)) newer else deletedRemotely)

        case That(r)    =>
          toNode(r.rootLabel, Nil, r.subForest.toList, if (isUpdatedRemote(r)) older else deletedLocally)

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