package edu.gemini.phase2.template.factory.impl.ghost

import edu.gemini.spModel.gemini.ghost.blueprint.SpGhostBlueprintBase
import edu.gemini.phase2.template.factory.impl.{GroupInitializer, StaticObservationEditor}
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.spModel.target.env.AsterismType

trait GhostBase[B <: SpGhostBlueprintBase] extends GroupInitializer[B] {

  override val program: String =
    "GHOST PHASE I/II MAPPING BPS"

  implicit def GhostObsOps(obs: ISPObservation) = new {
    val ed = StaticObservationEditor[edu.gemini.spModel.gemini.ghost.Ghost](obs, instrumentType)

    def setAsterismType(a: AsterismType): Either[String, Unit] =
      ed.updateInstrument(_.setPreferredAsterismType(a))

  }

}
