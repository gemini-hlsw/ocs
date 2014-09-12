package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }

object Condition {

  def empty = new Condition(
    None,
    M.CloudCover.cc100,
    M.ImageQuality.iq100,
    M.SkyBackground.sb100,
    M.WaterVapor.wv100)

  def apply(m: M.Condition) = new Condition(
    Option(m.getMaxAirmass).map(_.doubleValue),
    m.getCc,
    m.getIq,
    m.getSb,
    m.getWv)

}

case class Condition(
  maxAirmass: Option[Airmass],
  cc: CloudCover,
  iq: ImageQuality,
  sb: SkyBackground,
  wv: WaterVapor) {

  protected val idRegistry = Condition

  def name = maxAirmass match {
    case Some(am) => "CC %s, IQ %s, SB %s, WV %s, AM ≤ %3.2f".format(cc.value, iq.value, sb.value, wv.value, am)
    case None     => "CC %s, IQ %s, SB %s, WV %s".format(cc.value, iq.value, sb.value, wv.value)
  }

  def mutable(n:Namer) = {
    val m = Factory.createCondition
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setMaxAirmass(maxAirmass.map(BigDecimal(_).bigDecimal).orNull)
    m.setCc(cc)
    m.setIq(iq)
    m.setSb(sb)
    m.setWv(wv)
    m
  }

}