package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object MichelleBlueprintSpectroscopy {
  def apply(m: M.MichelleBlueprintSpectroscopy): MichelleBlueprintSpectroscopy =
    new MichelleBlueprintSpectroscopy(m)
}

case class MichelleBlueprintSpectroscopy(fpu: MichelleFpu, disperser: MichelleDisperser) extends MichelleBlueprintBase {
  def name = "Michelle Scpectroscopy %s %s".format(fpu.value, disperser.value)

  def this(m: M.MichelleBlueprintSpectroscopy) = this(m.getFpu, m.getDisperser)

  def mutable(n: Namer) = {
    val m = Factory.createMichelleBlueprintSpectroscopy
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setFpu(fpu)
    m.setDisperser(disperser)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createMichelleBlueprintChoice
    m.setSpectroscopy(mutable(n))
    m
  }
}