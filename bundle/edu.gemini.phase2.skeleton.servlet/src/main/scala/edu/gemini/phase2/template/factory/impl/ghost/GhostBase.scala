package edu.gemini.phase2.template.factory.impl.ghost

import edu.gemini.spModel.gemini.ghost.blueprint.SpGhostBlueprintBase
import edu.gemini.phase2.template.factory.impl.GroupInitializer

trait GhostBase[B <: SpGhostBlueprintBase] extends GroupInitializer[B] {

  override val program: String =
    "GHOST PHASE I/II MAPPING BPS"



}
