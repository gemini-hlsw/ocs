package edu.gemini.pot.sp.validator

import edu.gemini.pot.sp._

/** A pair of shell type, component type. */
case class NodeType[N <: ISPNode : Manifest](ct: SPComponentType) {
  def mf = implicitly[Manifest[N]].runtimeClass
  def matches(n: ISPNode) = mf.isInstance(n) && n.getDataObject.getType == ct
  override def toString = mf.getSimpleName + "/" + ct

  def cardinalityOf(childType: NodeType[_ <: ISPNode]): NodeCardinality =
    Constraint.forType(this).map(_.cardinality(childType)).getOrElse(NodeCardinality.Zero)
}

object NodeType {
  def apply[A <: ISPNode : Manifest](n:A):NodeType[A] = NodeType[A](n.getDataObject.getType)

  def forNode(n: ISPNode): NodeType[_ <: ISPNode] =
    // This match expression peels off the proper manifest, so although it looks like
    // it doesn't do anything, it's important.
    n match {
         case n: ISPGroup              => NodeType(n)
         case n: ISPObsComponent       => NodeType(n)
         case n: ISPObservation        => NodeType(n)
         case n: ISPProgram            => NodeType(n)
         case n: ISPSeqComponent       => NodeType(n)
         case n: ISPTemplateFolder     => NodeType(n)
         case n: ISPTemplateGroup      => NodeType(n)
         case n: ISPTemplateParameters => NodeType(n)
         case n: ISPConflictFolder     => NodeType(n)
         case n: ISPObsQaLog           => NodeType(n)
         case n: ISPObsExecLog         => NodeType(n)
       }

}
