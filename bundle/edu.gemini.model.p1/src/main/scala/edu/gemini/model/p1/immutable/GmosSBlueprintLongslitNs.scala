package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }

object GmosSBlueprintLongslitNs {
  def apply(m: M.GmosSBlueprintLongslitNs) = new GmosSBlueprintLongslitNs(
    m.getDisperser,
    m.getFilter,
    m.getFpu)
}

case class GmosSBlueprintLongslitNs(disperser: GmosSDisperser, filter: GmosSFilter, fpu: GmosSFpuNs)
  extends GmosSBlueprintSpectrosopyBase {

  def name = "GMOS-S LongSlit N+S %s %s %s".format(disperser.value, filter.value, fpu.value)

  def mutable(n:Namer) = {
    val m = Factory.createGmosSBlueprintLongslitNs
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDisperser(disperser)
    m.setFilter(filter)
    m.setFpu(fpu)
    m
  }

  def toChoice(n:Namer) = {
    val c = Factory.createGmosSBlueprintChoice
    c.setLongslitNs(mutable(n))
    c.setRegime(M.GmosSWavelengthRegime.OPTICAL)
    c
  }
}