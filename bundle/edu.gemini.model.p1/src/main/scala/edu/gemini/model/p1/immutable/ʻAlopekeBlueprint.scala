package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object ʻAlopekeBlueprint {
  def apply(m: M.ʻAlopekeBlueprint): ʻAlopekeBlueprint = new ʻAlopekeBlueprint(m)
}

case class ʻAlopekeBlueprint(mode: ʻAlopekeMode) extends GeminiBlueprintBase {
  def name: String = s"ʻAlopeke ${mode.value}"

  def this(m: M.ʻAlopekeBlueprint) = this(
    m.getMode
  )

  override def instrument: Instrument = Instrument.ʻAlopeke

  override def mutable(n: Namer) = {
    val m = Factory.createʻAlopekeBlueprint
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setMode(mode)
    m
  }

  override def toChoice(n: Namer) = {
    val m = Factory.createʻAlopekeBlueprintChoice
    m.setʻAlopeke(mutable(n))
    m
  }
}
