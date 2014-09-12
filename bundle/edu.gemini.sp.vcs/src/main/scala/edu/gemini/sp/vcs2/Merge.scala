package edu.gemini.sp.vcs2

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.version.VersionMap
import edu.gemini.shared.util.VersionVector

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scalaz._
import Scalaz._
import scalaz.std.vector._

object Merge {
  type MergeOut = Writer[Vector[SPNodeKey], ISPProgram]

//  def noOp(p: ISPProgram): MergeOut =
//    p.set(Vector.empty[SPNodeKey]).right[VcsFailure]

  def nodeMap(root: ISPNode): Map[SPNodeKey, ISPNode] = {
    def children(n: ISPNode): Vector[ISPNode] =
      n match {
        case c: ISPContainerNode => c.getChildren.asScala.toVector
        case _                   => Vector.empty[ISPNode]
      }

    @tailrec
    def go(ns: Vector[ISPNode], m: Map[SPNodeKey, ISPNode]): Map[SPNodeKey, ISPNode] =
      if (ns.isEmpty) m
      else go(children(ns.head) ++ ns.tail, m + (ns.head.getNodeKey -> ns.head))

    go(Vector(root), Map.empty)
  }
}


// (1) Go through the diffs creating nodes that are new remotely, leaving them
// without children yet.
// (2) Now go through the diffs again, moving children around in the program.
// Here there are two cases.  First if the remote node is new or strictly newer
// than the local one, just use its child list.  Otherwise, the existing node
// has also been updated and we have to merge the child lists.  :-(
//
// MERGING THE CHILD LISTS:
// - Anything new in the local program is kept.
// - Anything new in the remote program is kept.
// - Anything in both child lists is kept.
// - If moved locally, pull it back so that the remote version wins.
// - If removed locally, ...


// Issues:
// - how to handle "deleted but modified" locally and remotely
// -


import Merge._

class Merge(p: ISPProgram, f: ISPFactory, rVm: VersionMap, diff: Map[SPNodeKey, Option[NodeUpdate]]) {
  /*
  private val lVm    = p.getVersions
  private val lNodes = nodeMap(p)

  private lazy val newRemoteNodes: String \/ Map[SPNodeKey, ISPNode] =
    (rVm.keySet -- lVm.keySet).map { k =>
      for {
        nuO <- diff.get(k).toRightDisjunction(s"Missing diff for node $k")
        nO  <- nuO.fold(\/-(none[ISPNode])) { nu => nu.createNode(p, f).toRightDisjunction(s"Missing definition for creating a node from a ${nu.dataObject.getType}") }
      } yield nO
    }.sequenceU

  private def go(n: ISPNode, ins: Vector[MergeInstruction]): Vector[MergeInstruction] = {
    val key        = n.getNodeKey
    val lVersions  = lVm(key)
    val rVersionsO = rVm.get(key)

    // Cases:
    // - No remote version info.  This is a new local node.  There is nothing
    //   to do in this case.
    // -

    rVersionsO.fold(ins) { rVersions =>
      lVersions.tryCompareTo(rVersions) match {
        case Some(i) if i >= 0 => ins
        case Some(_)           =>
        case _                 =>
      }
    }
  }

  def apply(): MergeOut =
    if (diff.isEmpty) noOp(p)
    else {

    }
    */
}
