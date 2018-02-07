package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }

object GmosNBlueprintLongslitNs {

  def apply(m: M.GmosNBlueprintLongslitNs) = new GmosNBlueprintLongslitNs(
    Altair(m.getAltair),
    m.getDisperser,
    m.getFilter,
    m.getFpu)

}

case class GmosNBlueprintLongslitNs(altair: Altair, disperser: GmosNDisperser, filter: GmosNFilter, fpu: GmosNFpuNs)
  extends GmosNBlueprintSpectrosopyBase {

  def toChoice(n:Namer) = {
    val c = Factory.createGmosNBlueprintChoice
    c.setLongslitNs(mutable(n))
    c.setRegime(M.GmosNWavelengthRegime.OPTICAL)
    c
  }

  def mutable(n:Namer) = {
    val m = Factory.createGmosNBlueprintLongslitNs
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDisperser(disperser)
    m.setAltair(altair.mutable)
    m.setFilter(filter)
    m.setFpu(fpu)
    m
  }

  // REL-3363: No GMOS-N + Altair offered for 2018B. Remove from name.
  // def name = s"GMOS-N LongSlit N+S $altair ${disperser.value} ${filter.value} ${fpu.value}"
  def name = s"GMOS-N LongSlit N+S ${disperser.value} ${filter.value} ${fpu.value}"

}