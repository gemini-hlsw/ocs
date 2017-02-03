package edu.gemini.qv.plugin.util

import java.time.{Instant, LocalDateTime, LocalDate, Month, ZoneId}

import edu.gemini.spModel.core.{Semester, Site}
import edu.gemini.util.skycalc.calc.Interval
import org.specs2.mutable.Specification

class SemesterDataSpec extends Specification {
  val UTC = ZoneId.of("UTC")//Pacific/Honolulu")
  val HST = ZoneId.of("Pacific/Honolulu")

  "SemesterData" should {
    "work on the last day of the semester" in {

      // Some day of the semester
      val start = LocalDate.of(2016, Month.DECEMBER, 31).atStartOfDay(UTC).toInstant.toEpochMilli
      // Last day of the semester
      val end = LocalDate.of(2017, Month.JANUARY, 31).atStartOfDay(UTC).toInstant.toEpochMilli

      // Border of sunrise on the last day
      val beforeSunrise = LocalDateTime.of(2017, Month.JANUARY, 31, 6, 0).atZone(HST).toInstant.toEpochMilli
      val afterSunrise = LocalDateTime.of(2017, Month.JANUARY, 31, 7, 0).atZone(HST).toInstant.toEpochMilli

      // After sunrise on the day before
      val afterSunrisePreviousDay = LocalDateTime.of(2017, Month.JANUARY, 30, 7, 0).atZone(HST).toInstant.toEpochMilli

      val interval = Interval(start, end)
      // Prime the database
      SemesterData.update(Site.GN, interval)
      // 2016B should be found
      val semester = SemesterData.current(Site.GN, end)
      semester should beEqualTo(SemesterData(Site.GN, Semester.parse("2016B")))
      SolutionProvider.currentNight(Site.GN, beforeSunrise) should beSome
      SolutionProvider.currentNight(Site.GN, afterSunrisePreviousDay) should beSome
      // With REL-3069 this returns None
      SolutionProvider.currentNight(Site.GN, afterSunrise) should beSome
    }
  }
}
