package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object GracesBlueprint {
  def apply(m: M.GracesBlueprint): GracesBlueprint = new GracesBlueprint(m)
}

case class GracesBlueprint(fiberMode: GracesFiberMode, readMode: GracesReadMode) extends GeminiBlueprintBase {
  def name: String = s"Graces ${fiberMode.value} ${readMode.value}"
  override val visitor = true

  def this(m: M.GracesBlueprint) = this(
    m.getFiberMode, m.getReadMode
  )

  override def instrument: Instrument = Instrument.Graces
  override def ao: AoPerspective = AoNone

  def mutable(n: Namer) = {
    val m = Factory.createGracesBlueprint()
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setFiberMode(fiberMode)
    m.setReadMode(readMode)
    m.setVisitor(visitor)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createGracesBlueprintChoice()
    m.setGraces(mutable(n))
    m
  }

}