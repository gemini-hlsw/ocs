package edu.gemini.services.server.util

import java.net.URI

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.services.client.Calendar.{AllDayEvent, Entry}
import edu.gemini.services.client.TelescopeSchedule.{LaserConstraint, LaserSchedule, ShutdownSchedule, _}
import edu.gemini.services.client._
import edu.gemini.services.server.telescope.LttsService
import edu.gemini.shared.util.DateTimeUtils
import edu.gemini.spModel.core.ProgramId
import edu.gemini.spModel.gemini.inst.InstRegistry
import edu.gemini.util.skycalc.calc.Interval

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Implementation for the telescope schedule service.
 * This service allows to get a schedule with telescope specific events like:
 * <ul>
 *   <li>availability of instruments,</li>
 *   <li>time blocks scheduled for specific programs, above all classical programs,</li>
 *   <li>nights for which laser operations are planned (taken from LTTS),</li>
 *   <li>fast Turnaround (FT) nights,</li>
 *   <li>planned telescope shutdowns,</li>
 *   <li>engineering nights,</li>
 *   <li>and long term Weather events.</li>
 * </ul>
 */
class TelescopeScheduleServiceImpl(calendarService: CalendarService, lttsService: LttsService, calendarId: String, calendarUrl: URI) extends TelescopeScheduleService {

  val calendar = calendarService.calendar(calendarId)

  def getSchedule(range: Interval): TelescopeSchedule = {
    val events = calendar.events(range).map(transformAllDayEvents)
    val ts = for {
      laserSchedule <- laserSchedule(range)

    } yield TelescopeSchedule(
      instrumentSchedules(events, range),
      programSchedules(events),
      laserSchedule,
      shutdownSchedule(events),
      weatherSchedule(events),
      engineeringSchedule(events)
    )

    Await.result(ts, 2 minutes)
  }

  def getScheduleUrl: URI = calendarUrl

  def addConstraint(constraint: Constraint): Unit = {
    val s = constraint.start - DateTimeUtils.StartOfDayHourInMs
    val e = constraint.end - DateTimeUtils.StartOfDayHourInMs
    calendar.addEvent(Calendar.AllDayEvent(labelFor(constraint), Interval(s, e)))
  }

  def deleteConstraint(constraint: Constraint): Unit = {
    val s = constraint.start - DateTimeUtils.StartOfDayHourInMs
    val e = constraint.end - DateTimeUtils.StartOfDayHourInMs
    calendar.deleteEvent(Calendar.AllDayEvent(labelFor(constraint), Interval(s, e)))
  }

  // ====

  private def laserSchedule(range: Interval) = lttsService.getNights(range).map(n => LaserSchedule(n.map(LaserConstraint)))
  private def shutdownSchedule(events: Seq[Entry]) = ShutdownSchedule(filter(events, "Shutdown").map(ShutdownConstraint))
  private def engineeringSchedule(events: Seq[Entry]) = EngineeringSchedule(filter(events, "Engineering").map(EngineeringConstraint))
  private def weatherSchedule(events: Seq[Entry]) = WeatherSchedule(filter(events, "Weather").map(WeatherConstraint))

  private def filter(events: Seq[Entry], s: String): Seq[Interval] =
    events.
      filter(_.summary.startsWith(s)).
      map(_.interval)

  private def programSchedules(events: Seq[Entry]) =
    events.
      filter(_.summary.startsWith("Program")).
      groupBy(getProgramId).map({ case (programId, ev) => {
        ProgramSchedule(programId, ev.map(e => ProgramConstraint(programId, e.interval)))
      }}).toSet

  private val programIdPattern = """Program\s*(.*)\s*""".r("programId")
  private def getProgramId(event: Entry): ProgramId = {
    val programIdPattern(programId) = event.summary // TODO: how to return program id directly?
    ProgramId.parse(programId)
  }

  private def instrumentSchedules(events: Seq[Entry], range: Interval) = {
    InstRegistry.instance.prototypes().map(p => {
      instrumentSchedule(events, p.getType, range)
    }).toSet
  }

  def instrumentSchedule(events: Seq[Entry], instrument: SPComponentType, range: Interval) = {
    val instrEvents =
      events.
      filter(_.summary.startsWith("Instrument " + instrument.readableStr))
    InstrumentSchedule(
      instrument,
      instrEvents.map(e => InstrumentConstraint(instrument, e.interval))
    )
  }

  private def transformAllDayEvents(e: Entry): Entry = e match {
    case AllDayEvent(summary, interval) =>
      val start = interval.start + DateTimeUtils.StartOfDayHourInMs
      val end   = interval.end + DateTimeUtils.StartOfDayHourInMs
      AllDayEvent(summary, Interval(start, end))
    case e => e
  }

  private def labelFor(c: Constraint): String = c match {
    case c: InstrumentConstraint => s"Instrument ${c.instrument.readableStr} - Off"
    case c: ProgramConstraint => s"Program ${c.id}"
    case c: LaserConstraint => "Laser"
    case c: ShutdownConstraint => "Shutdown"
    case c: EngineeringConstraint => "Engineering"
    case c: WeatherConstraint => "Weather"
  }

}
