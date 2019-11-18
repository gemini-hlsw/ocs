package edu.gemini.spModel.core

import java.time.{Instant, LocalDateTime, ZoneOffset}

import scalaz._
import Scalaz._

/** Epoch in Gregorian (?) years. */
final case class Epoch(year: Double) {
//  /** Offset in epoch-years from this `Epoch` to the given `Instant`. */
//  def untilInstant(i: Instant): Double =
//    untilLocalDateTime(LocalDateTime.ofInstant(i, ZoneOffset.UTC))
//
//  /** Offset in epoch-years from this `Epoch` to the given `LocalDateTime`. */
//  def untilLocalDateTime(ldt: LocalDateTime): Double =
//    untilJulianDay(Epoch.Scheme.toJulianDay(ldt))
//
//  def untilJulianDay(jd: Double): Double =
//    untilEpochYear(scheme.fromJulianDay(jd).epochYear)
}


object Epoch {

  val year: Epoch @> Double = Lens.lensu((a, b) => a.copy(year = b), _.year)

  val J2000 = Epoch(2000.0)

}



