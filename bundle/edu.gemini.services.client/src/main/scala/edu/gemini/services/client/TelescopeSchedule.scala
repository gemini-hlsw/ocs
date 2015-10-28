package edu.gemini.services.client

import edu.gemini.util.skycalc.calc.Interval
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.core.ProgramId

object TelescopeSchedule {

  sealed trait Constraint {
    def interval: Interval
    def start = interval.start
    def end = interval.end
  }
  case class InstrumentConstraint(instrument: SPComponentType, interval: Interval) extends Constraint
  case class ProgramConstraint(id: ProgramId, interval: Interval) extends Constraint
  case class LaserConstraint(interval: Interval) extends Constraint
  case class ShutdownConstraint(interval: Interval) extends Constraint
  case class WeatherConstraint(interval: Interval) extends Constraint
  case class EngineeringConstraint(interval: Interval) extends Constraint

  sealed trait Schedule {
    def constraints: Seq[Constraint]
    def intervals: Seq[Interval] = constraints.map(_.interval)
  }
  case class InstrumentSchedule(instrument: SPComponentType, constraints: Seq[InstrumentConstraint]) extends Schedule
  case class ProgramSchedule(id: ProgramId, constraints: Seq[ProgramConstraint]) extends Schedule
  case class LaserSchedule(constraints: Seq[LaserConstraint]) extends Schedule
  case class ShutdownSchedule(constraints: Seq[ShutdownConstraint]) extends Schedule
  case class WeatherSchedule(constraints: Seq[WeatherConstraint]) extends Schedule
  case class EngineeringSchedule(constraints: Seq[EngineeringConstraint]) extends Schedule

  lazy val empty = new TelescopeSchedule(
    Set(),
    Set(),
    new LaserSchedule(Seq()),
    new ShutdownSchedule(Seq()),
    new WeatherSchedule(Seq()),
    new EngineeringSchedule(Seq())
  )
}

import TelescopeSchedule._

/**
 * Telescope schedules contain calendar based information about availability of the telescope and the instruments
 * as well as special scheduling constraints like laser, classical and fast turnaround nights.
 */
case class TelescopeSchedule(
  instrumentSchedules: Set[InstrumentSchedule],
  programSchedules: Set[ProgramSchedule],
  laserSchedule: LaserSchedule,
  shutdownSchedule: ShutdownSchedule,
  weatherSchedule: WeatherSchedule,
  engineeringSchedule: EngineeringSchedule
) {

  def instrumentSchedule(instrument: SPComponentType): Option[InstrumentSchedule] =
    instrumentSchedules.find(s => s.instrument == instrument)

  def programSchedule(id: ProgramId): Option[ProgramSchedule] =
    programSchedules.find(s => s.id == id)

}
