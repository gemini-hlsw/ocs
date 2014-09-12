package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object VisitorBlueprint {
  def apply(m: M.VisitorBlueprint): VisitorBlueprint = new VisitorBlueprint(Site.fromMutable(m.getSite), m.getCustomName)
}

case class VisitorBlueprint(site0: Site, customName: String) extends GeminiBlueprintBase {
  def name: String = s"Visitor - ${site.name} - $customName"
  override def site = site0
  override val visitor = true

  def this(m: M.VisitorBlueprint) = this(Site.fromMutable(m.getSite), m.getCustomName)

  override def instrument: Instrument = Instrument.Visitor

  def mutable(n: Namer) = {
    val m = Factory.createVisitorBlueprint()
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setVisitor(visitor)
    m.setSite(Site.toMutable(site))
    m.setCustomName(customName)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createVisitorBlueprintChoice()
    m.setVisitor(mutable(n))
    m
  }

}