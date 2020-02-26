package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }

object ProperMotion {
  def apply(m:M.ProperMotion):ProperMotion = ProperMotion(m.getDeltaRA.doubleValue, m.getDeltaDec.doubleValue)
}

case class ProperMotion(deltaRA:Double, deltaDec:Double) {

  def mutable = {
    val m = Factory.createProperMotion()
    m.setDeltaRA(BigDecimal(deltaRA).bigDecimal)
    m.setDeltaDec(BigDecimal(deltaDec).bigDecimal)
    m
  }

}
