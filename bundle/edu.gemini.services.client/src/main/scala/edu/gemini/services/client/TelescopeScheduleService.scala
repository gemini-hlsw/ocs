package edu.gemini.services.client

import Calendar._
import edu.gemini.services.client.TelescopeSchedule.Constraint
import edu.gemini.util.skycalc.calc.Interval
import java.net.URI

object Calendar {

  trait Entry {
    def summary: String
    def interval: Interval
  }

  case class Event(summary: String, interval: Interval) extends Entry
  case class AllDayEvent(summary: String, interval: Interval) extends Entry

}

// ==== Telescope Schedule Service interface

trait TelescopeScheduleService {
  def getSchedule(range: Interval): TelescopeSchedule
  def getScheduleUrl: URI
  def addConstraint(constraint: Constraint): Unit
  def deleteConstraint(constraint: Constraint): Unit
}

// ==== Calendar service interface

trait Calendar {
  def events(range: Interval, query: Option[String] = None): Seq[Entry]
  def addEvent(event: Entry): Unit
  def deleteEvent(event: Entry): Unit
}

trait CalendarService {
  def calendar(id: String): Calendar
}
