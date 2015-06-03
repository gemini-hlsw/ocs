package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.ISPNode
import edu.gemini.spModel.rich.pot.sp._

import scalaz.Tree

/** A division of the children of a matching local `ISPNode` and a remote
  * `Tree[MergeNode]` according to whether the child only appears in the local
  * node, only appears in the remote note, or appears in both. */
private[vcs2] final case class PartitionedChildren(
  local: List[ISPNode],
  both: List[(ISPNode, Tree[MergeNode])],
  remote: List[Tree[MergeNode]])

private[vcs2] object PartitionedChildren {
  def part(lc: List[ISPNode], rc: List[Tree[MergeNode]]): PartitionedChildren = {
    val localKeys = lc.map(_.key).toSet
    val rChildMap = rc.map(c => c.key -> c).toMap
    val bothKeys  = localKeys & rChildMap.keySet

    val (both0, local) = lc.partition(c => bothKeys.contains(c.key))
    val both           = both0.map(c => (c, rChildMap(c.key)))
    val remote         = rc.filter { c => !bothKeys.contains(c.key) }

    PartitionedChildren(local, both, remote.toList)
  }
}
