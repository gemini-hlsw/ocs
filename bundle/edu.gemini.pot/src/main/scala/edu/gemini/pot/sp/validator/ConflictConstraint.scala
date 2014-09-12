package edu.gemini.pot.sp.validator

import edu.gemini.pot.sp.{SPNodeKey, ISPNode}

/** A constraint for the children of a conflict (which is no constraint at all). */
case object ConflictConstraint extends Constraint {
  val initial = ConflictConstraint
  def types:Types = sys.error("Unsupported operation.")
  def copy(ts: Types) = sys.error("Unsupported operation.")
  override def apply(n: NodeType[_ <: ISPNode], key:Option[SPNodeKey]) = {

//    println("Validating child %s/%s (unconstrainted)".format(
//      n.mf.getSimpleName,
//      n.ct))

    Right(this)
  }
}

