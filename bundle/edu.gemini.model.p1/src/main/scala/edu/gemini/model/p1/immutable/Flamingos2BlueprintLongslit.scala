package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import scala.collection.JavaConverters._

object Flamingos2BlueprintLongslit {
  def apply(m: M.Flamingos2BlueprintLongslit): Flamingos2BlueprintLongslit =
    new Flamingos2BlueprintLongslit(m)
}

case class Flamingos2BlueprintLongslit(disperser: Flamingos2Disperser, filters: List[Flamingos2Filter], fpu: Flamingos2Fpu) extends Flamingos2BlueprintSpectroscopyBase {

  def name: String = "Flamingos2 Longslit %s %s %s".format(
    disperser.value,
    filters.map(_.value).mkString("+"),
    fpu.value)

  def this(m: M.Flamingos2BlueprintLongslit) = this(
    m.getDisperser,
    m.getFilter.asScala.toList,
    m.getFpu
  )

  def mutable(n: Namer) = {
    val m = Factory.createFlamingos2BlueprintLongslit
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDisperser(disperser)
    m.getFilter.addAll(filters.asJava)
    m.setFpu(fpu)
    m
  }

  def toChoice(n: Namer) = {
    val m = Factory.createFlamingos2BlueprintChoice
    m.setLongslit(mutable(n))
    m
  }

  def withFilters(lst: List[Flamingos2Filter]): Flamingos2BlueprintLongslit =
    if (lst == filters) this else copy(filters = lst)
}