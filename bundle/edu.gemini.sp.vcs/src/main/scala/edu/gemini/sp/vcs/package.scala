package edu.gemini.sp

import edu.gemini.pot.sp.{ISPNode, Conflicts, Conflict, SPNodeKey}
import edu.gemini.shared.util.immutable.{DefaultImList, ImList}
import edu.gemini.spModel.rich.pot.sp._

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

package object vcs {
  type NodeMap = Map[SPNodeKey, ISPNode]
  val EmptyNodeMap: NodeMap = Map.empty

  def nodeMap(sp: ISPNode): NodeMap = {
    def nodeMap(m: NodeMap, sp: ISPNode): NodeMap =
      ((m + (sp.getNodeKey -> sp))/:sp.children) { nodeMap }

    nodeMap(EmptyNodeMap, sp)
  }

  // Inserts "child" into "intoList" using the "positionList" to figure out
  // the most likely desired location.  It will try to put the "child" after
  // the nearest left sibling in "positionList" that is common across "intoList"
  // and "positionList".  Roughly speaking, the idea is to figure out where the
  // child was in "positionList" and insert it in the same place in "intoList".
  private[sp] def orderedInsert[T](intoList: List[T], child: T, positionList: List[T])(getKey: T => SPNodeKey): List[T] = {
    val intoKeys = intoList.map(getKey).toSet
    val after    = positionList.takeWhile(t => getKey(t) != getKey(child)).filter(n => intoKeys.contains(getKey(n))).lastOption

    def insert(rem: List[T], afterKey: SPNodeKey): List[T] = rem match {
      case Nil    => List(child)
      case h :: t => if (getKey(h) == afterKey) h :: child :: t
                     else h :: insert(t, afterKey)
    }

    after.cata(a => insert(intoList, getKey(a)), child :: intoList)
  }

  def toImList[T](lst: List[T]): ImList[T] = DefaultImList.create(lst.asJava)

  def conflicts(notes: List[Conflict.Note]*): Conflicts =
    Conflicts.apply(toImList(notes.flatten.toList))
}

package vcs {

  sealed trait OdbPerspective
  case object LocalOdb extends OdbPerspective
  case object RemoteOdb extends OdbPerspective

}