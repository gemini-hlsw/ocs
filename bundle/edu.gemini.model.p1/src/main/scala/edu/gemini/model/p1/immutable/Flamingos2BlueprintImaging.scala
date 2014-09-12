package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import scala.collection.JavaConverters._

object Flamingos2BlueprintImaging {
  def apply(m: M.Flamingos2BlueprintImaging): Flamingos2BlueprintImaging =
    new Flamingos2BlueprintImaging(m)
}

case class Flamingos2BlueprintImaging(filters: List[Flamingos2Filter]) extends Flamingos2BlueprintBase {

  def name: String = "Flamingos2 Imaging %s".format(filters.map(_.value).mkString("+"))

  def this(m: M.Flamingos2BlueprintImaging) = this(
    m.getFilter.asScala.toList
  )

  def mutable(n: Namer) = {
    val m = Factory.createFlamingos2BlueprintImaging
    m.setId(n.nameOf(this))
    m.setName(name)
    m.getFilter.addAll(filters.asJava)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createFlamingos2BlueprintChoice
    m.setImaging(mutable(n))
    m
  }
}