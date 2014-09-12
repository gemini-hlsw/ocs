package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import scala.collection.JavaConverters._

object TrecsBlueprintImaging {
  def apply(m: M.TrecsBlueprintImaging): TrecsBlueprintImaging = new TrecsBlueprintImaging(m)
}

case class TrecsBlueprintImaging(filters: List[TrecsFilter]) extends TrecsBlueprintBase {

  def name: String = "T-ReCS Imaging %s".format(filters.map(_.value).mkString("+"))

  def this(m: M.TrecsBlueprintImaging) = this(
    m.getFilter.asScala.toList
  )

  def mutable(n: Namer) = {
    val m = Factory.createTrecsBlueprintImaging
    m.setId(n.nameOf(this))
    m.setName(name)
    m.getFilter.addAll(filters.asJava)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createTrecsBlueprintChoice
    m.setImaging(mutable(n))
    m
  }
}