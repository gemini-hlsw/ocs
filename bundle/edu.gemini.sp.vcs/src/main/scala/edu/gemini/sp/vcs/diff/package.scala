package edu.gemini.sp.vcs

import edu.gemini.pot.sp.{ISPProgram, SPNodeKey, ISPNode}
import edu.gemini.spModel.rich.pot.sp._

import scalaz.\/

package object diff {

  /**
   * A `MergeCorrection` is just a function that modifies an `MergePlan` to
   * correct some aspect of the merge.
   */
  type MergeCorrection = MergePlan => Unmergeable \/ MergePlan

  implicit class IspNodeTreeOps(val node: ISPNode) extends AnyVal {
    /** A Map with entries for all nodes rooted at this node, keyed by
      * `SPNodeKey`.
      */
    def nodeMap: Map[SPNodeKey, ISPNode] =
      node.fold(Map.empty[SPNodeKey, ISPNode]) { (m, n) => m + (n.key -> n) }

    /** Set of all the `SPNodeKey` in the subtree of nodes rooted at this node.
      */
    def keySet: Set[SPNodeKey] =
      node.fold(Set.empty[SPNodeKey]) { _ + _.key }
  }

  /** Returns the set of `SPNodeKey` for all nodes no longer in program `p`.
    *
    * Note, requires a complete tree traversal so this is somewhat expensive.
    */
  def removedKeys(p: ISPProgram): Set[SPNodeKey] =
    p.fold(p.getVersions.keySet) { _ - _.key }
}
