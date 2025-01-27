package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

import java.util.{Calendar, TimeZone}

object Semester {

  lazy val year = 2025
  lazy val semesterOption: SemesterOption = SemesterOption.B

  lazy val current: Semester = Semester(year, semesterOption)

  def apply(m: M.Semester): Semester = Semester(m.getYear, m.getHalf)

  def forDate(ms: Long): Unit = {

    val cal = Calendar.getInstance
    cal.setTimeInMillis(ms)

    // Semesters go from [Feb .. Jul] and [Aug .. Jan] inclusive.
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)

    month match {
      case Calendar.JANUARY        => Semester(year - 1, SemesterOption.B)
      case m if m <= Calendar.JULY => Semester(year, SemesterOption.A)
      case _                       => Semester(year, SemesterOption.B)
    }

  }

  private val SemesterRegex = """(\d\d\d\d)([A|B])""".r // Extract semester from the backend error message

  def parse(semester: String): Option[Semester] = semester match {
    case SemesterRegex(s, h) => Some(Semester(s.toInt, SemesterOption.forName(h)))
    case _                   => None
  }

  implicit class SemesterOptionExtension(val o:SemesterOption) extends AnyVal {
    def firstMonth: Int = o match {
      case M.SemesterOption.A => Calendar.FEBRUARY
      case M.SemesterOption.B => Calendar.AUGUST
    }
    def lastMonth: Int = o match {
      case M.SemesterOption.A => Calendar.JULY
      case M.SemesterOption.B => Calendar.JANUARY
    }
  }

}

final case class Semester(year: Int, half: SemesterOption) {

  import Semester._

  def mutable: M.Semester = {
    val m = Factory.createSemester
    m.setYear(year)
    m.setHalf(half)
    m
  }

  lazy val firstDay: Long = {
    val cal = Calendar.getInstance
    cal.setTimeZone(TimeZone.getTimeZone("UTC"))
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, half.firstMonth)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.getTimeInMillis
  }

  lazy val lastDay: Long = {
    val cal = Calendar.getInstance
    cal.setTimeZone(TimeZone.getTimeZone("UTC"))
    cal.set(Calendar.YEAR, half match {
      case SemesterOption.A => year
      case SemesterOption.B => year + 1
    })
    cal.set(Calendar.MONTH, half.lastMonth)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.MONTH, 1)
    cal.add(Calendar.DAY_OF_MONTH, -1)
    cal.getTimeInMillis
  }

  lazy val midPoint: Long = (firstDay + lastDay) / 2

  lazy val display: String = s"$year$half"
}
