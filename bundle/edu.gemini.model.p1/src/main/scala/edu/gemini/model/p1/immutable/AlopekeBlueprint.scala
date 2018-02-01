package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object AlopekeBlueprint {
  def apply(m: M.AlopekeBlueprint): AlopekeBlueprint = new AlopekeBlueprint(m)
}

case class AlopekeBlueprint(mode: AlopekeMode) extends GeminiBlueprintBase {
  def name: String = s"ʻAlopeke ${mode.value}"

  def this(m: M.AlopekeBlueprint) = this(
    m.getMode
  )

  override def instrument: Instrument = Instrument.Alopeke

  override def mutable(n: Namer) = {
    val m = Factory.createAlopekeBlueprint
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setMode(mode)
    m
  }

  override def toChoice(n: Namer) = {
    val m = Factory.createAlopekeBlueprintChoice
    m.setAlopeke(mutable(n))
    m
  }
}
