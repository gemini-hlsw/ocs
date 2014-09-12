package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object KeckBlueprint {
  def apply(m: M.KeckBlueprint): KeckBlueprint = new KeckBlueprint(m.getInstrument)
}

case class KeckBlueprint(instrument: KeckInstrument) extends BlueprintBase {
  def name = "Keck (%s)".format(instrument.name)
  def site = Site.Keck
  def toChoice(n: Namer) = mutable(n)
  def mutable(n: Namer) = {
    val m = Factory.createKeckBlueprint
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setInstrument(instrument)
    m
  }
}