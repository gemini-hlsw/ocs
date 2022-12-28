package edu.gemini.spModel.core

import java.time._

import scalaz._
import Scalaz._

/**
 * An epoch, the astronomer's equivalent of `Instant`, based on a fractional year in some temporal
 * scheme (Julian, in this case) that determines year zero and the length of a year. The only
 * meaningful operation for an `Epoch` is to ask the elapsed epoch-years between it and some other
 * point in time. We need this for proper motion corrections because velocities are measured in
 * motion per epoch-year.
 *
 * @see The Wikipedia [[https://en.wikipedia.org/wiki/Epoch_(astronomy) article]]
 */
final case class Epoch(year: Double) {
  /** Offset in epoch-years from this `Epoch` to the given `Instant`. */
  def untilInstant(i: Instant): Double =
    untilLocalDateTime(LocalDateTime.ofInstant(i, ZoneOffset.UTC))

  /** Offset in epoch-years from this `Epoch` to the given `LocalDateTime`. */
  def untilLocalDateTime(ldt: LocalDateTime): Double =
    untilJulianDay(Epoch.toJulianDay(ldt))

  /** Offset in epoch-years from this `Epoch` to the given fractional Julian day. */
  def untilJulianDay(jd: Double): Double =
    untilEpochYear(Epoch.JulianYearBasis + (jd - Epoch.JulianBasis) / Epoch.JulianLengthOfYear)

  /** Offset in epoch-years from this `Epoch` to the given epoch year under the same scheme. */
  def untilEpochYear(epochYear: Double): Double =
    epochYear - year
}

object Epoch {
  /**
   * Standard J2000 epoch and Julian constants.
   */
  val JulianYearBasis: Double = 2000.0
  val JulianBasis: Double = 2451545.0
  val JulianLengthOfYear: Double = 365.25
  val J2000: Epoch = Epoch(JulianYearBasis)

  def toJulianDay(dt: LocalDateTime): Double =
    JulianDate.ofLocalDateTime(dt).dayNumber.toDouble

  val year: Epoch @> Double = Lens.lensu((a, b) => a.copy(year = b), _.year)

  implicit val EpochOrder: Order[Epoch] =
    Order.orderBy(_.year)

  implicit val EpochShow: Show[Epoch] =
    Show.showFromToString
}
