package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.version._
import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.pot.sp.version.VersionComparison._
import edu.gemini.sp.vcs.diff.Diff._
import edu.gemini.spModel.rich.pot.sp._

import scala.collection.breakOut
import scalaz._
import Scalaz._

/** Produces a preliminary [[MergePlan]]. Before using it to complete a merge
  * however, various special case corrections (e.g., observation renumbering)
  * must be applied to the plan.
  */
object PreliminaryMerge {

  def merge(mc: MergeContext): MergePlan =
    plan(mc, mergedDiffs(mc))

  // TODO: remove after upgrading to scalaz 7.1
  implicit class HackityHack[A,B](abs: List[A \/ B]) {
    def separate: (List[A], List[B]) =
      abs.foldRight((List.empty[A], List.empty[B])) {
        case (-\/(a), (as, bs)) => (a :: as, bs)
        case (\/-(b), (as, bs)) => (as, b :: bs)
      }
  }

  private def mergedDiffs(mc: MergeContext): (List[Missing], List[Present]) =
    mc.remote.diffs.map { diff =>
      val key  = diff.key
      (mc.local.get(key), diff) match {
        case (None, Missing(_, nv)) =>
          Missing(key, nv.sync(mc.local.version(key))).left

        case (Some(n), _: Missing) =>
          val localP   = present(n)
          val remoteP  = localP.copy(nv = mc.remote.version(key), children = Nil)
          mergedPresent(mc, localP, Newer, remoteP).right

        case (None,    p: Present) =>
          val localP   = p.copy(nv = mc.local.version(key), children = Nil)
          mergedPresent(mc, localP, Older, p).right

        case (Some(n), p: Present) =>
          val comp     = VersionComparison.compare(mc.local.version(key), diff.nv)
          mergedPresent(mc, present(n), comp, p).right
      }
    }.separate

  private def mergedPresent(mc: MergeContext, local: Present, comp: VersionComparison, remote: Present): Present = {
    val key = local.key

    val present = comp match {
      case Same        =>
        local

      case Newer       =>
        val localResurrect = remote.children.diff(local.children).filter { c =>
          mc.local.isDeleted(c) && mc.remote.isTreeModified(c)
        }
        local.copy(children = localResurrect ++ local.children)

      case Older       =>
        val remoteResurrect = local.children.diff(remote.children).filter { c =>
          mc.remote.isDeleted(c) && mc.local.isTreeModified(c)
        }
        remote.copy(children = remoteResurrect ++ remote.children)

      case Conflicting =>
        val mergedChildren = local.children.diff(remote.children) ++ remote.children
        remote.copy(children = mergedChildren)
    }

    present.copy(nv = local.nv.sync(remote.nv))
  }


  private def plan(mc: MergeContext, mdiffs: (List[Missing], List[Present])): MergePlan = {
    val (missing, presents) = mdiffs

    val presentMap = presents.map(p => p.key -> p)(breakOut): Map[SPNodeKey, Present]
    val parentMap  = validParents(mc, presentMap)

    val root: MergeNode = {
      def go(root: SPNodeKey): MergeNode =
        presentMap.get(root).fold(UnmodifiedNode(mc.local.nodeMap(root)): MergeNode) { p =>
          val up = Update(p.key, p.nv, p.dob, p.detail)
          val cs = p.children.filter(c => parentMap(c) === p.key)
          ModifiedNode(up, cs.map(go))
        }
      go(mc.local.prog.key)
    }

    val mergedKeys  = MergeNode.fold(Set.empty[SPNodeKey], root) { _ + _.key }
    val deletedKeys = presentMap.keySet &~ mergedKeys

    // This node has been deleted in the merged tree so turn it into a Missing
    // Diff.
    val deleted = deletedKeys.map { k =>
      val p = presentMap(k)
      Missing(p.key, p.nv)
    }

    MergePlan(root, deleted ++ missing)
  }

  //
  // validParents - returns a map from child key to the correct parent key
  //
  // The same child node may appear in two different Present Diffs.  For
  // example, consider a program with three groups: g1, g2, and g3.  Assume
  // g1 has a node n and g2 and g3 are both empty.  Now locally we move n to g2
  // while someone else moves n to g3.  In this case the merge at this point
  // will produce merged Presents which each consider n to be a child since g2
  // is newer locally and g3 is newer remotely.
  //
  // The purpose of this method is to establish which parent to use in this
  // case.  We choose the parent in the remote version of the program.
  //
  private def validParents(mc: MergeContext, m: Map[SPNodeKey, Present]): Map[SPNodeKey, SPNodeKey] = {
    // fold over the map with a DF traversal, ignoring values that aren't
    // reachable
    def fold[A](root: SPNodeKey, z: A)(op: (A, Present) => A): A = {
      def go(rem: List[SPNodeKey], res: A): A =
        rem match {
          case Nil     => res
          case k :: ks =>
            val (children, res2) = m.get(k).fold((List.empty[SPNodeKey], res)) { p =>
              (p.children, op(res, p))
            }
            go(children ++ ks, res2)
        }
      go(List(root), z)
    }

    // returns Map[SPNodeKey, SPNodeKey] where the keys are child keys and
    // the values are their corresponding parent key
    fold(mc.local.prog.key, Map.empty[SPNodeKey, SPNodeKey]) { (pMap, p) =>
      (pMap/:p.children) { (pMap2, childKey) =>
        pMap2.get(childKey).fold(pMap2 + (childKey -> p.key)) { existingParent =>
          if ((existingParent === p.key) || mc.remote.parent(childKey).exists(_ === existingParent))
            pMap2
          else
            pMap2.updated(childKey, p.key)
        }
      }
    }
  }
}