package edu.gemini.spModel.util

import edu.gemini.pot.sp.{ISPNode, SPNodeKey}
import edu.gemini.pot.sp.version.nodeChecksum

/** Provides fast lookup of values that depend upon the state of `ISPNode`s,
  * avoiding recalculation unless the subtree rooted at the node has changed
  * since the last time the value was calculated. */
sealed trait NodeValueCache[A] {

  /** Get the value associated with the node, returning the previously cached
    * value if available and if the node has not been modified.  Otherwise
    * calculates the value with the provided function and caches it for the
    * next lookup. */
  def get(n: ISPNode)(a: ISPNode => A): (A, NodeValueCache[A])
}

object NodeValueCache {

  def empty[A]: NodeValueCache[A] = Impl[A](Map.empty[SPNodeKey, (Long, A)])

  private final case class Impl[A](m: Map[SPNodeKey, (Long, A)]) extends NodeValueCache[A] {
    def get(n: ISPNode)(a: ISPNode => A): (A, NodeValueCache[A]) = {
      val k  = n.getNodeKey
      val cs = nodeChecksum(n)

      def add: (A, NodeValueCache[A]) = {
        val aVal = a(n)
        (aVal, Impl(m.updated(k, (cs, aVal))))
      }

      m.get(k).fold(add) { case (check, aVal) =>
        if (cs == check) (aVal, this) else add
      }
    }
  }
}