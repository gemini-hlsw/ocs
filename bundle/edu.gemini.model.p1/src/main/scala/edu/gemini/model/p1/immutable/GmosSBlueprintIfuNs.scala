package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }

object GmosSBlueprintIfuNs {

  def apply(m: M.GmosSBlueprintIfuNs) = new GmosSBlueprintIfuNs(
    m.getDisperser,
    m.getFilter,
    m.getFpu)
}

case class GmosSBlueprintIfuNs(
  disperser: GmosSDisperser,
  filter: GmosSFilter,
  fpu: GmosSFpuIfuNs) extends GmosSBlueprintSpectrosopyBase {

  def name =
    "GMOS-S IFU N+S %s %s %s".format(disperser.value, filter.value, fpu.value)

  def mutable(n:Namer) = {
    val m = Factory.createGmosSBlueprintIfuNs
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDisperser(disperser)
    m.setFilter(filter)
    m.setFpu(fpu)
    m
  }

  def toChoice(n:Namer) = {
    val c = Factory.createGmosSBlueprintChoice
    c.setIfuNs(mutable(n))
    c.setRegime(M.GmosSWavelengthRegime.OPTICAL)
    c
  }
}