package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }

object GmosNBlueprintLongslit {

  def apply(m: M.GmosNBlueprintLongslit) = new GmosNBlueprintLongslit(
    Altair(m.getAltair),
    m.getDisperser,
    m.getFilter,
    m.getFpu)

}

case class GmosNBlueprintLongslit(altair: Altair, disperser: GmosNDisperser, filter: GmosNFilter, fpu: GmosNFpu)
  extends GmosNBlueprintSpectrosopyBase {

  def toChoice(n:Namer) = {
    val c = Factory.createGmosNBlueprintChoice
    c.setLongslit(mutable(n))
    c.setRegime(M.GmosNWavelengthRegime.OPTICAL)
    c
  }

  def mutable(n:Namer) = {
    val m = Factory.createGmosNBlueprintLongslit
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDisperser(disperser)
    m.setAltair(altair.mutable)
    m.setFilter(filter)
    m.setFpu(fpu)
    m
  }

  // REL-3363: No GMOS-N + Altair offered for 2018B. Remove from name.
  // def name = s"GMOS-N LongSlit $altair ${disperser.value} ${filter.value} ${fpu.value}"
  def name = s"GMOS-N LongSlit ${disperser.value} ${filter.value} ${fpu.value}"
}