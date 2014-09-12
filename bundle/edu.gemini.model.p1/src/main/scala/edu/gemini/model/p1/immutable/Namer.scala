package edu.gemini.model.p1.immutable

class Namer {

  private val tns = new NameSupply[Target]("target")
  private val bns = new NameSupply[BlueprintBase]("blueprint")
  private val cns = new NameSupply[Condition]("condition")
  private val ins = new NameSupply[Investigator]("investigator")

  def nameOf(t:Target) = tns.nameOf(t)
  def nameOf(b:BlueprintBase) = bns.nameOf(b)
  def nameOf(c:Condition) = cns.nameOf(c)
  def nameOf(i:Investigator) = ins.nameOf(i)

}

