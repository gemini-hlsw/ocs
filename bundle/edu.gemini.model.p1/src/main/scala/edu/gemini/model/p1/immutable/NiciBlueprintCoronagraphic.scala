package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import scala.collection.JavaConverters._

object NiciBlueprintCoronagraphic {
  def apply(m: M.NiciBlueprintCoronagraphic): NiciBlueprintCoronagraphic =
    new NiciBlueprintCoronagraphic(m)
}

case class NiciBlueprintCoronagraphic(fpm: NiciFpm, dichroic: NiciDichroic, redFilters: List[NiciRedFilter], blueFilters: List[NiciBlueFilter]) extends NiciBlueprintBase {
  def name: String = "NICI Coronagraphic %s %s%s".format(fpm.value, dichroic.value, formatFilters)

  def this(m: M.NiciBlueprintCoronagraphic) = this(
    m.getFpm,
    m.getDichroic,
    m.getRedFilter.asScala.toList,
    m.getBlueFilter.asScala.toList
  )

  def mutable(n: Namer) = {
    val m = Factory.createNiciBlueprintCoronagraphic
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setFpm(fpm)
    m.setDichroic(dichroic)
    m.getRedFilter.addAll(redFilters.asJava)
    m.getBlueFilter.addAll(blueFilters.asJava)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createNiciBlueprintChoice
    m.setCoronagraphic(mutable(n))
    m
  }
}