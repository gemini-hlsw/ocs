package edu.gemini.util.skycalc.constraint

import edu.gemini.skycalc.TimeUtils
import edu.gemini.spModel.core.{Angle, Coordinates, Declination, RightAscension, Site}
import edu.gemini.util.skycalc.calc.{Interval, Solution, TargetCalculator}
import org.junit.Assert._
import org.junit.{Test}

/**
 * Check elevation constraints for some examples.
 */
class ElevationConstraintsTest {

  @Test def checkHourAngleConstraint(): Unit = {

    val start = TimeUtils.time(2014, 12, 4, 17, 30, Site.GN.timezone())
    val end = TimeUtils.time(2014, 12, 5, 6, 30, Site.GN.timezone())
    val pos = (t: Long) => Coordinates(
      RightAscension.fromAngle(Angle.parseHMS("17:32:10.569").toOption.get),
      Declination.fromAngle(Angle.parseDMS("+55:11:03.27").toOption.get).get
    )
    val range = Interval(start, end)
    val calc = TargetCalculator(Site.GN, pos, range)
    val constraint = HourAngleConstraint(-2, +2)
    val solution = constraint.solve(range, calc)

    assertEquals(Solution.Never, solution)

  }

}
