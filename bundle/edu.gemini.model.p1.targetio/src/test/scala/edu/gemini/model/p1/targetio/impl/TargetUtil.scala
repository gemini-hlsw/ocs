package edu.gemini.model.p1.targetio.impl

import edu.gemini.model.p1.immutable.NonSiderealTarget
import edu.gemini.model.p1.immutable.ProperMotion
import edu.gemini.model.p1.immutable.SiderealTarget
import edu.gemini.model.p1.immutable._
import edu.gemini.model.p1.immutable.CoordinatesEpoch.J_2000

import java.util.Calendar._
import edu.gemini.spModel.core.{Angle, Coordinates, RightAscension, Declination}
import edu.gemini.spModel.core.{Magnitude, MagnitudeBand, MagnitudeSystem}
import org.junit.Assert._
import java.util.{UUID, TimeZone, GregorianCalendar}

object TargetUtil {
  def utc(year: Int, month: Int, day: Int, hour: Int = 0, min: Int = 0): Long = {
    val cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"))
    cal.set(year, month, day, hour, min, 0)
    cal.set(MILLISECOND, 0)
    cal.getTimeInMillis
  }

  val DELTA = 0.00001

  def mkEp(ra: String, dec: String, mag: Double, year: Int, month: Int, day: Int, hour: Int = 0, min: Int = 0): EphemerisElement =
    EphemerisElement(Coordinates(RightAscension.fromAngle(Angle.parseHMS(ra).getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS(dec).getOrElse(Angle.zero)).getOrElse(Declination.zero)), Some(mag), utc(year, month, day, hour, min))

  def mkTarget(name: String, elements: List[EphemerisElement]): NonSiderealTarget =
    NonSiderealTarget(UUID.randomUUID(), name, elements, J_2000, None, None)

  def mkMag(value: Double, band: MagnitudeBand, system: MagnitudeSystem = MagnitudeSystem.default): Magnitude =
    new Magnitude(value, band, system)

  def mkTarget(name: String, ra: String, dec: String): SiderealTarget =
    SiderealTarget(UUID.randomUUID(), name, Coordinates(RightAscension.fromAngle(Angle.parseHMS(ra).getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS(dec).getOrElse(Angle.zero)).getOrElse(Declination.zero)), J_2000, None, Nil)

  def mkTarget(name: String, ra: String, dec: String, pmRa: Double, pmDec: Double): SiderealTarget =
    mkTarget(name, ra, dec).copy(properMotion = Some(ProperMotion(pmRa, pmDec)))

  def ra(coords: Coordinates): Double  = coords.ra.toAngle.toDegrees
  def dec(coords: Coordinates): Double = coords.dec.toDegrees

  def validateCoords(expected: Coordinates, actual: Coordinates) {
    assertEquals(ra(expected), ra(actual), DELTA)
    assertEquals(dec(expected), dec(actual), DELTA)
  }

  def validateElement(expected: EphemerisElement, actual: EphemerisElement) {
    validateCoords(expected.coords, actual.coords)
    (expected.magnitude, actual.magnitude) match {
      case (None, None)         => // ok
      case (Some(ex), Some(ac)) => assertEquals(ex, ac, DELTA)
      case _                    => fail()
    }
    assertEquals(expected.validAt, actual.validAt)
  }

  def validateElements(expected: List[EphemerisElement], actual: List[EphemerisElement]) {
    assertEquals(expected.size, actual.size)
    expected.zip(actual) foreach { case (ex, ac) => validateElement(ex, ac) }
  }

  def validateTarget(expected: NonSiderealTarget, actual: NonSiderealTarget) {
    assertEquals(expected.name, actual.name)
    assertEquals(expected.epoch, actual.epoch)
    validateElements(expected.ephemeris, actual.ephemeris)
  }

  def validateBigDecimal(expected: BigDecimal, actual: BigDecimal) {
    assertEquals(expected.toDouble, actual.toDouble, DELTA)
  }

  def validateProperMotion(expected: Option[ProperMotion], actual: Option[ProperMotion]) {
    (expected, actual) match {
      case (None, None)         =>
      case (Some(ex), Some(ac)) =>
        validateBigDecimal(ex.deltaRA,  ac.deltaRA)
        validateBigDecimal(ex.deltaDec, ac.deltaDec)
      case _                    => fail()
    }
  }

  def validateMagnitude(expected: Magnitude, actual: Magnitude) {
    assertEquals(expected.band, actual.band)
    assertEquals(expected.system, actual.system)
    validateBigDecimal(expected.value, actual.value)
  }

  private def mapMagnitudes(mags: List[Magnitude]): Map[MagnitudeBand, Magnitude] =
    (mags map { mag => mag.band -> mag }).toMap

  def validateMagnitudes(expected: List[Magnitude], actual: List[Magnitude]) {
    val ex = mapMagnitudes(expected)
    val ac = mapMagnitudes(actual)
    assertEquals(ex.keySet, ac.keySet)
    ex foreach {
      case (band, mag) => validateMagnitude(mag, ac(band))
    }
  }

  def validateTarget(expected: SiderealTarget, actual: SiderealTarget) {
    assertEquals(expected.name,  actual.name)
    assertEquals(expected.epoch, actual.epoch)
    validateCoords(expected.coords, actual.coords)
    validateProperMotion(expected.properMotion, actual.properMotion)
    validateMagnitudes(expected.magnitudes, actual.magnitudes)
  }

  def validateTargets(expected: List[NonSiderealTarget], actual: List[NonSiderealTarget]) {
    assertEquals(expected.size, actual.size)
    expected.zip(actual) foreach { case (ex, ac) => validateTarget(ex, ac) }
  }

  // A few ephemeris elements for testing
  val dec30 = mkEp("23:52:00.00", "-11:20:00.0", 9.0, 2011, DECEMBER, 30)
  val dec31 = mkEp("23:52:01.00", "-11:20:01.0", 9.1, 2011, DECEMBER, 31)
  val jan01 = mkEp("23:52:02.00", "-11:20:02.0", 9.2, 2012, JANUARY,   1)

  val aug15 = mkEp("15:00:00.00", "-15:00:00.0", 15.0, 2012, AUGUST, 15)
  val aug16 = mkEp("16:00:00.00", "-16:00:00.0", 16.0, 2012, AUGUST, 16)
  val aug17 = mkEp("17:00:00.00", "-17:00:00.0", 17.0, 2012, AUGUST, 17)
}