package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.version._
import edu.gemini.pot.sp.{ISPNode, ISPObservation, ISPProgram, SPNodeKey}
import edu.gemini.sp.vcs.diff.MergeNode._
import edu.gemini.spModel.rich.pot.sp._

import scalaz._
import Scalaz._

object ProgramDiff {

  /** Returns a (super-set) of differences between two programs.
    *
    * It is a "super-set" because ultimately not every returned difference may
    * be necessary for merging program versions.
    *
    * Each instance/copy of a program has its own unique id.  Each node in a
    * program maintains a map from unique program id to a counter representing
    * the number of edits made to the node in the associated program instance.
    * This map is called `NodeVersions`, a type alias for a
    * [[edu.gemini.shared.util.VersionVector]] keyed by unique program id with
    * `java.lang.Integer` values.  A map of all the `NodeVersions` keyed by the
    * node's `SPNodeKey` is a `VersionMap`. An edit to either the node's "data
    * object" or child list increments the counter associated with the program's
    * unique id.
    *
    * This method compares a local program with the `VersionMap` of a remote
    * copy of the program. It generates a `MergePlan` to describe the
    * differences and allow the remote program to incorporate any changes that
    * have been made locally.  The `MergePlan` lists `Missing` elements that
    * are unknown to this program either because they have been deleted or
    * because they have never been seen before.  It also provides a
    * `scalaz.Tree` of `MergeNode`s (`Modified` and `Unmodified`) corresponding
    * to existing (potentially) edited nodes.
    *
    * The comparison must generate
    *
    * - `Missing` diffs for any node not in the program that either (a) is
    * still in the other version of the program or (b) has different version
    * information than the other program.
    *
    * - `Modified` diffs for any node in the program with different version
    * information than its counterpart.
    *
    * - `Modified` diffs for all nodes in an observation that have at least one
    * contained node with a difference in version data.  In other words, any
    * difference in an observation will bring `Modified` diffs for the entire
    * observation.
    *
    * - `Modified` diffs for all ancestor nodes of a node with different version
    * data.  For example, an edited note in a group will pull in the group and
    * the program ancestors.
    *
    * Conversely, the comparison must not
    *
    * - include `Modified` diffs for any node which has the same version
    * information as the corresponding node in the other copy of the program
    * and which has no descendants with different version data
    *
    * - include `Missing` diffs for nodes deleted both locally and remotely and
    * that have the same version data
    *
    * It should however include `Unmodified` diffs to mark places in the
    * science program tree that do not differ.  For example, if there are no
    * differences whatsoever a single `Unmodified` node corresponding to the
    * root is still returned.
    *
    * @param p program from which differences are to be extracted
    *
    * @param vm `VersionMap` of the other instance of the program
    *
    * @param removed `Set` of all node keys that are not present in the
    *                other instance of the program
    *
    * @return `MergePlan` describing differences between the two program
    *         instances
    */
  def compare(p: ISPProgram, vm: VersionMap, removed: Set[SPNodeKey]): MergePlan = {
    def versionDiffers(k: SPNodeKey): Boolean =
      vm.get(k).forall(_ =/= p.getVersions(k))

    // locally present node differs from the remote version
    def presentDiffers(k: SPNodeKey): Boolean =
      versionDiffers(k) || removed.contains(k)

    // locally missing node differs from the remote version
    def missingDiffers(k: SPNodeKey): Boolean =
      versionDiffers(k) || !removed.contains(k)

    def nodeDiffers(n: ISPNode): Boolean = presentDiffers(n.key)

    // Present differences in in-use nodes rooted at r.
    def presentDiffs(r: ISPNode): Tree[MergeNode] =
      r match {
        case o: ISPObservation =>
          // Observations are atomic.  If anything differs at all in either
          // version copy the entire observation.
          if (o.exists(nodeDiffers)) modifiedTree(o) else unmodified(o).leaf

        case _                 =>
          val children = r.children.map(presentDiffs)

          // If there is even one descendant that differs, include this node
          // in the results.  Otherwise, only include it if it differs.
          if (nodeDiffers(r) || children.exists(_.rootLabel.isModified))
            Tree.node(modified(r), children.toStream)
          else
            unmodified(r).leaf
      }

    def missingDiffs(keys: Set[SPNodeKey]): Set[Missing] =
      keys.map(k => Missing(k, p.getVersions(k)))

    val pKeys       = p.getVersions.keySet
    val vmKeys      = vm.keySet

    // Any remote keys that we don't have locally are missing.
    val vmOnlyKeys  = vmKeys &~ pKeys

    // Any removed keys that either differ from the remote version or are not
    // deleted remotely.
    val deletedKeys = removedKeys(p).filter { missingDiffers }

    MergePlan(presentDiffs(p), missingDiffs(vmOnlyKeys ++ deletedKeys))
  }
}
