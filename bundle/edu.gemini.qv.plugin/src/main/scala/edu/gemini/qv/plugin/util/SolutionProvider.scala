package edu.gemini.qv.plugin.util

import java.net.URI

import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.qv.plugin.QvContext
import edu.gemini.qv.plugin.ui.QvGui
import edu.gemini.qv.plugin.util.ConstraintsCache._
import edu.gemini.qv.plugin.util.ScheduleCache.ScheduleEvent
import edu.gemini.qv.plugin.util.SolutionProvider.{ConstraintType, ValueType}
import edu.gemini.services.client.TelescopeSchedule
import edu.gemini.skycalc.TimeUtils
import edu.gemini.spModel.core.{Peer, Site}
import edu.gemini.util.skycalc.Night
import edu.gemini.util.skycalc.calc._

import scala.concurrent.Future
import scala.swing.Publisher

object SolutionProvider {

  trait ConstraintType
  trait ValueType

  private val providers: Map[Site, SolutionProvider] = Map(
    Site.GN -> new SolutionProvider(Site.GN),
    Site.GS -> new SolutionProvider(Site.GS)
  )

  def currentNight(site: Site, currentTime: Long): Option[Night] =
    SemesterData.current(site, currentTime).nights.find(n => n.dayEnd > currentTime)

  def apply(site: Site): SolutionProvider = providers(site)
  def apply(peer: Peer): SolutionProvider = providers(peer.site)
  def apply(ctx: QvContext): SolutionProvider = providers(ctx.site)
}


sealed class SolutionProvider(site: Site) extends Publisher {

  // ====================================================================
  // the default start of our range is today and it goes on for a year
  val range: Interval = {
    val start = TimeUtils.startOfDay(System.currentTimeMillis(), site.timezone)
    val end = TimeUtils.endOfDay(start + TimeUtils.days(364), site.timezone)
    // update semester data accordingly
    SemesterData.update(site, Interval(start, end))
    Interval(start, end)
  }

  val nights: Seq[Night] = SemesterData.nights(site, range)
  // ====================================================================

  val scheduleCache = new ScheduleCache()
  val constraintsCache = new ConstraintsCache(nights)

  deafTo(this) // avoid cycles
  listenTo(scheduleCache, constraintsCache)
  reactions += {
    case e: CalculationEvent => publish(e) // forward
    case e: ScheduleEvent => publish(e) // forward
  }

  def clear(): Unit = {
    scheduleCache.clear()
    constraintsCache.clear()
  }

  /** Reloads and recalculates all constraints in the background. */
  def update(ctx: QvContext, newObservations: Set[Obs], oldObservations: Set[Obs]): Future[Unit] = {
    scheduleCache.update(ctx.peer, range)
    constraintsCache.update(ctx.peer, nights, newObservations)
  }

  def values(nights: Seq[Night], valueType: ValueType, obs: Obs): Seq[Double] =
    constraintsCache.value(nights, valueType, obs)

  def value(valueType: ValueType, night: Night, obs: Obs): Double =
    constraintsCache.value(Seq(night), valueType, obs).head

  def remainingHours(ctx: QvContext, o: Obs, currentTime: Long = System.currentTimeMillis()): Option[Long] = {
    SolutionProvider.currentNight(ctx.site, currentTime).map { n =>
      val s = solution(Seq(n), Set[ConstraintType](Elevation), o).restrictTo(n.interval)
      val set = s.intervals.find(_.end < n.nauticalTwilightEnd).map(_.end).getOrElse(n.nauticalTwilightEnd)
      set - n.nauticalTwilightStart
    }
  }

  def remainingNights(ctx: QvContext, obs: Obs, thisSemester: Boolean, nextSemester: Boolean): Int = {
    val now = System.currentTimeMillis()
    val nights = remainingNights(ctx.site, thisSemester, nextSemester)
    if (nights.nonEmpty) {
      solution(nights, ctx.selectedConstraints, obs).intervals.
        filter(i => i.end > now).
        map(i => TimeUtils.startOfDay(i.start, ctx.site.timezone())).
        toSet.size
    } else 0
  }

  def remainingTime(ctx: QvContext, obs: Obs, thisSemester: Boolean, nextSemester: Boolean): Long = {
    val now = System.currentTimeMillis()
    val nights = remainingNights(ctx.site, thisSemester, nextSemester)
    if (nights.nonEmpty) {
      solution(nights, ctx.selectedConstraints, obs).intervals.
        filter(i => i.end > now).
        map(_.duration).
        sum
    } else 0
  }

  private def remainingNights(site: Site, thisSemester: Boolean, nextSemester: Boolean): Seq[Night] = {
    val now = System.currentTimeMillis()
    val ts = if (thisSemester) SemesterData.current(site).nights.filter(n => n.sunrise > now) else Seq()
    val ns = if (nextSemester) SemesterData.next(site).nights else Seq()
    ts ++ ns
  }

  def telescopeSchedule: TelescopeSchedule = scheduleCache.schedule
  def telescopeScheduleUrl: URI = scheduleCache.scheduleUrl

  def solution(nights: Seq[Night], constraints: Set[ConstraintType], observations: Set[Obs]): Solution = {
    val solutions: Set[Solution] = observations.map(o => solution(nights, constraints, o))
    // reduce all solutions to a single solution by combining all of them
    if (solutions.nonEmpty) solutions.reduce(_ combine _) else Solution()
  }

  // calculate the solution for one single observation applying all constraints
  // if needed minimal time constraint is applied, making sure that observation is available for at least
  // the specified amount of time between nautical twilight bounds
  def solution(nights: Seq[Night], constraints: Set[ConstraintType], observation: Obs): Solution = {

    // combine all cached solutions
    val cachedConstraints = constraints - MinimumTime  // MinTime is not cached!
    val solution = Seq(
      constraintsCache.solution(nights, cachedConstraints, observation),
      scheduleCache.solution(nights, constraints, observation)
    ).reduce(_ intersect _)

    // if needed apply minimum time constraint for every single night
    if (constraints.contains(MinimumTime)) {
      val minTime = minDurationFor(nights.head, observation)
      Solution(solution.intervals.filter(i => i.duration >= minTime))
    }
    else solution

  }

  // Note: See note for minElevationFor()
  private def minDurationFor(n: Night, o: Obs) =
    if (o.getLGS && n.site == Site.GS) TimeUtils.minutes(60)  // minimal science time for GeMS (LGS + site = GS): 60 minutes
    else if (o.getLGS) TimeUtils.minutes(30)                  // minimal science time for Altair + LGS: 30 minutes
    else TimeUtils.minutes(30)                                // minimal science time for everything else: 30 minutes

}



