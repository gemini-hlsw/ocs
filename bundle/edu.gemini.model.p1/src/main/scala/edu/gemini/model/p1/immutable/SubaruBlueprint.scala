package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object SubaruBlueprint {
  def apply(m: M.SubaruBlueprint): SubaruBlueprint = new SubaruBlueprint(m.getInstrument, Option(m.getCustomName))
}

case class SubaruBlueprint(instrument: SubaruInstrument, customName: Option[String]) extends BlueprintBase {
  val name = if (instrument == M.SubaruInstrument.VISITOR) {
      s"Subaru (${instrument.value} - ${customName.getOrElse("Unknown")})"
    } else {
      s"Subaru (${instrument.value})"
    }
  val site = Site.Subaru
  def toChoice(n: Namer) = mutable(n)
  def mutable(n: Namer) = {
    val m = Factory.createSubaruBlueprint
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setInstrument(instrument)
    customName.foreach(m.setCustomName)
    m
  }
}