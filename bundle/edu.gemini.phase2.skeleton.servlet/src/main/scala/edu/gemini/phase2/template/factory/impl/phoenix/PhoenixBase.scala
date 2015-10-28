package edu.gemini.phase2.template.factory.impl.phoenix

import edu.gemini.phase2.template.factory.impl.{TemplateDsl2, GroupInitializer}
import edu.gemini.spModel.gemini.phoenix.{PhoenixParams, SeqConfigPhoenix, InstPhoenix}
import edu.gemini.spModel.gemini.phoenix.blueprint.SpPhoenixBlueprint

trait PhoenixBase extends GroupInitializer[SpPhoenixBlueprint] with TemplateDsl2[InstPhoenix] {
  val program           = "PHOENIX PHASE I/II MAPPING BPS"
  val seqConfigCompType = SeqConfigPhoenix.SP_TYPE
  val instCompType      = InstPhoenix.SP_TYPE
  val db                = None
  val setFpu            = mutateStatic[PhoenixParams.Mask](_ setMask _)
  val setFilter         = mutateStatic[PhoenixParams.Filter](_ setFilter _)
  val setExposureTime   = mutateStatic[Double](_ setExposureTime _)
  val setCoadds         = mutateStatic[Int](_ setCoadds _)
}