package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object NifsBlueprintAo {
  def apply(m: M.NifsBlueprintAo): NifsBlueprintAo = new NifsBlueprintAo(m)
}

case class NifsBlueprintAo(altair: Altair, occultingDisk: NifsOccultingDisk, disperser: NifsDisperer) extends NifsBlueprintBase {
  def name = "NIFS %s %s %s".format(altair.shortName, occultingDisk.value, disperser.value)

  override def ao: AoPerspective = altair.ao

  def this(m: M.NifsBlueprintAo) = this(
    Altair(m.getAltair),
    m.getOccultingDisk,
    m.getDisperser
  )

  def mutable(n: Namer) = {
    val m = Factory.createNifsBlueprintAo
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setAltair(altair.mutable)
    m.setOccultingDisk(occultingDisk)
    m.setDisperser(disperser)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createNifsBlueprintChoice
    m.setAo(mutable(n))
    m
  }
}
