package edu.gemini.pot.sp.validator

import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.pot.sp.SPComponentBroadType._
import edu.gemini.pot.sp._


object TemplateGroupConstraint {
  val initial = new TemplateGroupConstraint(Types.initial
    .addNarrow[ISPObservation](OBSERVATION_BASIC)
    .addNarrow[ISPTemplateParameters](TEMPLATE_PARAMETERS)
    .addBroad[ISPObsComponent](INFO))
}

/** Constraint for the children of a template group. */
case class TemplateGroupConstraint private(val types: Types) extends Constraint {
  def copy(ts: Types) = new TemplateGroupConstraint(ts)
}

