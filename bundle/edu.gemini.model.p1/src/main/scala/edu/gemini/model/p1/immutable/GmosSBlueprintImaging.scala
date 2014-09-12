package edu.gemini.model.p1.immutable

import scala.collection.JavaConverters._
import edu.gemini.model.p1.{ mutable => M }

object GmosSBlueprintImaging {
  def apply(m: M.GmosSBlueprintImaging) = new GmosSBlueprintImaging(m.getFilter.asScala.toList)
}

case class GmosSBlueprintImaging(filters: List[GmosSFilter]) extends GmosSBlueprintBase {

  lazy val name = "GMOS-S Imaging %s".format(filters.map(_.value).mkString("+"))

  def mutable(n:Namer) = {
    val m = Factory.createGmosSBlueprintImaging
    m.setId(n.nameOf(this))
    m.setName(name)
    m.getFilter.addAll(filters.asJava)
    m
  }

  def toChoice(n:Namer) = {
    val c = Factory.createGmosSBlueprintChoice
    c.setImaging(mutable(n))
    c.setRegime(M.GmosSWavelengthRegime.OPTICAL)
    c
  }
}