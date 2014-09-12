package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import scala.collection.JavaConverters._

object Flamingos2BlueprintMos {
  def apply(m: M.Flamingos2BlueprintMos): Flamingos2BlueprintMos =
    new Flamingos2BlueprintMos(m)
}

case class Flamingos2BlueprintMos(disperser: Flamingos2Disperser, filters: List[Flamingos2Filter], preImaging: Boolean) extends Flamingos2BlueprintSpectroscopyBase {

  def name: String = "Flamingos2 MOS%s %s %s".format(
    if (preImaging) "+Pre" else "",
    disperser.value,
    filters.map(_.value).mkString("+"))

  def this(m: M.Flamingos2BlueprintMos) = this(
    m.getDisperser,
    m.getFilter.asScala.toList,
    m.isPreimaging
  )

  def mutable(n: Namer) = {
    val m = Factory.createFlamingos2BlueprintMos
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDisperser(disperser)
    m.getFilter.addAll(filters.asJava)
    m.setPreimaging(preImaging)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createFlamingos2BlueprintChoice
    m.setMos(mutable(n))
    m
  }

  def withFilters(lst: List[Flamingos2Filter]): Flamingos2BlueprintMos =
    if (lst == filters) this else copy(filters = lst)
}