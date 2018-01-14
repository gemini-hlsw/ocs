package edu.gemini.util.skycalc.constraint

import edu.gemini.util.skycalc.calc._
import edu.gemini.util.skycalc.calc.Interval
import edu.gemini.util.skycalc.Night

import scala.concurrent.duration._

trait Constraint[A] {

  protected val solver: Solver[A] // use Scala solver from util package instead of Java one

  def solve(nights: Seq[Night], param: A): Solution =
    nights.map(n => solve(n, param)).foldLeft(Solution.Never)(_ add _)

  def solve(night: Night, param: A): Solution =
    solve(night.interval, param)

  /** Finds solution for an interval. */
  def solve(interval: Interval, param: A): Solution =
    solver.solve(this, interval, param)

  /** This function defines the actual constraint by returning true or false for a given time <code>t</code>
    * depending on whether to constraint is met or not.
    * @param t
    * @return
    */
  def metAt(t: Long, param: A): Boolean

}

/**
 * Implementation for an elevation constraint that uses the pre-calculated data from a
 * {@see edu.gemini.util.skycalc.calc.TargetCalc} object.
 */
case class ElevationConstraint(min: Double, max: Double, tolerance: Long = 30.seconds.toMillis) extends Constraint[TargetCalculator] {
  protected val solver = DefaultSolver[TargetCalculator](tolerance)
  def metAt(t: Long, target: TargetCalculator): Boolean = {
    val elevation = target.elevationAt(t)
    elevation >= min && elevation <= max
  }
}

case class MoonElevationConstraint(min: Double, max: Double, tolerance: Long = 30.seconds.toMillis) extends Constraint[MoonCalculator] {
  protected val solver = DefaultSolver[MoonCalculator](tolerance)
  def metAt(t: Long, moon: MoonCalculator): Boolean = {
    val elevation = moon.elevationAt(t)
    elevation >= min && elevation <= max
  }
}

case class SkyBrightnessConstraint(min: Double, max: Double, tolerance: Long = 30.seconds.toMillis) extends Constraint[TargetCalculator] {
  protected val solver = DefaultSolver[TargetCalculator](tolerance)
  def metAt(t: Long, target: TargetCalculator): Boolean = {
    val skyBrightness = target.skyBrightnessAt(t)
    skyBrightness >= min && skyBrightness <= max
  }
}

case class AirmassConstraint(min: Double, max: Double, tolerance: Long = 30.seconds.toMillis) extends Constraint[TargetCalculator] {
  protected val solver = DefaultSolver[TargetCalculator](tolerance)
  def metAt(t: Long, target: TargetCalculator): Boolean = {
    val airmass = target.airmassAt(t)
    // NOTE: we need to work around errors with interpolation etc which may cause to give wrong airmass values for very small altitudes (<1deg)
    target.elevationAt(t) >= 5 && airmass >= min && airmass <= max
  }
}

case class HourAngleConstraint(min: Double, max: Double, tolerance: Long = 30.seconds.toMillis) extends Constraint[TargetCalculator] {
  protected val solver = DefaultSolver[TargetCalculator](tolerance)
  def metAt(t: Long, target: TargetCalculator): Boolean = {
    val hourAngle = target.hourAngleAt(t)
    // NOTE: we need to work around errors with interpolation etc which may cause to give wrong hour angle values for very small altitudes (<1deg)
    target.elevationAt(t) >= 5 && hourAngle >= min && hourAngle <= max
  }
}

