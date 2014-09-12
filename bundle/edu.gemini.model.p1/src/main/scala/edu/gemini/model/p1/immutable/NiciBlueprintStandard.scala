package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import scala.collection.JavaConverters._

object NiciBlueprintStandard {
  def apply(m: M.NiciBlueprintStandard): NiciBlueprintStandard =
    new NiciBlueprintStandard(m)
}

case class NiciBlueprintStandard(dichroic: NiciDichroic, redFilters: List[NiciRedFilter], blueFilters: List[NiciBlueFilter]) extends NiciBlueprintBase {
  def name: String = "NICI Standard %s%s".format(dichroic.value, formatFilters)

  def this(m: M.NiciBlueprintStandard) = this(
    m.getDichroic,
    m.getRedFilter.asScala.toList,
    m.getBlueFilter.asScala.toList
  )

  def mutable(n: Namer) = {
    val m = Factory.createNiciBlueprintStandard
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDichroic(dichroic)
    m.getRedFilter.addAll(redFilters.asJava)
    m.getBlueFilter.addAll(blueFilters.asJava)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createNiciBlueprintChoice
    m.setStandard(mutable(n))
    m
  }
}