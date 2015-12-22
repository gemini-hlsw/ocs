package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object ObservationMeta {
  def apply(m: M.ObservationMetaData): ObservationMeta = {
    val guiding    = Option(m.getGuiding) map { g => GuidingEstimation(g) }
    val visibility = Option(m.getVisibility) map { v => TargetVisibility(v) }
    val gsa        = Option(m.getGsa) map { bi => bi.intValue }
    ObservationMeta(guiding, visibility, gsa)
  }

  val empty = ObservationMeta(None, None, None)
}

case class ObservationMeta(guiding: Option[GuidingEstimation],
                           visibility: Option[TargetVisibility],
                           gsa: Option[Int]) {

  def mutable = {
    val m = Factory.createObservationMetaData
    guiding.foreach { g => m.setGuiding(g.mutable) }
    visibility.foreach { v => m.setVisibility(v.mutable) }
    gsa.foreach { g => m.setGsa(java.math.BigInteger.valueOf(g.toLong))}
    m.setCk("")
    m
  }
}