package edu.gemini.qv.plugin.util

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.qv.plugin.QvTool
import edu.gemini.qv.plugin.ui.QvGui
import edu.gemini.qv.plugin.util.ScheduleCache._
import edu.gemini.qv.plugin.util.SolutionProvider.ConstraintType
import edu.gemini.services.client.{TelescopeSchedule, TelescopeScheduleClient}
import edu.gemini.spModel.core.ProgramType.Classical
import edu.gemini.spModel.core.{ProgramId, ProgramType, Peer}
import edu.gemini.util.skycalc.Night
import edu.gemini.util.skycalc.calc.Interval
import edu.gemini.util.skycalc.calc.Solution
import java.net.URI
import java.util.logging.{Level, Logger}
import scala.collection.concurrent
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing.event.Event
import scala.swing.{Publisher, Swing}
import scala.util.Failure
import scala.util.Success


/**
 * Definition of a set of schedule related constraints that are relevant to QV.
 */
object ScheduleCache {

  val Log = Logger.getLogger(classOf[ScheduleCache].getName)

  sealed trait ScheduleEvent extends Event
  object ScheduleLoadStart extends ScheduleEvent
  object ScheduleLoadEnd extends ScheduleEvent
  object ScheduleLoadFailed extends ScheduleEvent

  sealed abstract class ScheduleConstraint(val label: String) extends ConstraintType
  object InstrumentConstraint extends ScheduleConstraint("Instrument")
  object FastTurnaroundConstraint extends ScheduleConstraint("Fast Turnaround")
  object ProgramConstraint extends ScheduleConstraint("Program")
  object LaserConstraint extends ScheduleConstraint("Laser")
  object ShutdownConstraint extends ScheduleConstraint("Shutdown")
  object WeatherConstraint extends ScheduleConstraint("Weather")
  object EngineeringConstraint extends ScheduleConstraint("Engineering")

  val Constraints = Set(
    InstrumentConstraint, FastTurnaroundConstraint, ProgramConstraint,
    LaserConstraint, ShutdownConstraint, WeatherConstraint, EngineeringConstraint
  )

}

/**
 *
 */
class ScheduleCache extends Publisher {

  sealed trait ScheduleKey
  case class InstrumentKey(instrument: SPComponentType) extends ScheduleKey
  case object FastTurnaroundKey extends ScheduleKey
  case class ProgramKey(programId: ProgramId) extends ScheduleKey
  case object LaserKey extends ScheduleKey
  case object ShutdownKey extends ScheduleKey
  case object WeatherKey extends ScheduleKey
  case object EngineeringKey extends ScheduleKey

  private var cachedSchedule: TelescopeSchedule = TelescopeSchedule.empty
  private var cachedScheduleUrl = new URI("http://www.gemini.edu")
  private var nonExclusiveProgramNights: Solution = Solution.Always
  private val scheduleMap: concurrent.Map[ScheduleKey, Solution] = concurrent.TrieMap()


  def schedule = cachedSchedule
  def scheduleUrl = cachedScheduleUrl

  def solution(nights: Seq[Night], constraints: Set[ConstraintType], observation: Obs): Solution =
    constraints.
      filter({
        case c: ScheduleConstraint => true
        case _ => false
      }).
      map(c => solution(c, observation)).
      reduceOption(_ intersect _).
      getOrElse(Solution.Always)

  private def solution(constraint: ConstraintType, observation: Obs): Solution = constraint match {
    case InstrumentConstraint =>
      val key = InstrumentKey(observation.getInstrumentComponentType)
      scheduleMap.getOrElse(key, Solution.Always)
    case FastTurnaroundConstraint =>
      observation.getProg.getType match {
        case Some(ProgramType.FastTurnaround) => scheduleMap.getOrElse(FastTurnaroundKey, Solution.Always)
        case _ => Solution.Always
      }
    case ProgramConstraint =>
      val programId = ProgramId.parse(observation.getProg.getProgramId.stringValue)
      observation.getProg.getType match {
        case Some(ProgramType.Classical) => scheduleMap.getOrElse(ProgramKey(programId), Solution.Never)
        case _ => nonExclusiveProgramNights
      }
    case LaserConstraint =>
      if (observation.getLGS)
        scheduleMap.getOrElse(LaserKey, Solution.Always)
      else
        Solution.Always
    case ShutdownConstraint =>
      scheduleMap.getOrElse(ShutdownKey, Solution.Always)
    case WeatherConstraint =>
      scheduleMap.getOrElse(WeatherKey, Solution.Always)
    case EngineeringConstraint =>
      scheduleMap.getOrElse(EngineeringKey, Solution.Always)
  }

  def clear() = scheduleMap.clear()

  /**
   * Loads the schedule from the telescope schedule service.
   * @param peer
   * @param range
   */
  def update(peer: Peer, range: Interval): Unit = {

    Swing.onEDT(publish(ScheduleLoadStart))

    // update schedule url (if we could get it, more error handling down below)
    TelescopeScheduleClient.getScheduleUrl(QvTool.authClient, peer) onSuccess {
      case scheduleUrl => cacheScheduleUrl(scheduleUrl)
    }

    // update the actual schedule, do a bit more elaborate error handling for this one
    TelescopeScheduleClient.getSchedule(QvTool.authClient, peer, range) onComplete {
      // deal with success / failure cases
      case Success(schedule) =>
        cacheSchedule(schedule, range)
        Swing.onEDT(publish(ScheduleLoadEnd))

      case Failure(t) =>
        cacheSchedule(TelescopeSchedule.empty, range)
        Swing.onEDT(publish(ScheduleLoadFailed))
        Log.log(Level.WARNING, "Could not load telescope schedule.", t)
        QvGui.showError(
          "Could not load telescope schedule",
          """Loading the telescope schedule failed.
            |External constraints will not be available.
          """.stripMargin,
          t)
    }
  }

  /**
   * Add the solutions defined in the given schedule to the cache.
   * This will replace (update) all already existing solutions.
   * @param schedule
   * @return
   */
  def cacheSchedule(schedule: TelescopeSchedule, range: Interval) = {
    schedule.instrumentSchedules.map(s =>
      scheduleMap.put(new InstrumentKey(s.instrument), Solution.Always.reduce(s.intervals))
    )
    scheduleMap.put(FastTurnaroundKey, Solution(schedule.fastTurnaroundSchedule.intervals))
    schedule.programSchedules.map(s =>
      scheduleMap.put(new ProgramKey(s.id), Solution(s.intervals))
    )
    scheduleMap.put(LaserKey, Solution(schedule.laserSchedule.intervals))

    // for shutdown, weather and engineering the solution to the constraints are all dates inside given
    // range for which we don't have a shutdown, weather or engineering interval scheduled planned
    scheduleMap.put(ShutdownKey, Solution(Interval.reduce(range, schedule.shutdownSchedule.intervals)))
    scheduleMap.put(WeatherKey, Solution(Interval.reduce(range, schedule.weatherSchedule.intervals)))
    scheduleMap.put(EngineeringKey, Solution(Interval.reduce(range, schedule.engineeringSchedule.intervals)))

    // calculate and cache the intervals for all nights which are exclusively assigned to a specific program
    // currently we only take classical programs into account for this, this might change
    val exclusiveNights = schedule.programSchedules.filter(_.id.ptype == Some(Classical)) // filter for classical nights
    nonExclusiveProgramNights = Solution.Always.reduce(
      exclusiveNights.size match {
        case 0 => Solution.Never
        case 1 => Solution(exclusiveNights.head.intervals)
        case _ => Solution(exclusiveNights.map(s => s.intervals).reduce(Interval.combine))
      }
    )

    cachedSchedule = schedule
  }
  
  private def cacheScheduleUrl(scheduleUrl: URI) = cachedScheduleUrl = scheduleUrl


}
