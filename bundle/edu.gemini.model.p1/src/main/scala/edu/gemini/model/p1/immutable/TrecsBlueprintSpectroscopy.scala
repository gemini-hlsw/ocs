package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object TrecsBlueprintSpectroscopy {
  def apply(m: M.TrecsBlueprintSpectroscopy): TrecsBlueprintSpectroscopy =
    new TrecsBlueprintSpectroscopy(m)
}

case class TrecsBlueprintSpectroscopy(disperser: TrecsDisperser, fpu: TrecsFpu) extends TrecsBlueprintBase {
  def name: String = "T-ReCS Spectroscopy %s %s".format(
    disperser.value, fpu.value)

  def this(m: M.TrecsBlueprintSpectroscopy) = this(
    m.getDisperser,
    m.getFpu
  )

  def mutable(n: Namer) = {
    val m = Factory.createTrecsBlueprintSpectroscopy
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDisperser(disperser)
    m.setFpu(fpu)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createTrecsBlueprintChoice
    m.setSpectroscopy(mutable(n))
    m
  }
}