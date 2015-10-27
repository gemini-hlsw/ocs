package edu.gemini.phase2.template.factory.impl.phoenix

import edu.gemini.phase2.template.factory.impl.{TemplateDsl2, GroupInitializer}
import edu.gemini.spModel.gemini.phoenix.{PhoenixParams, SeqConfigPhoenix, InstPhoenix}
import edu.gemini.spModel.gemini.phoenix.blueprint.SpPhoenixBlueprint

trait PhoenixBase extends GroupInitializer[SpPhoenixBlueprint] with TemplateDsl2[InstPhoenix] {
  val program           = "PHOENIX PHASE I/II MAPPING BPS"
  val seqConfigCompType = SeqConfigPhoenix.SP_TYPE
  val instCompType      = InstPhoenix.SP_TYPE
  val db                = None
  val setFpu            = mutateStatic[PhoenixParams.Mask]((a, b) => a.setMask(b))
  val setFilter         = mutateStatic[PhoenixParams.Filter]((a, b) => a.setFilter(b))
  val setExposureTime   = mutateStatic[Double]((a, b) => a.setExposureTime(b))
  val setCoadds         = mutateStatic[Int]((a, b) => a.setCoadds(b))
}