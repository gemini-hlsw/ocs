package edu.gemini.phase2.template.factory.impl.gpi

import edu.gemini.phase2.template.factory.impl._
import edu.gemini.spModel.gemini.gpi.blueprint.SpGpiBlueprint
import edu.gemini.spModel.gemini.gpi.{Gpi => InstGpi, SeqConfigGpi}
import edu.gemini.spModel.gemini.gpi.Gpi.Filter.{Y, J, H, K1, K2}
import edu.gemini.spModel.gemini.gpi.Gpi.ObservingMode

sealed trait GpiFilterGroup {
  def filters: Set[InstGpi.Filter]
}

object GpiFilterGroup {
  case object Yjh extends GpiFilterGroup {
    val filters = Set(Y, J, H)
  }

  case object K1k2 extends GpiFilterGroup {
    val filters = Set(K1, K2)
  }

  val all: List[GpiFilterGroup] = List(Yjh, K1k2)

  def lookup(m: ObservingMode): Option[GpiFilterGroup] =
    all.find(_.filters.contains(m.getFilter))
}

trait GpiBase[B <: SpGpiBlueprint] extends GroupInitializer[B] with TemplateDsl2[InstGpi] {
  val program           = "GPI PHASE I/II MAPPING BPS"
  val seqConfigCompType = SeqConfigGpi.SP_TYPE
  val instCompType      = InstGpi.SP_TYPE

  // No DB for now
  val db = None

  // DSL Extensions
  val setObservingMode = mutateStatic[InstGpi.ObservingMode]((gpi, o) => gpi.setObservingMode(new edu.gemini.shared.util.immutable.Some(o)))
}