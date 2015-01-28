package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{ISPObservation, ISPNode, SPNodeKey}
import edu.gemini.pot.sp.version.NodeVersions
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.rich.pot.sp._

import scalaz._
import Scalaz._


/** MergeNodes form a tree with potential links into an existing science
  * program.  There are two types of MergeNode, [[Modified]] and
  * [[Unmodified]]s.  `Modified` describes a potential update to an
  * existing science program node (or the definition of a node missing locally).
  * `Unmodified` is just a `MergeNode` wrapper for an existing science program
  * node.
  */
sealed trait MergeNode {
  def key: SPNodeKey
  def isModified: Boolean
}


final case class Modified(key: SPNodeKey,
                          nv: NodeVersions,
                          dob: ISPDataObject,
                          detail: NodeDetail) extends MergeNode {
  def isModified = true
}

/** Contains any relevant details about the node that don't appear in its data
  * object or list of children.
  */
sealed trait NodeDetail

object NodeDetail {
  case object Empty extends NodeDetail
  final case class Obs(number: Int) extends NodeDetail

  def apply(n: ISPNode): NodeDetail = n match {
    case o: ISPObservation => Obs(o.getObservationNumber)
    case _                 => Empty
  }
}

final case class Unmodified(key: SPNodeKey) extends MergeNode {
  def isModified = false
}


object MergeNode {
  def unmodified(n: ISPNode): MergeNode =
    Unmodified(n.key)

  def modified(key: SPNodeKey, nv: NodeVersions, dob: ISPDataObject, detail: NodeDetail): MergeNode =
    Modified(key, nv, dob, detail)

  def modified(n: ISPNode): MergeNode =
    Modified(n.key, n.getVersion, n.getDataObject, NodeDetail(n))

  def modifiedTree(root: ISPNode): Tree[MergeNode] =
    Tree.node(modified(root), root.children.map(modifiedTree).toStream)

  implicit class TreeOps[A](t: Tree[A]) {
    /** A `foldRight` with strict evaluation of the `B` value of `f`. */
    def sFoldRight[B](z: => B)(f: (A, B) => B): B =
      t.foldRight(z) { (a, b) => f(a,b) }

    // TODO: what's the proper way to do this?
    /** A fold that provides access to the children. */
    def foldTree[B](z: B)(f: (Tree[A], B) => B): B = {
      def go(rem: List[Tree[A]], res: B): B =
        rem match {
          case Nil        => res
          case (t2 :: ts) => go(t2.subForest.toList ++ ts, f(t2, res))
        }

      go(List(t), z)
    }
  }

  implicit class MergeTreeOps(t: Tree[MergeNode]) {
    def key: SPNodeKey = t.rootLabel.key
  }

  implicit val ShowNode = Show.shows[MergeNode] {
    case Modified(k, _, dob, _) => s"m $k (${dob.getType})"
    case Unmodified(k)          => s"u $k"
  }

  def draw(t: Tree[MergeNode]): String =
    t.draw.zipWithIndex.collect { case (s0, n) if n % 2 == 0 => s0 }.mkString("\n")
}

