package edu.gemini.util.skycalc.calc

import edu.gemini.util.skycalc.constraint.Constraint

import scala.concurrent.duration._

/**
 * Representation of an algorithm that finds all intervals between a start and end point in time for which a given
 * function <code>f(t: Long): Boolean</code> is true.
 */
trait Solver[A] {
  def solve(constraint: Constraint[A], interval: Interval, param: A): Solution
}

/**
 * Solver that finds all intervals for which the underlying constraint is true by sampling the constraint function
 * at a given rate. Intervals shorter than the sampling rate may be missed by this solver. Use this solver
 * for complex functions which can have an arbitrary amount of separate intervals for which the constraint
 * meats its criteria (e.g. Sky Background).
 */
case class DefaultSolver[A](step: Long = 30.seconds.toMillis) extends Solver[A] {
  require(step > 0)

  def solve(constraint: Constraint[A], interval: Interval, param: A): Solution = {

    def solve(curStart: Long, curState: Boolean, t: Long, solution: Solution): Solution = {
      if (t >= interval.end)
        if (curState) solution.add(Interval(curStart, interval.end))
        else solution
      else {
        if (constraint.metAt(t, param) == curState) solve(curStart, curState, t + step, solution)
        else if (curState) solve(t, curState = false, t + step, solution.add(Interval(curStart, t)))
        else solve(t, curState = true, t + step, solution)
      }
    }

    solve(interval.start, constraint.metAt(interval.start, param), interval.start, Solution.Never)

  }


}

/**
 * Finds a solution for a constraint on a parabolic curve that crosses that constraint
 * at most twice during the given interval. This is true for all basic elevation constraints for a single night.
 */
case class ParabolaSolver[A](tolerance: Long = 30.seconds.toMillis) extends Solver[A] {
  require(tolerance > 0)

  def solve(constraint: Constraint[A], interval: Interval, param: A): Solution = {

    def solve(s: Long, fs: Boolean, e: Long, fe: Boolean): Solution = {
      val m = (s + e) / 2
      val fm = constraint.metAt(m, param)
      if (e - s > tolerance) {
        (fs, fm, fe) match {
          case (false, false, false) => solve(s, fs, m, fm).add(solve(m, fm, e, fe))
          case (false, false, true)  => solve(m, fm, e, fe)
          case (false, true,  false) => solve(s, fs, m, fm).add(solve(m, fm, e, fe))
          case (false, true,  true)  => solve(s, fs,  m, fm).add(Solution(m, e))
          case (true,  false, false) => solve(s, fs, m, fm)
          case (true,  false, true)  => solve(s, fs, m, fm).add(solve(m, fm, e, fe))
          case (true,  true,  false) => Solution(s, m).add(solve(m, fm, e, fe))
          case (true,  true,  true)  => solve(s, fs, m, fm).add(solve(m, fm, e, fe))
        }
      } else {
        if (fm) Solution(Interval(s, e))
        else Solution()
      }

    }

    val fs = constraint.metAt(interval.start, param)
    val fe = constraint.metAt(interval.end, param)
    solve(interval.start, fs, interval.end, fe)
  }
}

