package edu.gemini.phase2.template.factory.impl.michelle

import edu.gemini.pot.sp.ISPObservation
import edu.gemini.spModel.gemini.michelle.{MichelleParams, SeqConfigMichelle, InstMichelle}
import MichelleParams._
import edu.gemini.spModel.template.SpBlueprint
import edu.gemini.spModel.data.YesNoType
import edu.gemini.phase2.template.factory.impl.{TemplateDsl2, TemplateDsl, ObservationEditor, GroupInitializer}

trait MichelleBase[B <: SpBlueprint] extends GroupInitializer[B] with TemplateDsl2[InstMichelle] {

  val program = "MICHELLE PHASE I/II MAPPING BPS"
  val seqConfigCompType = SeqConfigMichelle.SP_TYPE
  val instCompType = InstMichelle.SP_TYPE

  // No DB for now
  val db = None

  // Seq params
  val PARAM_FILTER = InstMichelle.FILTER_PROP.getName

  // DSL Extensions
  def setDisperser = mutateStatic[Disperser](_.setDisperser(_))
  def setPolarimetry = mutateStatic[Boolean]((a, b) => a.setPolarimetry(YesNoType.fromBoolean(b)))
  def setFilter = mutateStatic[Filter](_.setFilter(_))
  def setMask = mutateStatic[Mask](_.setMask(_))
  def setTimeOnSource = mutateStatic[Double](_.setTimeOnSource(_))

}
