package edu.gemini.services.server.util

import java.text.ParseException
import java.time.{Instant, ZonedDateTime}
import java.util.{Date, TimeZone}

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.{EventDateTime, Calendar => GoogleModelCalendar, Event => GoogleEvent}
import edu.gemini.services.client.Calendar
import edu.gemini.services.client.Calendar.{AllDayEvent, Entry, Event}
import edu.gemini.shared.util.{DateTimeFormatters, DateTimeUtils, UTCDateTimeFormatters}
import edu.gemini.spModel.core.Site
import edu.gemini.util.skycalc.calc.Interval

import scala.collection.JavaConversions._

/**
 * Calendar service implementation based on Google calendar web services.
 * Bare-bone, very rudimentary implementation to support requirements for QV telescope schedule.
 */
class GoogleCalendarService(val site: Site) extends edu.gemini.services.client.CalendarService {

  // Get authorization on startup.
  // (Authorization expires after an hour and will have to be renegotiated.
  // The Google libs we are using here deal with all of that for us.)
  val credential = GoogleServices.authorize()
  val client = new com.google.api.services.calendar.Calendar.Builder(GoogleServices.HttpTransport, GoogleServices.JsonFactory, credential).
    setApplicationName("GeminiCalendar/1.0").
    build()

  def calendar(id: String): Calendar = {
    new GoogleCalendar(this, client.calendars().get(id).execute())
  }

}

/**
 * Calendar implementation based on Google calendar.
 * @param service
 * @param calendar
 */
class GoogleCalendar(service: GoogleCalendarService, calendar: GoogleModelCalendar) extends Calendar {
  import GoogleCalendar._

  def events(range: Interval, query: Option[String] = None): Seq[Entry]  = {
    getCalendarEvents(getEvents(query, range))
  }
  
  def addEvent(event: Entry): Unit = event match {
    case AllDayEvent(summary, interval) => addAllDayEvent(summary, interval)
    case Event(summary, interval) => addEvent(summary, interval)
  }
  
  def deleteEvent(event: Entry): Unit = event match {
    case AllDayEvent(summary, interval) => deleteAllDayEvent(summary, interval)
    case Event(summary, interval) => deleteEvent(summary, interval)
  }

  private def deleteAllDayEvent(summary: String, interval: Interval): Unit = {
    val (s,e) = formatStartEndDate(interval.start, interval.end)
    val events = getEvents(Some(summary.replace("-", " ")), interval)
    events.filter(_.getSummary == summary).foreach(ev => {
      if (ev.getStart.getDate.toStringRfc3339 == s && ev.getEnd.getDate.toStringRfc3339 == e)
        service.client.events().delete(calendar.getId, ev.getId).execute()
    })
  }

  private def deleteEvent(summary: String, interval: Interval): Unit = {
    val events = getEvents(Some(summary.replace("-", " ")), interval)
    events.filter(_.getSummary == summary).foreach(ev => {
      service.client.events().delete(calendar.getId, ev.getId).execute()
    })
  }

  private def addAllDayEvent(constraint: String, interval: Interval): Unit = {
    val (s,e) = formatStartEndDate(interval.start, interval.end - DateTimeUtils.StartOfDayHourInMs)
    val event = new GoogleEvent().
      setSummary(constraint).
      setStart(new EventDateTime().setDate(new DateTime(s))).
      setEnd(new EventDateTime().setDate(new DateTime(e)))

    service.client.events().insert(calendar.getId, event).execute()
  }

  private def addEvent(summary: String, interval: Interval): Unit = {
    val s = new DateTime(new Date(interval.start), TimeZone.getTimeZone("UTC"))
    val e = new DateTime(new Date(interval.end),   TimeZone.getTimeZone("UTC"))
    val event = new GoogleEvent().
      setSummary(summary).
      setStart(new EventDateTime().setDateTime(s)).
      setEnd(new EventDateTime().setDateTime(e))

    service.client.events().insert(calendar.getId, event).execute()
  }

  private def getCalendarEvents(events: Seq[GoogleEvent]) =
    events.map(toEvent)

  private def toEvent(e: GoogleEvent) =
    if (isAllDay(e)) AllDayEvent(e.getSummary, toInterval(e))
    else Event(e.getSummary, toInterval(e))

  private def getEvents(q: Option[String], range: Interval): Seq[GoogleEvent] = {
    val request = service.client.events().list(calendar.getId).
      setSingleEvents(true).
      setTimeMin(new DateTime(range.start)).
      setTimeMax(new DateTime(range.end)).
      setOrderBy("startTime")
    if (q.isDefined) {
      request.
      setQ(q.get).
      execute().
      getItems
    } else {
      request.
      execute().
      getItems
    }
  }
      
  /** Checks if this is an all-day event. */
  private def isAllDay(e: GoogleEvent): Boolean = e.getStart.getDateTime == null

  /**
   * Turns an event into an interval.
   * Note that for all-day events we get Date objects instead of DateTime objects and that the timestamp for
   * full-day events is GMT no matter what, which is not very useful. In order to work around this, we use the
   * date string (yyyy-MM-dd) and transform that into the day start/end timestamps using the site's timezone.
   * @param e
   * @return
   */
  private def toInterval(e: GoogleEvent): Interval = {
    if (isAllDay(e)) {
      val st = startOfDay(e.getStart.getDate.toStringRfc3339, service.site.timezone)
      val en = startOfDay(e.getEnd.getDate.toStringRfc3339, service.site.timezone)
      Interval(st, en)
    } else {
      Interval(e.getStart.getDateTime.getValue, e.getEnd.getDateTime.getValue)
    }
  }

  private def startOfDay(dateString: String, timeZone: TimeZone): Long = {
    try {
      val f = DateTimeFormatters(timeZone.toZoneId).YYYY_MM_DD
      ZonedDateTime.parse(dateString, f).toInstant.toEpochMilli
    } catch {
      case t: ParseException => throw new IllegalArgumentException("invalid time format, expected yyyy-MM-dd, received " + dateString, t)
    }
  }

}

object GoogleCalendar {
  def formatStartEndDate(start: Long, end: Long): (String, String) = {
    val df = UTCDateTimeFormatters.YYYY_MM_DD
    (df.format(Instant.ofEpochMilli(start)), df.format(Instant.ofEpochMilli(end)))
  }
}