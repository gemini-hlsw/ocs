package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

import scala.collection.JavaConverters._


object NiriBlueprint {
  def apply(m: M.NiriBlueprint): NiriBlueprint = new NiriBlueprint(m)
}

case class NiriBlueprint(altair: Altair, camera: NiriCamera, filters: List[NiriFilter]) extends GeminiBlueprintBase {
  def name: String = "NIRI %s %s %s".format(
    altair, camera.value, filters.map(_.value).mkString("+"))

  def this(m: M.NiriBlueprint) = this(
    Altair(m.getAltair),
    m.getCamera,
    m.getFilter.asScala.toList
  )

  override def instrument: Instrument = Instrument.Niri
  override def ao: AoPerspective = altair.ao

  def mutable(n: Namer) = {
    val m = Factory.createNiriBlueprint
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setAltair(altair.mutable)
    m.setCamera(camera)
    m.getFilter.addAll(filters.asJava)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createNiriBlueprintChoice()
    m.setNiri(mutable(n))
    m
  }

}