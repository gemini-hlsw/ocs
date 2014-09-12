package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object GpiBlueprint {
  def apply(m: M.GpiBlueprint): GpiBlueprint = new GpiBlueprint(m)
}

case class GpiBlueprint(observingMode: GpiObservingMode, disperser: GpiDisperser) extends GeminiBlueprintBase {
  def name: String = s"GPI ${observingMode.value} ${disperser.value}"

  def this(m: M.GpiBlueprint) = this(
    m.getObservingMode,
    m.getDisperser
  )

  override def instrument: Instrument = Instrument.Gpi
  override def ao: AoPerspective = AoNone

  def mutable(n: Namer) = {
    val m = Factory.createGpiBlueprint()
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setObservingMode(observingMode)
    m.setDisperser(disperser)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createGpiBlueprintChoice()
    m.setGpi(mutable(n))
    m
  }

}