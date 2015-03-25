package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{ISPObservation, ISPNode, SPNodeKey}
import edu.gemini.pot.sp.version.{EmptyVersionMap, VersionMap, NodeVersions}
import edu.gemini.shared.util.VersionComparison
import edu.gemini.sp.vcs.diff.NodeDetail.Obs
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.rich.pot.sp._

import scalaz._


/** MergeNodes form a tree with potential links into an existing science
  * program.  There are two types of MergeNode, [[edu.gemini.sp.vcs.diff.Modified]]
  * and [[edu.gemini.sp.vcs.diff.Unmodified]]s.  `Modified` describes a
  * potential update to an existing science program node (or the definition of a
  * node missing locally). `Unmodified` is just a `MergeNode` wrapper for an
  * existing science program node.
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

  implicit class TreeLocOps[A](tl: TreeLoc[A]) {
    /** Deletes the current node and selects the parent.  Unlike the
      * `TreeLoc.delete` function, this function will always select the parent
      * node. */
    def deleteNodeFocusParent: Option[TreeLoc[A]] = {
      def combine(ls: Stream[Tree[A]], rs: Stream[Tree[A]]) =
        ls.foldLeft(rs)((a, b) => b #:: a)

      tl.parents match {
        case (pls, v, prs) #:: ps => Some(TreeLoc.loc(Tree.node(v, combine(tl.lefts, tl.rights)), pls, prs, ps))
        case Stream.Empty         => None
      }
    }
  }

  implicit class MergeTreeOps(t: Tree[MergeNode]) {
    def key: SPNodeKey = t.rootLabel.key

    /** A fold over observations contained in this tree node (if any).
      * Doesn't descend into the observation itself.
      */
    def foldObservations[B](z: B)(f: (Modified, Int, Stream[Tree[MergeNode]], B) => B): B = {
      def go(rem: List[Tree[MergeNode]], res: B): B =
        rem match {
          case Nil        => res
          case (t2 :: ts) => t2.rootLabel match {
            case m@Modified(_, _, _, Obs(n)) => go(ts, f(m, n, t2.subForest, res))
            case _                           => go(t2.subForest.toList ++ ts, res)
          }
        }

      go(List(t), z)
    }

    def vm: VersionMap = {
      def go(rem: Stream[Tree[MergeNode]], res: VersionMap): VersionMap =
        if (rem.isEmpty) res
        else {
          val t   = rem.head
          val vm0 = t.rootLabel match {
            case Modified(k, nv, _, _) => res + (k -> nv)
            case _                     => res
          }
          go(t.subForest ++ rem.tail, vm0)
        }

      go(Stream(t), EmptyVersionMap)
    }

    def compare(that: Tree[MergeNode]): VersionComparison = VersionMap.compare(vm, that.vm)
  }

  implicit val ShowNode = Show.shows[MergeNode] {
    case Modified(k, _, dob, _) => s"m $k (${dob.getType})"
    case Unmodified(k)          => s"u $k"
  }

  def draw(t: Tree[MergeNode]): String =
    t.draw.zipWithIndex.collect { case (s0, n) if n % 2 == 0 => s0 }.mkString("\n")
}

