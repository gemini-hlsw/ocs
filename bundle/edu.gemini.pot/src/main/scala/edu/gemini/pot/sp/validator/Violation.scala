package edu.gemini.pot.sp.validator

import edu.gemini.pot.sp.{SPNodeKey, ISPNode}

sealed trait Violation

case class DuplicateKeyViolation(key: SPNodeKey) extends Violation {
  override def toString = "Duplicate key: %s".format(key)
}

case class CardinalityViolation(nt: NodeType[_ <: ISPNode], key: Option[SPNodeKey], c: Constraint) extends Violation {
  override def toString =
    "At node %s\n of type %s/%s\n  I expected to find one of:\n   %s".format(
      key,
      nt.mf.getSimpleName,
      nt.ct,
      c.types.nodeTypes.mkString("\n   "))
}
