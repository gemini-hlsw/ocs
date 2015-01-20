package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.version.NodeVersions
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.rich.pot.sp._

import scala.annotation.tailrec

/** Identifies a program node, its version information and a (potential)
  * difference between a local program node and the remote version to which it
  * is compared.
  */
sealed trait Diff {
  def key: SPNodeKey
  def nv: NodeVersions
}

object Diff {

  /** Marks a node that either never existed in the program or else has been
    * removed and had different version information.
    */
  final case class Missing(key: SPNodeKey, nv: NodeVersions) extends Diff

  /** Describes the content of a node that potentially differs and is still in
    * use in the local program.  Contains enough information to merge the node
    * with its counterpart in the remote VM if necessary.  We cannot send
    * `ISPNode` descendants directly because they contain links to the enclosing
    * program that would be followed when serializing.  In other words,
    * including any `ISPNode` would mean including the entire program.
    */
  final case class Present(key:      SPNodeKey,
                           nv:       NodeVersions,
                           dob:      ISPDataObject,
                           children: List[SPNodeKey],
                           detail:   NodeDetail      ) extends Diff

  /** Creates an [[Present]] `Diff` from the provided program node. */
  def present(n: ISPNode): Present = {
    val detail = n match {
      case o: ISPObservation => NodeDetail.Obs(o.getObservationNumber)
      case _                 => NodeDetail.Empty
    }
    Present(n.key, n.getVersion, n.getDataObject, n.children.map(_.key), detail)
  }

  /** Creates [[Present]] `Diff`s for each program node rooted at `root`. */
  def tree(root: ISPNode): List[Diff] = {
    @tailrec def go(nodes: List[ISPNode], results: List[Diff]): List[Diff] =
      nodes match {
        case Nil       => results
        case (n :: ns) => go(n.children ++ ns, present(n) :: results)
      }

    go(List(root), List.empty)
  }
}

/** Contains any relevant details about the node that don't appear in its data
  * object or list of children.
  */
sealed trait NodeDetail

object NodeDetail {
  case object Empty extends NodeDetail
  final case class Obs(number: Int) extends NodeDetail
}
