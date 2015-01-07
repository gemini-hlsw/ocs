package edu.gemini.sp.vcs

import edu.gemini.pot.sp.{SPNodeKey, ISPNode}
import edu.gemini.spModel.rich.pot.sp._

import scala.annotation.tailrec
import scalaz._

package object diff {

  // Java Integer seems to be missing from scalaz.Equal object but necessary
  // for use with NodeVersions (a VersionVector[LifespanId, Integer]).
  implicit def IntegerEqual: Equal[java.lang.Integer] = Equal.equalA

  implicit class IspNodeTreeOps(val node: ISPNode) extends AnyVal {
    def dfs[A](zero: A)(append: (A,  ISPNode) => A): A = {
      @tailrec def go(rem: List[ISPNode], res: A): A =
        rem match {
          case Nil     => res
          case n :: ns => go(n.children ++ rem, append(res, n))
        }

      go(List(node), zero)
    }

    /** A Map with entries for all nodes rooted at this node, keyed by
      * `SPNodeKey`.
      */
    def nodeMap: Map[SPNodeKey, ISPNode] =
      dfs(Map.empty[SPNodeKey, ISPNode]) { (m, n) => m + (n.getNodeKey -> n) }

    /** Set of all the `SPNodeKey` in the subtree of nodes rooted at this node.
      */
    def keySet: Set[SPNodeKey] =
      dfs(Set.empty[SPNodeKey]) { _ + _.getNodeKey }
  }
}
