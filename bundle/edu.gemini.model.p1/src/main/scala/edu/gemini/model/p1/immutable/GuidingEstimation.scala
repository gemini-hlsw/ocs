package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import GuidingEvaluation._

object GuidingEstimation {
  def apply(m: M.GuidingEstimation): GuidingEstimation = m.getPercentage match {
    case x if x >= 0 && x <= 100 => GuidingEstimation(x)
    case _                       => GuidingEstimation(0)
  }
}

case class GuidingEstimation(perc: Int) {
  require(perc >= 0 && perc <= 100, "Invalid percentage: " + perc)

  def evaluation: GuidingEvaluation = perc match {
    case x if x >= 100              => SUCCESS
    case x if x >    0  && x <= 50  => WARNING
    case x if x <=   0              => FAILURE
    case _                          => CAUTION
  }

  override def toString = perc + "%"

  def mutable = {
    val m = Factory.createGuidingEstimation
    m.setPercentage(perc)
    m.setEvaluation(evaluation)
    m
  }
}