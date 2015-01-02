package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.version.NodeVersions
import edu.gemini.sp.vcs.diff
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.rich.pot.sp._

import scala.annotation.tailrec


/** Identifies a program node, its version information and a (potential)
  * difference between a local program node and the remote version to which it
  * is compared.
  *
  * The `diff` field contains enough information to merge the node with its
  * counterpart in the remote VM if necessary.  We cannot send `ISPNode`
  * descendants directly because they contain links to the enclosing program
  * that would be followed when serializing.  In other words, including any
  * `ISPNode` would mean including the entire program tree.
  *
  * A client initiates an update to its version of the program by sending the
  * remote peer version information.  Using the version information, the peer
  * constructs a sequence of `DiffNode`s that may potentially be necessary for
  * performing the merge. Not all `DiffNode`s represent actual differences.
  * Some are created when there isn't enough information to determine whether
  * they will be ultimately required.
  */
case class DiffNode(key: SPNodeKey, nv: NodeVersions, diff: Diff)


object DiffNode {

  /** Creates an [[diff.Diff.InUse]] `DiffNode` from the
    * provided program node.
    */
  def apply(n: ISPNode): DiffNode =
    DiffNode(n.getNodeKey, n.getVersion, Diff(n))

  /** Creates [[diff.Diff.InUse]] `DiffNode`s for each
    * program node rooted at `root`.
    */
  def tree(root: ISPNode): List[DiffNode] = {
    @tailrec def go(nodes: List[ISPNode], results: List[DiffNode]): List[DiffNode] =
      nodes match {
        case Nil     => results
        case n :: ns => go(n.children ++ ns, DiffNode(n) :: results)
      }

    go(List(root), List.empty)
  }
}

sealed trait Diff

object Diff {

  /** Describes the content of a node that potentially differs and is still in
    * use in the local program.
    */
  case class  InUse(dob: ISPDataObject, children: List[SPNodeKey], detail: NodeDetail) extends Diff

  /** Marks a node that has never existed in the local program. */
  case object Missing extends Diff

  /** Marks a node that no longer exists in the local program and has different
    * version information than the remote program.
    */
  case object Removed extends Diff

  def apply(n: ISPNode): Diff = {
    val detail = n match {
      case o: ISPObservation => NodeDetail.Obs(o.getObservationNumber)
      case _                 => NodeDetail.Empty
    }

    InUse(n.getDataObject, n.children.map(_.getNodeKey), detail)
  }
}

/** Contains any relevant details about the node that don't appear in its data
  * object or list of children.
  */
sealed trait NodeDetail

object NodeDetail {
  case object Empty extends NodeDetail
  case class Obs(number: Int)  extends NodeDetail
}