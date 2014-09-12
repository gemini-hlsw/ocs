package edu.gemini.pot.sp.validator

import edu.gemini.pot.sp.SPComponentType._
import edu.gemini.pot.sp.{ISPObservation, ISPTemplateGroup, ISPContainerNode}

object TemplateFolderConstraint {
  val initial = new TemplateFolderConstraint(Types.initial
    .addNarrow[ISPTemplateGroup](TEMPLATE_GROUP))
}

/** Constraint for the children of a template folder. */
class TemplateFolderConstraint private (val types:Types) extends Constraint {
  def copy(ts: Types) = new TemplateFolderConstraint(ts)
}

