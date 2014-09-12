package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }

object GmosSBlueprintLongslit {
  def apply(m: M.GmosSBlueprintLongslit) = new GmosSBlueprintLongslit(
    m.getDisperser,
    m.getFilter,
    m.getFpu)
}

case class GmosSBlueprintLongslit(disperser: GmosSDisperser, filter: GmosSFilter, fpu: GmosSFpu)
  extends GmosSBlueprintSpectrosopyBase {

  def name = "GMOS-S Longslit %s %s %s".format(disperser.value, filter.value, fpu.value)

  def mutable(n:Namer) = {
    val m = Factory.createGmosSBlueprintLongslit
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDisperser(disperser)
    m.setFilter(filter)
    m.setFpu(fpu)
    m
  }

  def toChoice(n:Namer) = {
    val c = Factory.createGmosSBlueprintChoice
    c.setLongslit(mutable(n))
    c.setRegime(M.GmosSWavelengthRegime.OPTICAL)
    c
  }
}