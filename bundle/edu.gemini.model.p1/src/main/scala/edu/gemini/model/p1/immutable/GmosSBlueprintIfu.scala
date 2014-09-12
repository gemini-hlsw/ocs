package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }

object GmosSBlueprintIfu {

  def apply(m: M.GmosSBlueprintIfu) = new GmosSBlueprintIfu(
    m.getDisperser,
    m.getFilter,
    m.getFpu)
}

case class GmosSBlueprintIfu(
  disperser: GmosSDisperser,
  filter: GmosSFilter,
  fpu: GmosSFpuIfu) extends GmosSBlueprintSpectrosopyBase {

  def name =
    "GMOS-S IFU %s %s %s".format(disperser.value, filter.value, fpu.value)

  def mutable(n:Namer) = {
    val m = Factory.createGmosSBlueprintIfu
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDisperser(disperser)
    m.setFilter(filter)
    m.setFpu(fpu)
    m
  }

  def toChoice(n:Namer) = {
    val c = Factory.createGmosSBlueprintChoice
    c.setIfu(mutable(n))
    c.setRegime(M.GmosSWavelengthRegime.OPTICAL)
    c
  }
}