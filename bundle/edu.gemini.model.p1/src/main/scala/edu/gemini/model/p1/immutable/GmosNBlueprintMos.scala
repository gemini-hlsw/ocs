package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }

object GmosNBlueprintMos {

  def apply(m: M.GmosNBlueprintMos) = new GmosNBlueprintMos(
    Altair(m.getAltair),
    m.getDisperser,
    m.getFilter,
    m.getFpu,
    m.isNodAndShuffle,
    m.isPreimaging)

}

case class GmosNBlueprintMos(
  altair: Altair,
  disperser: GmosNDisperser,
  filter: GmosNFilter,
  fpu: GmosNMOSFpu,
  nodAndShuffle: Boolean,
  preImaging: Boolean) extends GmosNBlueprintSpectrosopyBase {

  def toChoice(n:Namer) = {
    val c = Factory.createGmosNBlueprintChoice
    c.setMos(mutable(n))
    c.setRegime(M.GmosNWavelengthRegime.OPTICAL)
    c
  }

  def mutable(n:Namer) = {
    val m = Factory.createGmosNBlueprintMos
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDisperser(disperser)
    m.setAltair(altair.mutable)
    m.setFpu(fpu)
    m.setFilter(filter)
    m.setNodAndShuffle(nodAndShuffle)
    m.setPreimaging(preImaging)
    m
  }

  def name = {
    val ns = if (nodAndShuffle) "MOS N+S" else "MOS"
    val pi = if (preImaging) "+Pre" else ""
    // REL-3363: No GMOS-N + Altair offered for 2018B. Remove from name.
    // s"GMOS-N $ns $altair ${fpu.value} ${disperser.value} ${filter.value} $pi"
    s"GMOS-N $ns ${fpu.value} ${disperser.value} ${filter.value} $pi"
  }

}