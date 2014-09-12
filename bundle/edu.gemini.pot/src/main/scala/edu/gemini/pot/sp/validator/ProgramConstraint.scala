package edu.gemini.pot.sp.validator

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.pot.sp.SPComponentBroadType._

object ProgramConstraint {
  val initial = new ProgramConstraint(Types.initial
    .addNarrow[ISPObservation](OBSERVATION_BASIC)
    .addNarrow[ISPGroup](GROUP_GROUP)
    .addNarrow[ISPTemplateFolder](TEMPLATE_FOLDER)
    .addBroad[ISPObsComponent](INFO))
}

/** A constraint on the children of ISPPrograms. */
class ProgramConstraint private(val types: Types) extends Constraint {
  override def uniqueNarrowTypes = super.uniqueNarrowTypes ++ Set(TEMPLATE_FOLDER)

  // There can be only one
  def copy(ts: Types) = new ProgramConstraint(ts)
}

