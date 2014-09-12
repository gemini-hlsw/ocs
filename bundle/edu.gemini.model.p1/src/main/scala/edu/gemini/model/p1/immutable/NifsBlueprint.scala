package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object NifsBlueprint {
  def apply(m: M.NifsBlueprint): NifsBlueprint = new NifsBlueprint(m)
}

case class NifsBlueprint(disperser: NifsDisperer) extends NifsBlueprintBase {
  def name = s"NIFS ${disperser.value}"

  def this(m: M.NifsBlueprint) = this(m.getDisperser)

  def mutable(n: Namer) = {
    val m = Factory.createNifsBlueprint
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDisperser(disperser)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createNifsBlueprintChoice
    m.setNonAo(mutable(n))
    m
  }
}