package edu.gemini.spModel.core

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

import edu.gemini.shared.util.DateTimeUtils

import scala.util.Try

sealed trait ProgramId {
  def site: Option[Site]
  def semester: Option[Semester]
  def ptype: Option[ProgramType]

  /**
   * Convert to an SPProgramID if possible.  It is only not possible if using
   * a ProgramId.Arbitrary id that was constructed with a character not
   * supported by SPProgramID.
   */
  def spOption: Option[SPProgramID]
}

sealed trait StandardProgramId extends ProgramId {
  def toSp: SPProgramID
  def spOption = Some(toSp)
}

/**
 * A parser for program IDs that tries to decide on the type of program based on known/valid patterns for
 * some specific program types. The parser will also try to get other information that can be encoded in the
 * program name like for example the date for ENG and CAL programs. Note that program ids can be anything
 * (e.g. "PetersTest-1") but usually we're only interested in the programs with well defined names
 * (e.g. "GN-2012A-Q-2", "GN-ENG20131211" etc.).
 */
object ProgramId {
  private val SciencePattern         = """(G[NS])-(\d\d\d\d[AB])-([A-Z]*)-(\d+)""".r
  private val DailyPattern           = """(G[NS])-(CAL|ENG)(\d{4})(\d\d)(\d\d)""".r
  private val SitePattern            = """(G[NS]).*""".r
  private val SemesterPattern        = """.*(\d\d\d\d[AB]).*""".r
  private val ArbitraryCalEngPattern = """G[NS]-(CAL|ENG).*""".r

  case class Science(siteVal: Site, semesterVal: Semester, ptypeVal: ProgramType, index: Int) extends StandardProgramId {
    require(index >= 0)

    def site: Option[Site]         = Some(siteVal)
    def semester: Option[Semester] = Some(semesterVal)
    def ptype: Option[ProgramType] = Some(ptypeVal)

    lazy val toSp: SPProgramID     = SPProgramID.toProgramID(toString)

    override def toString: String =
      s"${siteVal.abbreviation}-${semesterVal.toString}-${ptypeVal.abbreviation}-$index"
  }

  case class Daily(siteVal: Site, ptypeVal: ProgramType, year: Int, month: Int, day: Int) extends StandardProgramId {
    def site: Option[Site]         = Some(siteVal)

    val (start, end) = {
      val zdt = ZonedDateTime.of(year, month, day, DateTimeUtils.StartOfDayHour, 0, 0, 0, siteVal.timezone.toZoneId).truncatedTo(ChronoUnit.SECONDS)
      val e = zdt.toInstant.toEpochMilli
      val s = zdt.minus(1, ChronoUnit.DAYS).toInstant.toEpochMilli
      (s, e)
    }

    def includes(t: Long): Boolean = start <= t && t < end

    val semester: Option[Semester] = Some(new Semester(siteVal, start))
    def ptype: Option[ProgramType] = Some(ptypeVal)

    lazy val toSp: SPProgramID = SPProgramID.toProgramID(toString)

    override def toString: String =
      f"${siteVal.abbreviation}-${ptypeVal.abbreviation}$year%04d$month%02d$day%02d"
  }

  case class Arbitrary(site: Option[Site], semester: Option[Semester], ptype: Option[ProgramType], idString: String) extends ProgramId {
    // Try to get the corresponding SPProgramID which might fail if the idString
    // has an unsupported character.
    val spOption: Option[SPProgramID] = Try { SPProgramID.toProgramID(idString) }.toOption

    override def toString: String = idString
  }

  def parse(s: String): ProgramId =
    s match {
      // e.g. GS-2013A-Q-1
      case SciencePattern(siteStr, semesterStr, typeStr, indexStr) =>
        val site     = Site.parse(siteStr)
        val semester = Semester.parse(semesterStr)
        val index    = indexStr.toInt
        ProgramType.read(typeStr).map { ptype =>
          Science(site, semester, ptype, index)
        }.getOrElse(Arbitrary(Some(site), Some(semester), None, s))

      // e.g GS-ENG20120102
      case DailyPattern(siteStr, typeStr, yearStr, monthStr, dayStr) =>
        val site  = Site.parse(siteStr)
        val year  = yearStr.toInt
        val month = monthStr.toInt
        val day   = dayStr.toInt

        ProgramType.read(typeStr).map { ptype =>
          Daily(site, ptype, year, month, day)
        }.getOrElse(Arbitrary(Some(site), None, None, s))

      // e.g. GS-ENG-Telescope-Setup
      case _ =>
        val site = s match {
          case SitePattern(siteStr) => Some(Site.parse(siteStr))
          case _                  => None
        }
        val semester = s match {
          case SemesterPattern(semStr) => Some(Semester.parse(semStr))
          case _                     => None
        }
        val ptype = s match {
          case ArbitraryCalEngPattern(typeStr) => ProgramType.read(typeStr)
          case _                               => None
        }
        Arbitrary(site, semester, ptype, s)
    }

  def parseStandardId(s: String): Option[StandardProgramId] =
    parse(s) match {
      case s: StandardProgramId => Some(s)
      case _                    => None
    }
}


case class RichSpProgramId(id: SPProgramID) {
  val pid = ProgramId.parse(id.toString)
  def site: Option[Site]         = pid.site
  def semester: Option[Semester] = pid.semester
  def ptype: Option[ProgramType] = pid.ptype
}