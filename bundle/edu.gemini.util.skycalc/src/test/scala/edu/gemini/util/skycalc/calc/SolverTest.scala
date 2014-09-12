package edu.gemini.util.skycalc.calc

import org.junit.Test
import junit.framework.Assert._
import edu.gemini.util.skycalc.constraint.Constraint

/**
 * Test cases for the solver class.
 */
class SolverTest {

  @Test
  def checkDefaultSolver(): Unit = {

    def f(t: Long) =
      if (t < 150) true
      else if (t >= 250 && t < 450) true
      else false

    val c = new TestConstraint(DefaultSolver(1), f)
    assertEquals(Solution(Seq(Interval(0,150))), c.solve(new Interval(0, 200), 0))
    assertEquals(Solution(Seq(Interval(250,400))), c.solve(new Interval(200, 400), 0))
    assertEquals(Solution(Seq(Interval(250,450))), c.solve(new Interval(200, 500), 0))
    assertEquals(Solution(Seq(Interval(0,150),Interval(250,400))), c.solve(new Interval(0, 400), 0))

  }

  @Test
  def checkParabolaSolver(): Unit = {

    def f(t: Long) =
      if (t < 150) true
      else if (t >= 250 && t < 450) true
      else false

    val c = new TestConstraint(ParabolaSolver(1), f)
    assertEquals(Solution(Seq(Interval(0,150))), c.solve(new Interval(0, 200), 0))
    assertEquals(Solution(Seq(Interval(250,400))), c.solve(new Interval(200, 400), 0))
    assertEquals(Solution(Seq(Interval(250,450))), c.solve(new Interval(200, 500), 0))
    assertEquals(Solution(Seq(Interval(0,150),Interval(250,400))), c.solve(new Interval(0, 400), 0))

  }

  @Test
  def checkParabolaSolver2(): Unit = {

    def f(t: Long) =
      if (t >= 5000 && t < 6000) true
      else false

    val c = new TestConstraint(ParabolaSolver(1), f)
    assertEquals(Solution(Seq(Interval(5000,6000))), c.solve(new Interval(0, 10000), 0))

  }

  case class TestConstraint(solver: Solver[Long], f: Long => Boolean) extends Constraint[Long] {
    def metAt(t: Long, dummy: Long) = f(t)
  }

}
