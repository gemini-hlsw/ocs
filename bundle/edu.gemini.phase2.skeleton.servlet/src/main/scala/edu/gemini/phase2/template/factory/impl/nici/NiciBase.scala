package edu.gemini.phase2.template.factory.impl.nici

import edu.gemini.spModel.gemini.nici.{NICIParams, SeqConfigNICI, InstNICI}
import NICIParams._
import edu.gemini.spModel.gemini.nici.blueprint.SpNiciBlueprintBase
import scala.collection.JavaConverters._
import edu.gemini.phase2.template.factory.impl.{TemplateDsl2, GroupInitializer}

trait NiciBase[B <: SpNiciBlueprintBase] extends GroupInitializer[B] with TemplateDsl2[InstNICI] {

  val program = "NICI PHASE I/II MAPPING BPS"
  val seqConfigCompType = SeqConfigNICI.SP_TYPE
  val instCompType = InstNICI.SP_TYPE

  // No DB for now
  val db = None

  // DSL Extensions
  def setDichroic = mutateStatic[DichroicWheel](_.setDichroicWheel(_))
  def setRedChannelFilter = mutateStatic[Channel1FW](_.setChannel1Fw(_))
  def setBlueChannelFilter = mutateStatic[Channel2FW](_.setChannel2Fw(_))
  def setFPM = mutateStatic[FocalPlaneMask](_.setFocalPlaneMask(_))

  // Helpers
  lazy val firstRedFilterOrBlock = blueprint.redFilters.asScala.headOption.getOrElse(Channel1FW.BLOCK)
  lazy val firstBlueFilterOrBlock = blueprint.blueFilters.asScala.headOption.getOrElse(Channel2FW.BLOCK)

}
