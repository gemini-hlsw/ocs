package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object PhoenixBlueprint {
  def apply(m: M.PhoenixBlueprint): PhoenixBlueprint = new PhoenixBlueprint(m)
}

case class PhoenixBlueprint(fpu: PhoenixFocalPlaneUnit, filter: PhoenixFilter) extends GeminiBlueprintBase {
  def name: String = s"Phoenix ${fpu.value} ${filter.value}"

  def this(m: M.PhoenixBlueprint) = this(
    m.getFpu,
    m.getFilter
  )

  override def instrument: Instrument = Instrument.Phoenix
  override def ao: AoPerspective = AoNone

  def mutable(n: Namer) = {
    val m = Factory.createPhoenixBlueprint()
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setFpu(fpu)
    m.setFilter(filter)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createPhoenixBlueprintChoice()
    m.setPhoenix(mutable(n))
    m
  }

}