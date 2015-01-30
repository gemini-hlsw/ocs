package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{ISPNode, SPNodeKey}
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
}


// This is just a Diff.Present without the children.
final case class Modified(key: SPNodeKey,
                          nv: NodeVersions,
                          dob: ISPDataObject,
                          detail: NodeDetail) extends MergeNode


final case class Unmodified(n: ISPNode) extends MergeNode {
  def key: SPNodeKey = n.key
}

object MergeNode {
  def unmodified(n: ISPNode): MergeNode =
    Unmodified(n)

  def modified(key: SPNodeKey, nv: NodeVersions, dob: ISPDataObject, detail: NodeDetail): MergeNode =
    Modified(key, nv, dob, detail)

  implicit class TreeOps[A](t: Tree[A]) {
    /** A `foldRight` with strict evaluation of the `B` value of `f`. */
    def sFoldRight[B](z: => B)(f: (A, B) => B): B =
      t.foldRight(z) { (a, b) => f(a,b) }
  }
}