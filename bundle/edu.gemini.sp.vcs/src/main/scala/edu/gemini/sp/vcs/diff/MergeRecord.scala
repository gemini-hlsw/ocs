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


  /** Compares the given [[ModifiedNode]] to the corresponding local program node,
    * if any, and returns the list of changes that were applied.
    *
    * @return changes to the given local node applied by the merge
    */
  def changes(mc: MergeContext, mn: ModifiedNode): List[MergeRecord] = {
    val k = mn.key

    def created: List[MergeRecord] = {
      val mr = if (mc.local.vm.contains(k)) Resurrect(k) else Create(k)
      mr :: mn.children.map(c => AddChild(k, c.key))
    }

    def updated(n: ISPNode): List[MergeRecord] = {
      val dob        = n.getDataObject
      val updateDob  = !DataObjectBlob.same(mn.u.dob, dob) option UpdateDataObject(k, dob)

      val children   = n.children.map(_.key)
      val mnChildren = mn.children.map(_.key)
      val added      = mnChildren.diff(children)
      val removed    = children.diff(mnChildren)

      // ignoring what has been added and removed, have we reordered?
      val rKeys      = removed.toSet
      val lst0       = children.filterNot(rKeys.contains)
      val aKeys      = added.toSet
      val lst1       = mnChildren.filterNot(aKeys.contains)
      val reorder    = (lst0 =/= lst1) option ReorderChildren(k, children)

      updateDob.toList ++ reorder.toList ++ added.map(AddChild(k, _)) ++ removed.map(RemoveChild(k, _))
    }

    mc.local.get(k).fold(created)(updated)
  }
}
