package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.version._
import edu.gemini.pot.sp.{ISPNode, ISPObservation, ISPProgram, SPNodeKey}
import edu.gemini.sp.vcs.diff.Diff.{missing, present}
import edu.gemini.spModel.rich.pot.sp._

import scalaz._
import Scalaz._

object ProgramDiff {

  /** Returns a (super-set) of differences between two programs.
    *
    * It is a "super-set" because ultimately not every returned `Diff` may be
    * necessary for merging program versions.
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
    * copy of the program. It generates `Diff` objects to describe the
    * differences and allow the remote program to incorporate any changes that
    * have been made locally.
    *
    * There are two types of `Diff` objects, `Present` and `Missing`.  A
    * `Present` diff refers to a node that exists in the local program while
    * `Missing` diffs are no longer reachable in the local program.
    *
    * The comparison must generate
    *
    * - `Missing` diffs for any node not in the local program that either (a)
    * is still in the remote program or (b) has different version information
    * than the remote program.
    *
    * - `Present` diffs for any node in the local program with different
    * version information than its remote counterpart.
    *
    * - `Present` diffs for all nodes in an observation that have at least one
    * contained node with a difference in version data.  In other words, any
    * difference in an observation will bring `Present` diffs for the entire
    * observation.
    *
    * - `Present` diffs for all ancestor nodes of a node with different version
    * data.  For example, an edited note in a group will pull in the group and
    * the program ancestors.
    *
    * Conversely, the comparison must not
    *
    * - include `Present` diffs for any node which has the same version
    * information as the remote node and which has no descendants with different
    * version data
    *
    * - include `Missing` diffs for nodes deleted both locally and remotely and
    * that have the same version data
    *
    * @param local local program from which differences are to be extracted
    *
    * @param remoteVm `VersionMap` of the remote instance of the program
    *
    * @param remoteRemoved `Set` of all node keys that are not present in the
    *                     remote instance of the program
    *
    * @return super-set of `Diff`s between the local and remote program
    *         instances
    */
  def compare(local: ISPProgram, remoteVm: VersionMap, remoteRemoved: Set[SPNodeKey]): List[Diff] = {
    def versionDiffers(k: SPNodeKey): Boolean =
      remoteVm.get(k).forall(_ =/= local.getVersions(k))

    // locally present node differs from the remote version
    def presentDiffers(k: SPNodeKey): Boolean =
      versionDiffers(k) || remoteRemoved.contains(k)

    // locally missing node differs from the remote version
    def missingDiffers(k: SPNodeKey): Boolean =
      versionDiffers(k) || !remoteRemoved.contains(k)

    def nodeDiffers(n: ISPNode): Boolean = presentDiffers(n.key)

    def diffNode(n: ISPNode): Option[Diff] = nodeDiffers(n) option present(n)

    // Present differences in in-use nodes rooted at r.
    def presentDiffs(r: ISPNode): List[Diff] =
      r match {
        case o: ISPObservation =>
          // Observations are atomic.  If anything differs at all in either
          // version copy the entire observation.
          if (o.exists(nodeDiffers)) Diff.tree(o) else List.empty

        case _                 =>
          // If there is even one descendant that differs, include this node
          // in the results.  Otherwise, only include it if it differs.
          r.children.flatMap(presentDiffs) match {
            case Nil => diffNode(r).toList
            case cds => present(r) :: cds
          }
      }

    import scala.collection.breakOut
    def missingDiffs(keys: Set[SPNodeKey]): List[Diff] =
      keys.map(k => missing(k, local.getVersions(k)))(breakOut)

    val localKeys       = local.getVersions.keySet
    val remoteKeys      = remoteVm.keySet

    // Any remote keys that we don't have locally are missing.
    val remoteOnlyKeys  = remoteKeys &~ localKeys

    // Any removed keys that either differ from the remote version or are not
    // deleted remotely.
    val deletedKeys     = removedKeys(local).filter { missingDiffers }

    missingDiffs(remoteOnlyKeys ++ deletedKeys) ++ presentDiffs(local)
  }
}
