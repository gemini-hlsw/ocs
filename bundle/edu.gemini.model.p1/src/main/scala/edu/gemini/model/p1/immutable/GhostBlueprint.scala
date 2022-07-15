package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object GhostBlueprint {
  def apply(m: M.GhostBlueprint): GhostBlueprint = new GhostBlueprint(m)
}

case class GhostBlueprint(resolutionMode: GhostResolutionMode, targetMode: GhostTargetMode) extends GeminiBlueprintBase {
  def name: String = s"Ghost ${resolutionMode.value} ${targetMode.value}"

  def this(m: M.GhostBlueprint) = this(
    m.getResolutionMode,
    m.getTargetMode
  )

  override def instrument: Instrument = Instrument.Ghost
  override def ao: AoPerspective = AoNone

  def mutable(n: Namer) = {
    val m = Factory.createGhostBlueprint()
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setResolutionMode(resolutionMode)
    m.setTargetMode(targetMode)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createGhostBlueprintChoice()
    m.setGhost(mutable(n))
    m
  }

}
