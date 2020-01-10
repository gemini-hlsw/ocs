package edu.gemini.pot.sp.validator

import edu.gemini.pot.sp.{ISPConflictFolder, SPComponentType, SPComponentBroadType, ISPNode}
import edu.gemini.pot.sp.SPComponentType._


object Types {
  def initial = new Types(Set()).addNarrow[ISPConflictFolder](CONFLICT_FOLDER)
}

/** A set of NodeTypes. */
case class Types(nodeTypes: Set[NodeType[_]]) {

  def matches(t: NodeType[_]) = nodeTypes.contains(t)

  def -(b: SPComponentBroadType): Types = new Types(nodeTypes.filterNot(_.ct.broadType == b))

  def -(b: SPComponentType): Types = new Types(nodeTypes.filterNot(_.ct == b))

  def +(b: SPComponentType): Types = new Types(nodeTypes + NodeType(b))

  def addNarrow[N <: ISPNode : Manifest](cts: SPComponentType *): Types =
    (this /: cts)((ts, ct) => new Types(ts.nodeTypes + NodeType(ct)))

  def addBroad[N <: ISPNode : Manifest](bts: SPComponentBroadType*): Types =
    (this /: SPComponentType.values.filter(t => bts.contains(t.broadType)))(_ addNarrow _)

  def retainOnly(bt:SPComponentBroadType, nts:SPComponentType*) =
    new Types(nodeTypes.filterNot(t => t.ct.broadType == bt && !nts.contains(t.ct)))

}
