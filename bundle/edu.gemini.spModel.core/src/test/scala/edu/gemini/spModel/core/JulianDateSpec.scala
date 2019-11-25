package edu.gemini.spModel.core

import scalaz._
import Scalaz._
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import AlmostEqual.AlmostEqualOps

import java.time.LocalDateTime

final class JulianDateSpec extends Specification with ScalaCheck with Arbitraries with Helpers {

  import ArbTime._
  import ArbJulianDate._

  // Old Epoch.Scheme.toJulianDay algorithm
  private def oldEpochSchemeJulianDate(dt: LocalDateTime): Double = {
    import scala.math.floor

    val a = floor((14.0 - dt.getMonthValue) / 12.0)
    val y = dt.getYear + 4800.0 - a
    val m = dt.getMonthValue + 12 * a - 3.0
    dt.getDayOfMonth +
      floor((153.0 * m + 2.0) / 5.0) +
      365 * y +
      floor(y / 4.0) -
      floor(y / 100.0) +
      floor(y / 400.0) -
      32045.0
  }

  "JulianDate" should {
        "respect Eq" !
          forAll { (a: JulianDate, b: JulianDate) =>
            a.equals(b) shouldEqual Equal[JulianDate].equal(a, b)
          }

    "have dayNumber match old calculation" !
      forAll { (ldt: LocalDateTime) =>
        val jd0 = oldEpochSchemeJulianDate(ldt)
        val jd1 = JulianDate.ofLocalDateTime(ldt).dayNumber.toDouble

        jd0 shouldEqual jd1
      }

    "Some specific dates compared to USNO calculations" ! {
      JulianDate.ofLocalDateTime(LocalDateTime.of(1918, 11, 11, 11, 0, 0)).toDouble ~= 2421908.958333
      JulianDate.ofLocalDateTime(LocalDateTime.of(1969, 7, 21, 2, 56, 15)).toDouble ~=  2440423.622396
      JulianDate.ofLocalDateTime(LocalDateTime.of(2001, 9, 11, 8, 46, 0)).toDouble ~= 2452163.865278
      JulianDate.ofLocalDateTime(LocalDateTime.of(2345, 6, 7, 12, 0, 0)).toDouble ~= 2577711.000000
    }

    "Modified JulianDate should almost equal JulianDate - 2400000.5" !
      forAll { (j: JulianDate) =>
        j.toModifiedDouble ~= (j.toDouble - 2400000.5)
      }
  }
}