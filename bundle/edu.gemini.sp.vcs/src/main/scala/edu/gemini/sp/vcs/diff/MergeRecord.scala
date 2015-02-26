package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{ISPNode, DataObjectBlob, SPNodeKey}
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.data.ISPDataObject

import scalaz._
import Scalaz._

/** A base trait for a description of the various changes that may be applied
  * to a local program node.
  */
sealed trait MergeRecord {
  def key: SPNodeKey
}

object MergeRecord {
  case class Create(key: SPNodeKey)                                extends MergeRecord
  case class Resurrect(key: SPNodeKey)                             extends MergeRecord

  case class AddChild(key: SPNodeKey, child: SPNodeKey)            extends MergeRecord
  case class RemoveChild(key: SPNodeKey, child: SPNodeKey)         extends MergeRecord
  case class ReorderChildren(key: SPNodeKey, was: List[SPNodeKey]) extends MergeRecord

  case class UpdateDataObject(key: SPNodeKey, was: ISPDataObject)  extends MergeRecord


  // TODO: currently this examines the root node only

  /** Compares the given tree node to the corresponding local program
    * node, if any, and returns the list of changes that were applied.
    *
    * @return changes to the given local node applied by the merge
    */
  def changes(mc: MergeContext, t: Tree[MergeNode]): List[MergeRecord] = {
    val k = t.rootLabel.key
    val newChildren = t.subForest.map(_.rootLabel.key).toList

    def created: List[MergeRecord] = {
      val mr = if (mc.local.vm.contains(k)) Resurrect(k) else Create(k)
      mr :: newChildren.map(c => AddChild(k, c))
    }

    def updated(n: ISPNode, m: Modified): List[MergeRecord] = {
      val curDob      = n.getDataObject
      val newDob      = m.dob
      val updateDob   = !DataObjectBlob.same(curDob, newDob) option UpdateDataObject(k, curDob)

      val curChildren = n.children.map(_.key)
      val added       = newChildren.diff(curChildren)
      val removed     = curChildren.diff(newChildren)

      // ignoring what has been added and removed, have we reordered?
      val rKeys       = removed.toSet
      val lst0        = curChildren.filterNot(rKeys.contains)
      val aKeys       = added.toSet
      val lst1        = newChildren.filterNot(aKeys.contains)
      val reorder     = (lst0 =/= lst1) option ReorderChildren(k, curChildren)

      updateDob.toList ++ reorder.toList ++ added.map(AddChild(k, _)) ++ removed.map(RemoveChild(k, _))
    }

    t.rootLabel match {
      case m: Modified   => mc.local.get(k).fold(created)(n => updated(n, m))
      case _: Unmodified => Nil
    }
  }
}
