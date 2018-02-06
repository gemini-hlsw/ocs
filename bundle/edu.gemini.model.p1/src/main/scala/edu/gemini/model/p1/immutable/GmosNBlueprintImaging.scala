package edu.gemini.model.p1.immutable

import scala.collection.JavaConverters._
import edu.gemini.model.p1.{ mutable => M }

object GmosNBlueprintImaging {
  def apply(m: M.GmosNBlueprintImaging) = new GmosNBlueprintImaging(m)
}

case class GmosNBlueprintImaging(altair: Altair, filters: List[GmosNFilter]) extends GmosNBlueprintBase {

  // REL-3363: No GMOS-N + Altair offered for 2018B. Remove from name.
  // lazy val name = s"GMOS-N Imaging $altair ${filters.map(_.value).mkString("+")}"
  lazy val name = s"GMOS-N Imaging ${filters.map(_.value).mkString("+")}"

  def toChoice(n:Namer) = {
    val c = Factory.createGmosNBlueprintChoice
    c.setImaging(mutable(n))
    c.setRegime(M.GmosNWavelengthRegime.OPTICAL)
    c
  }

  def this(m: M.GmosNBlueprintImaging) = this(Altair(m.getAltair), m.getFilter.asScala.toList)

  def mutable(n:Namer) = {
    val m = Factory.createGmosNBlueprintImaging
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setAltair(altair.mutable)
    m.getFilter.addAll(filters.asJava)
    m
  }
}