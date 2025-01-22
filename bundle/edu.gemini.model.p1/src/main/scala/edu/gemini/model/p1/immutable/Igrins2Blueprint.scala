package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object Igrins2Blueprint {
  def apply(m: M.Igrins2Blueprint): Igrins2Blueprint = new Igrins2Blueprint(m)
}

case class Igrins2Blueprint(nodding: Igrins2NoddingOption, telluricStars: Int) extends GeminiBlueprintBase {
  require(telluricStars >= 0 && telluricStars <= 2)
  private val ts = if (telluricStars == 1) "1 telluric star" else s"$telluricStars telluric stars"
  def name: String = s"IGRINS-2 ${nodding.value}, $ts"

  def this(m: M.Igrins2Blueprint) = this(
    m.getNodding,
    m.getTelluricStars
  )

  override def instrument: Instrument = Instrument.Igrins2

  override def mutable(n: Namer) = {
    val m = Factory.createIgrins2Blueprint
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setVisitor(visitor)
    m.setNodding(nodding)
    m.setTelluricStars(telluricStars)
    m
  }

  override def toChoice(n: Namer) = {
    val m = Factory.createIgrins2BlueprintChoice
    m.setIgrins2(mutable(n))
    m
  }
}
