package edu.gemini.pot.sp.validator

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.pot.sp.SPComponentBroadType._

object GroupConstraint {
  val initial = new GroupConstraint(Types.initial
    .addNarrow[ISPObservation](OBSERVATION_BASIC)
    .addBroad[ISPObsComponent](INFO))
}

/** A constraint for the children of a Group. */
case class GroupConstraint private(val types: Types) extends Constraint {
  def copy(ts: Types) = new GroupConstraint(ts)
}

