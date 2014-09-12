package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object TexesBlueprint {
  def apply(m: M.TexesBlueprint): TexesBlueprint = new TexesBlueprint(m)
}

case class TexesBlueprint(disperser: TexesDisperser) extends GeminiBlueprintBase {
  def name: String = s"Texes ${disperser.value}"
  override val visitor = true

  def this(m: M.TexesBlueprint) = this(
    m.getDisperser
  )

  override def instrument: Instrument = Instrument.Texes

  def mutable(n: Namer) = {
    val m = Factory.createTexesBlueprint()
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDisperser(disperser)
    m.setVisitor(visitor)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createTexesBlueprintChoice()
    m.setTexes(mutable(n))
    m
  }

}