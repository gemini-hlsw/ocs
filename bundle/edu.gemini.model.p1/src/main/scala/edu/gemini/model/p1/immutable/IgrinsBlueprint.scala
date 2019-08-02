package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.mutable.IgrinsBlueprintChoice
import edu.gemini.model.p1.{mutable => M}

object IgrinsBlueprint {
  def apply(m: M.IgrinsBlueprint): IgrinsBlueprint = new IgrinsBlueprint(m)
}

case class IgrinsBlueprint() extends GeminiBlueprintBase {
  override val name: String = "IGRINS"
  override val visitor = true

  def this(m: M.IgrinsBlueprint) = this()

  override def instrument: Instrument = Instrument.Igrins

  override def mutable(n: Namer): M.IgrinsBlueprint = {
    val m = Factory.createIgrinsBlueprint
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setVisitor(visitor)
    m
  }

  override def toChoice(n: Namer): IgrinsBlueprintChoice = {
    val m = Factory.createIgrinsBlueprintChoice
    m.setIgrins(mutable(n))
    m
  }
}