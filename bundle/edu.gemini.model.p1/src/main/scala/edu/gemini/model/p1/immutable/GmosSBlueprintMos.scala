package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }

object GmosSBlueprintMos {

  def apply(m: M.GmosSBlueprintMos) = new GmosSBlueprintMos(
    m.getDisperser,
    m.getFilter,
    m.getFpu,
    m.isNodAndShuffle,
    m.isPreimaging)
}

case class GmosSBlueprintMos(
  disperser: GmosSDisperser,
  filter: GmosSFilter,
  fpu: GmosSMOSFpu,
  nodAndShuffle: Boolean,
  preImaging: Boolean) extends GmosSBlueprintSpectrosopyBase {

  def name = {
    val ns = if (nodAndShuffle) "MOS N+S" else "MOS"
    val pi = if (preImaging) "+Pre" else ""
    s"GMOS-S $ns ${fpu.value} ${disperser.value} ${filter.value} $pi"
  }

  def mutable(n:Namer) = {
    val m = Factory.createGmosSBlueprintMos
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDisperser(disperser)
    m.setFpu(fpu)
    m.setFilter(filter)
    m.setNodAndShuffle(nodAndShuffle)
    m.setPreimaging(preImaging)
    m
  }

  def toChoice(n:Namer) = {
    val c = Factory.createGmosSBlueprintChoice
    c.setMos(mutable(n))
    c.setRegime(M.GmosSWavelengthRegime.OPTICAL)
    c
  }
}