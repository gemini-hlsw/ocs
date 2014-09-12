package edu.gemini.spModel.util

import edu.gemini.pot.sp.{ISPNode, SPNodeKey}
import edu.gemini.pot.sp.version.nodeChecksum

object NodeValueCache {
  type CacheMap[A] = Map[SPNodeKey, (Long, A)]
  def empty[A]: NodeValueCache[A] = NodeValueCache(Map.empty[SPNodeKey, (Long, A)])
}

import NodeValueCache._

case class NodeValueCache[A](m: CacheMap[A]) {
  def get(n: ISPNode): (Option[A], NodeValueCache[A]) =
    m.get(n.getNodeKey).fold((Option.empty[A], this)) { case (check, value) =>
      val curCheck = nodeChecksum(n)
      if (curCheck == check) (Some(value), this)  // hit
      else (None, copy(m = m - n.getNodeKey))     // miss, node has been edited
    }

  def put(n: ISPNode)(a: ISPNode => A): (A, NodeValueCache[A]) = {
    val check = nodeChecksum(n)
    val aVal  = a(n)
    (aVal, copy(m = m.updated(n.getNodeKey, (check, aVal))))
  }
}
