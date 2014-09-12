package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

import scala.collection.JavaConverters._

object MichelleBlueprintImaging {
  def apply(m: M.MichelleBlueprintImaging): MichelleBlueprintImaging =
    new MichelleBlueprintImaging(m)
}

case class MichelleBlueprintImaging(filters: List[MichelleFilter], polarimetry: MichellePolarimetry) extends MichelleBlueprintBase {
  def name = "Michelle Imaging %s %s".format(
    filters.map(_.value).mkString(", "),
    if (polarimetry == MichellePolarimetry.YES) "Polarimetry" else "")

  def this(m: M.MichelleBlueprintImaging) = this(
    m.getFilter.asScala.toList,
    m.getPolarimetry
  )

  def mutable(n: Namer) = {
    val m = Factory.createMichelleBlueprintImaging
    m.setId(n.nameOf(this))
    m.setName(name)
    m.getFilter.addAll(filters.asJava)
    m.setPolarimetry(polarimetry)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createMichelleBlueprintChoice
    m.setImaging(mutable(n))
    m
  }
}