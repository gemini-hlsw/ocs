package edu.gemini.qv.plugin.util

import edu.gemini.qv.plugin.filter.core.Filter.RA
import edu.gemini.spModel.core.Semester
import edu.gemini.spModel.core.Site
import edu.gemini.util.skycalc.Night
import edu.gemini.util.skycalc.calc.Interval

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * SemesterData objects store fundamental semester data about all nights in a semester like sun and moon set
 * and rise times, dark time intervals and durations etc.
 * Calculation of all data for a semester can be time consuming, therefore it is done asynchronously as soon
 * as a semester is added to the cache in the hope that by the time the data is loaded and a graph is actually
 * painted the data is already available (if not, the get call will block until calculations are finished)
 */
case class SemesterData(site: Site, semester: Semester) {
  require(site == Site.GN || site == Site.GS)

  val start: Long = semester.getStartDate(site).getTime
  val end: Long = semester.getEndDate(site).getTime
  val interval = Interval(start, end)

  /** Calculate and store all nights with their relevant data for this semester. */
  val nights: Seq[Night] = {
    val start = semester.getStartDate(site).getTime
    val end   = semester.getEndDate(site).getTime
    createNights(site, start, end)
  }

  private def createNights(site: Site, cur: Long, end: Long): Seq[Night] =
    if (cur == end || cur > end) Seq()
    else Seq(new Night(site, cur)) ++: createNights(site, cur + 1.day.toMillis, end)

}

/**
 * Simple cache for semester data.
 */
object SemesterData {

  def lst(site: Site, range: Interval, ra: RA): Option[Long] = {
    val nights = nightsForRa(site, range, ra)
    if (nights.nonEmpty) Some(nights.head.lstMiddleNightTime)
    else None
  }

  def scienceTime(site: Site, range: Interval, ra: RA): Long = {
    val nights = nightsForRa(site, range, ra)
    val duration = nights.map(_.scienceTime.duration).sum
    (duration/nights.size * 365.0/(24.0/(ra.max - ra.min))).toInt
  }

  def darkTime(site: Site, range: Interval, ra: RA): Long = {
    val nights = nightsForRa(site, range, ra)
    val duration = nights.
      map(_.darkScienceTime).                 // get dark science time for every night
      filter(_ >= 30.minutes.toMillis).       // only register if there are more than 30 minutes available
      sum                                     // (anything less is too short for running an observation)
    (duration/nights.size * 365.0/(24.0/(ra.max - ra.min))).toInt
  }

  private def nightsForRa(site: Site, range: Interval, ra: RA) =
    nights(site, range).filter(n => n.lst >= ra.min && n.lst < ra.max)

  def nights(site: Site, range: Interval): Seq[Night] =
    nights(site, range.start, range.end)

  /**
   * Gets all available nights between the given start and end time.
   */
  def nights(site: Site, start: Long, end: Long): Seq[Night] =
    semesters(site, start, end).flatMap(_.nights).
      filter(n => n.sunrise > start && n.sunset < end)

  def semester(night: Night): SemesterData =
    semesters(Seq(night)).head

  def semesters(nights: Seq[Night]): Seq[SemesterData] =
    semesters(nights.head.site, nights.head.sunset, nights.last.sunrise)

  def update(site: Site, range: Interval): Unit = {
    // figure out which semesters we need to cover the given range, this will always return at least one semester
    @tailrec def semesters(cur: Semester, res: Seq[Semester]): Seq[Semester] =
      if (cur.getStartDate(site).getTime >= range.end) res
      else semesters(cur.next(), res :+ cur)

    // get semesters we need to cover and add those semesters to the cache
    val coverage = semesters(new Semester(site, range.start), Seq())
    coverage.foreach(SemesterData.add(site, _))
  }


  /**
   * Gets all available semesters covering the given time frame.
   * @return
   */
  def semesters(site: Site, start: Long, end: Long): Seq[SemesterData] =
    semesters(site).
      filter(sd => sd.end > start && sd.start < end)

  private def add(site: Site, semester: Semester) = Cache.add(site, semester)

  def get(site: Site, semester: Semester): SemesterData = Cache.get(site, semester)

  def semesters(site: Site): Seq[SemesterData] = Cache.semesters(site)

  def current(site: Site, currentTime: Long = System.currentTimeMillis()): SemesterData = semesters(site, currentTime, currentTime).head

  def next(site: Site): SemesterData = semesters(site, System.currentTimeMillis(), System.currentTimeMillis() + 185.days.toMillis)(1)

  /**
   * The actual cache.
   * It deals with synchronization issues and makes clients wait for results if they are not ready yet.
   */
  private object Cache {
    private var semestersMap: Map[(Site, Semester), Future[SemesterData]] = Map()

    /**
     * Adds a semester to the cache.
     * Does nothing if the semester is already available.
     */
    def add(site: Site, semester: Semester): Unit = synchronized {
      if (!semestersMap.contains((site, semester))) {
        val semesterData = Future { new SemesterData(site, semester) }
        semestersMap += (site, semester) -> semesterData
      }
    }

    /**
     * Gets the data for the given semester.
     * This will block for at most 90 seconds if the semester data is not available yet (assumption is that
     * the semester data calculation never fails and does not take longer than 90 seconds, if we don't have
     * the semester data available we can't do much anyways, so we can just as well throw a TimeoutException).
     */
    def get(site: Site, semester: Semester): SemesterData =  {
      Await.result(semestersMap((site, semester)), 90.seconds)
    }

    /**
     * Gets a list of all semesters that are (will be) available.
     * This also blocks and waits for unfinished calculations by calling
     * @return
     */
    def semesters(site: Site): Seq[SemesterData] = semestersMap.keys.filter(_._1 == site).map(_._2).toSeq.sorted.map(get(site, _))

  }

}
