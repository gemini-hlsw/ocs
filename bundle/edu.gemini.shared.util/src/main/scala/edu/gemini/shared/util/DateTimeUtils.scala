package edu.gemini.shared.util

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.{Comparator, Date, TimeZone}
import java.util.function.ToLongFunction

import scala.concurrent.duration._
import scalaz._
import Scalaz._

object DateTimeUtils {
  val UTC: ZoneId               = ZoneId.of("UTC")
  val SystemDefaultZone: ZoneId = ZoneId.systemDefault()

  // Create a Comparator<String> from a given DateTimeFormatter.
  def createComparator(df: DateTimeFormatter): Comparator[String] = Comparator.comparingLong(new ToLongFunction[String] {
    override def applyAsLong(s: String): Long = ZonedDateTime.parse(s, df).toInstant.toEpochMilli
  })


  // Convenience algorithms.
  def nearestMinute(date: Date, zone: TimeZone): Date = Date.from(nearestMinute(ZonedDateTime.ofInstant(date.toInstant, zone.toZoneId)).toInstant)
  def nearestMinute(dt: ZonedDateTime): ZonedDateTime = dt.plus(30, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.MINUTES)

  def startOfDayInMs(ms: Long, zone: ZoneId): Long = {
    val zdt = Instant.ofEpochMilli(ms - StartOfDayHour.hours.toMillis).atZone(zone)
    zdt.withHour(StartOfDayHour).truncatedTo(ChronoUnit.HOURS).toInstant.toEpochMilli
  }

  def endOfDayInMs(ms: Long, zone: ZoneId): Long = {
    val start = startOfDayInMs(ms, zone)
    val zdt = Instant.ofEpochMilli(start).atZone(zone)
    zdt.plus(1, ChronoUnit.DAYS).toInstant.toEpochMilli
  }

  def timeInMs(year: Int, month: Int, dayOfMonth: Int, hours: Int, minutes: Int, seconds: Int, zone: ZoneId): Long =
    ZonedDateTime.of(year, month, dayOfMonth, hours, minutes, seconds, 0, zone).toInstant.toEpochMilli


  // Java time API doesn't support formatters for Duration, so we have to roll our own.

  // Hours zero-padded to two decimal places.
  def msToHHMM(span: Long): String = {
    val neg = span < 0
    val d   = Math.abs(span).milliseconds
    val hh  = d.toHours
    val mm  = d.toMinutes % MinutesPerHour
    f"${neg ? "-" | ""}%s$hh%02d:$mm%02d"
  }

  def msToHMM(span: Long): String = {
    val neg = span < 0
    val d   = Math.abs(span).milliseconds
    val hh  = d.toHours
    val mm  = d.toMinutes % MinutesPerHour
    f"${neg ? "-" | ""}%s$hh:$mm%02d"
  }

  def msToHMMSS(span: Long): String = {
    val neg = span < 0
    val d   = Math.abs(span).milliseconds
    val hh  = d.toHours
    val mm  = d.toMinutes % MinutesPerHour
    val ss  = d.toSeconds % SecondsPerMinute
    f"${neg ? "-" | ""}%s$hh:$mm%02d:$ss%02d"
  }

  def msToMSS(span: Long): String = {
    val neg = span < 0
    val d   = Math.abs(span).milliseconds
    val mm  = d.toMinutes
    val ss  = d.toSeconds % SecondsPerHour
    f"${neg ? "-" | ""}%s$mm:$ss%02d"
  }

  def hoursToHMM(hours: Double): String =
    msToHMM((hours * MillisecondsPerHour).toLong)

  def hoursToHMMSS(hours: Double): String =
    msToHMMSS((hours * MillisecondsPerHour).toLong)

  // Some useful constants.
  val StartOfDayHour:        Int  = 14
  val StartOfDayHourInMs:    Long = StartOfDayHour.hours.toMillis
  val HoursPerDay:           Long = 1.day.toHours
  val MinutesPerHour:        Long = 1.hour.toMinutes
  val SecondsPerMinute:      Long = 1.minute.toSeconds
  val SecondsPerHour:        Long = 1.hour.toSeconds
  val MillisecondsPerSecond: Long = 1.second.toMillis
  val MillisecondsPerMinute: Long = 1.minute.toMillis
  val MillisecondsPerHour:   Long = 1.hour.toMillis
  val MillisecondsPerDay:    Long = 1.day.toMillis

  def DateTimeUtilsJava: DateTimeUtils.type = DateTimeUtils
}

case class DateTimeFormatters(zone: ZoneId) {
  val YYYY_MM_DD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zone)
  val YYYY_MMM_DD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd").withZone(zone)
  val YYYY_MMM_DD_HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm").withZone(zone)
  val YYYY_MMM_DD_HHMMSS: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withZone(zone)
  val YYYY_MMM_DD_HHMMSS_Z: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss z").withZone(zone)
  val HHMMSS: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(zone)

  // Nonstandard formatters.
  val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(zone)

  def DateTimeFormattersJava: DateTimeFormatters.type = DateTimeFormatters
}
object DateTimeFormatters {
  def apply(zone: TimeZone): DateTimeFormatters = DateTimeFormatters(zone.toZoneId)
}

object UTCDateTimeFormatters extends DateTimeFormatters(DateTimeUtils.UTC) {
  def UTCDateTimeFormattersJava: UTCDateTimeFormatters.type = UTCDateTimeFormatters
}
object SystemDefaultDateTimeFormatters extends DateTimeFormatters(DateTimeUtils.SystemDefaultZone) {
  def SystemDefaultDateTimeFormattersJava: SystemDefaultDateTimeFormatters.type = SystemDefaultDateTimeFormatters
}