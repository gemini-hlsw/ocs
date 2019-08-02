package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.immutable.IgrinsBlueprint

object Igrins {
  // Needs a Node to be returned from apply :-/.
  // We're supposed to have a node which then progresses to another node or returns the final blueprint.

  def apply() = Right(IgrinsBlueprint)
}