package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.ISPNode
import edu.gemini.pot.sp.version.NodeVersions
import edu.gemini.spModel.util.ReadableNodeName
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.data.ISPDataObject

sealed trait MergeInstruction {
  def apply(): Unit
  def msg: String
}

object MergeInstruction {
  case class SetDataObject(vv: NodeVersions, n: ISPNode, dataObj: ISPDataObject) extends MergeInstruction {
    def apply(): Unit = n.setDataObject(dataObj)

    def msg: String = s"Updated ${ReadableNodeName.format(n)}"
  }

  case class SetChildren(vv: NodeVersions, n: ISPNode, newChildren: Seq[ISPNode]) extends MergeInstruction {
    def apply(): Unit = n.children = newChildren.toList

    def msg: String = s"Set children of ${ReadableNodeName.format(n)}"
  }
}
