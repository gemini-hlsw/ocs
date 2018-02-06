package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }

object GmosNBlueprintIfu {

  def apply(m: M.GmosNBlueprintIfu) = new GmosNBlueprintIfu(
    Altair(m.getAltair),
    m.getDisperser,
    m.getFilter,
    m.getFpu)

}

case class GmosNBlueprintIfu(
  altair: Altair,
  disperser: GmosNDisperser,
  filter: GmosNFilter,
  fpu: GmosNFpuIfu) extends GmosNBlueprintSpectrosopyBase {

  // REL-3363: No GMOS-N + Altair offered for 2018B. Remove from name.
  // def name = s"GMOS-N IFU $altair ${disperser.value} ${filter.value} ${fpu.value}"
  def name = s"GMOS-N IFU ${disperser.value} ${filter.value} ${fpu.value}"

  def mutable(n:Namer) = {
    val m = Factory.createGmosNBlueprintIfu
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDisperser(disperser)
    m.setAltair(altair.mutable)
    m.setFilter(filter)
    m.setFpu(fpu)
    m
  }

  def toChoice(n:Namer) = {
    val c = Factory.createGmosNBlueprintChoice
    c.setIfu(mutable(n))
    c.setRegime(M.GmosNWavelengthRegime.OPTICAL)
    c
  }
}