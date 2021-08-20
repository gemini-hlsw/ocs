package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.mutable.MaroonXBlueprintChoice
import edu.gemini.model.p1.{mutable => M}

object MaroonXBlueprint {
  def apply(m: M.MaroonXBlueprint): MaroonXBlueprint = new MaroonXBlueprint(m)
}

case class MaroonXBlueprint() extends GeminiBlueprintBase {
  override val name: String = "MAROON-X"
  override val visitor: Boolean = true

  def this(m: M.MaroonXBlueprint) = this()

  override def instrument: Instrument = Instrument.MaroonX

  override def mutable(n: Namer): M.MaroonXBlueprint = {
    val m = Factory.createMaroonXBlueprint
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setVisitor(visitor)
    m
  }

  override def toChoice(n: Namer): MaroonXBlueprintChoice = {
    val m = Factory.createMaroonXBlueprintChoice
    m.setMaroonX(mutable(n))
    m
  }
}
