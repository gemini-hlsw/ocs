package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object DssiBlueprint {
  def apply(m: M.DssiBlueprint): DssiBlueprint = new DssiBlueprint()
}

case class DssiBlueprint() extends GeminiBlueprintBase {
  def name: String = "DSSI"
  override val visitor = true

  def this(m: M.DssiBlueprint) = this()

  override def instrument: Instrument = Instrument.Dssi

  def mutable(n: Namer) = {
    val m = Factory.createDssiBlueprint()
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setVisitor(visitor)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createDssiBlueprintChoice()
    m.setDssi(mutable(n))
    m
  }

}