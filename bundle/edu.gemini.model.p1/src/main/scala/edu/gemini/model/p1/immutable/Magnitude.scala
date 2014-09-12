package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }

import MagnitudeBand._
import MagnitudeSystem._

object Magnitude {
  def apply(m: M.Magnitude) = new Magnitude(m)
}

case class Magnitude(value: Double, band: MagnitudeBand, system: MagnitudeSystem) {

  def this(m: M.Magnitude) = this(
    m.getValue.doubleValue,
    m.getBand,
    m.getSystem)

  def mutable = {
    val m = Factory.createMagnitude
    m.setValue(BigDecimal(value).bigDecimal)
    m.setBand(band)
    m.setSystem(system)
    m
  }

}