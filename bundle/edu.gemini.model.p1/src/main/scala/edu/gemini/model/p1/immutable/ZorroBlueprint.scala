package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object ZorroBlueprint {
  def apply(m: M.ZorroBlueprint): ZorroBlueprint = new ZorroBlueprint(m)
}

case class ZorroBlueprint(mode: ZorroMode) extends GeminiBlueprintBase {
  def name: String = s"Zorro ${mode.value}"
  override val visitor = true

  def this(m: M.ZorroBlueprint) = this(
    m.getMode
  )

  override def instrument: Instrument = Instrument.Zorro

  override def mutable(n: Namer) = {
    val m = Factory.createZorroBlueprint
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setVisitor(visitor)
    m.setMode(mode)
    m
  }

  override def toChoice(n: Namer) = {
    val m = Factory.createZorroBlueprintChoice
    m.setZorro(mutable(n))
    m
  }
}
