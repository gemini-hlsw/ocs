package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

import scala.collection.JavaConverters._


object GsaoiBlueprint {
  def apply(m: M.GsaoiBlueprint): GsaoiBlueprint = new GsaoiBlueprint(m)
}

case class GsaoiBlueprint(filters: List[GsaoiFilter]) extends GeminiBlueprintBase {
  def name: String = "GSAOI %s".format(filters.map(_.value).mkString("+"))

  def this(m: M.GsaoiBlueprint) = this(
    m.getFilter.asScala.toList
  )

  override def instrument: Instrument = Instrument.Gsaoi
  override def ao: AoPerspective = AoLgs

  def mutable(n: Namer) = {
    val m = Factory.createGsaoiBlueprint()
    m.setId(n.nameOf(this))
    m.setName(name)
    m.getFilter.addAll(filters.asJava)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createGsaoiBlueprintChoice()
    m.setGsaoi(mutable(n))
    m
  }

}