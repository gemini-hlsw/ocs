package jsky.util

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.function.ToLongFunction
import java.util.{Comparator, Date, TimeZone}

object DateUtil {
  private val UTC = ZoneId.of("UTC")
  private val DefaultZone = ZoneId.systemDefault()

  // Standard UTC formatters.
  val YYYY_MMM_DD_HHMM_UTC_Formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm").withZone(UTC)
  val YYYY_MMM_DD_HHMMSS_UTC_Formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withZone(UTC)
  val YYYY_MMM_DD_HHMMSS_Z_UTC_Formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss z").withZone(UTC)
  val HHMMSS_UTC_Formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(UTC)

  // Standard non-UTC formatters.
  val YYYY_MM_DD_HHMMSS_DefaultZone_Formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withZone(DefaultZone)

  // Nonstandard formatters.
  val YYYY_MM_DD_UTC_Formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(UTC)

  // Create a Comparator<String> from a given DateTimeFormatter.
  def createComparator(df: DateTimeFormatter): Comparator[String] = Comparator.comparingLong(new ToLongFunction[String] {
    override def applyAsLong(s: String): Long = ZonedDateTime.parse(s, df).toInstant.toEpochMilli
  })


  // Convenience algorithms.
  def nearestMinute(date: Date, zone: TimeZone): Date = Date.from(nearestMinute(ZonedDateTime.ofInstant(date.toInstant, zone.toZoneId)).toInstant)

  def nearestMinute(dt: ZonedDateTime): ZonedDateTime = dt.plus(30, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.MINUTES)

  def DateUtilJava: DateUtil.type = DateUtil
}
