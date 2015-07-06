package edu.gemini.pot.sp.validator

import edu.gemini.pot.sp.{SPComponentType, SPNodeKey, ISPNode}

/** A constraint for the children of a conflict (which is no constraint at all). */
case object ConflictConstraint extends Constraint {
  val initial = ConflictConstraint

  val types: Types =
    (Types(Set())/:SPComponentType.values()) { (ts,ct) =>
      if (ct == SPComponentType.CONFLICT_FOLDER) ts
      else ts.addNarrow(ct)
    }

  def copy(ts: Types) = sys.error("Unsupported operation.")

  override def apply(n: NodeType[_ <: ISPNode], key:Option[SPNodeKey]) = Right(this)
}

