package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{ISPNode, SPNodeKey}
import edu.gemini.pot.sp.version.NodeVersions
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.rich.pot.sp._

import scalaz._
import Scalaz._


// This is just a Diff.Present without the children.
final case class Update(key: SPNodeKey,
                        nv: NodeVersions,
                        dob: ISPDataObject,
                        detail: NodeDetail)


/** MergeNodes form a tree with potential links into an existing science
  * program.  There are two types of MergeNode, [[ModifiedNode]]s and
  * [[UnmodifiedNode]]s.  `ModifiedNode` describes a potential update to an
  * existing science program node (or the definition of a node missing locally).
  * They have children which can be another `ModifiedNode` or else an
  * `UnmodifiedNode` which is just a link to an existing science program
  * node.
  */
sealed trait MergeNode {
  def key: SPNodeKey
}


final case class ModifiedNode(u: Update, children: List[MergeNode]) extends MergeNode {
  def key: SPNodeKey = u.key
}

final case class UnmodifiedNode(n: ISPNode) extends MergeNode {
  def key: SPNodeKey = n.key
}


object MergeNode {

  def fold[A](z: A, root: MergeNode)(op: (A, MergeNode) => A): A = {
    def go(rem: List[MergeNode], res: A): A =
      rem match {
        case Nil     => res
        case n :: ns =>
          n match {
            case ModifiedNode(_, children) => go(children ++ ns, op(res, n))
            case _                         => go(ns,             op(res, n))
          }
      }
    go(List(root), z)
  }


  /** Zipper will be used for applying corrections to the merge tree.
    *
    * TODO: work in progress
    */

  object Zipper {
    final case class Crumb(parent: Update, prev: List[MergeNode], next: List[MergeNode])
  }

  import Zipper.Crumb

  final case class Zipper(focus: MergeNode, crumbs: List[Zipper.Crumb] = Nil) {
    def up: Option[Zipper] = crumbs match {
      case Crumb(parent, prev, next) :: cs =>
        some(Zipper(ModifiedNode(parent, (focus :: prev).reverse ++ next), cs))
      case _ =>
        none
    }

    def top: Zipper = up.map(_.top) | this

    def down: Option[Zipper] = focus match {
      case ModifiedNode(u, children) =>
        children match {
          case n :: ns => some(Zipper(n, Crumb(u, Nil, ns) :: crumbs))
          case _       => none
        }
      case _ =>
        none
    }

    def nextSibling: Option[Zipper] = crumbs match {
      case Crumb(parent, prev, n :: ns) :: cs =>
        some(Zipper(n, Crumb(parent, focus :: prev, ns) :: cs))
      case _ =>
        none
    }

    def prevSibling: Option[Zipper] = crumbs match {
      case Crumb(parent, n :: ns, next) :: cs =>
        some(Zipper(n, Crumb(parent, ns, focus :: next) :: cs))
      case _ =>
        none
    }

    def find(p: MergeNode => Boolean): Option[Zipper] =
      if (p(focus))
        some(this)
      else
        down.flatMap(_.find(p)) orElse nextSibling.flatMap(_.find(p))
  }
}