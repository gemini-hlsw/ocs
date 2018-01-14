package edu.gemini.util.skycalc.calc

import edu.gemini.skycalc.{ImprovedSkyCalc, MoonCalc}
import java.util.Date

import edu.gemini.spModel.core.Site
import jsky.coords.WorldCoords
import edu.gemini.util.skycalc.calc.MoonCalculator.Fields
import javax.swing.Icon
import java.awt.geom.Arc2D
import java.awt.{Color, Component, Graphics, Graphics2D}

import scala.concurrent.duration._

/**
 * Support for a variety of calculations regarding the moon.
 */
trait MoonCalculator extends Calculator {
  require(site == Site.GN || site == Site.GS)

  val site: Site
  val values: Vector[Vector[Double]] = calculate()

  import Fields._

  lazy val elevation: Double = valueAt(Elevation.id, start)
  lazy val phaseAngle: Double = valueAt(PhaseAngle.id, start)
  lazy val illuminatedFraction: Double = valueAt(IlluminatedFraction.id, start)

  def elevationAt(t: Long): Double = valueAt(Elevation.id, t)
  lazy val minElevation: Double = min(Elevation.id)
  lazy val maxElevation: Double = max(Elevation.id)
  lazy val meanElevation: Double = mean(Elevation.id)

  def phaseAngleAt(t: Long): Double = valueAt(PhaseAngle.id, t)
  lazy val minPhaseAngle: Double = min(PhaseAngle.id)
  lazy val maxPhaseAngle: Double = max(PhaseAngle.id)
  lazy val meanPhaseAngle: Double = mean(PhaseAngle.id)

  def illuminatedFractionAt(t: Long): Double = valueAt(IlluminatedFraction.id, t)
  lazy val minIlluminatedFraction: Double = min(IlluminatedFraction.id)
  lazy val maxIlluminatedFraction: Double = max(IlluminatedFraction.id)
  lazy val meanIlluminatedFraction: Double = mean(IlluminatedFraction.id)

  lazy val newMoons: Seq[Long] = MoonCalculator.calculatePhases(site, Interval(start, end), MoonCalc.Phase.NEW)
  lazy val firstQuarterMoons: Seq[Long] = MoonCalculator.calculatePhases(site, Interval(start, end), MoonCalc.Phase.FIRST_QUARTER)
  lazy val fullMoons: Seq[Long] = MoonCalculator.calculatePhases(site, Interval(start, end), MoonCalc.Phase.FULL)
  lazy val lastQuarterMoons: Seq[Long] = MoonCalculator.calculatePhases(site, Interval(start, end), MoonCalc.Phase.LAST_QUARTER)

  protected def calculate() = {
    val skycalc = new ImprovedSkyCalc(site)
    val dummy = new WorldCoords(0, 0)

    // prepare temporary data structure
    val values = Array (
      new Array[Double](samples),
      new Array[Double](samples),
      new Array[Double](samples),
      new Array[Double](samples),
      new Array[Double](samples),
      new Array[Double](samples),
      new Array[Double](samples)
    )
    // fill temporary data structure with calculated values
    for (ix <- 0 to samples-1) {
      val t = times(ix)
      skycalc.calculate(dummy, new Date(t), true)
      values(Elevation.id)(ix) = skycalc.getLunarElevation
      values(PhaseAngle.id)(ix) = skycalc.getLunarPhaseAngle
      values(IlluminatedFraction.id)(ix) = skycalc.getLunarIlluminatedFraction
      values(SkyBrightness.id)(ix) = if (skycalc.getLunarSkyBrightness == null) 0.0 else skycalc.getLunarSkyBrightness.toDouble
    }

    // turn into immutable data structure
    Vector(
      // IMPORTANT: Make sure the order reflects the id values of the field enums!
      Vector(values(Elevation.id):_*),
      Vector(values(PhaseAngle.id):_*),
      Vector(values(IlluminatedFraction.id):_*),
      Vector(values(SkyBrightness.id):_*)
    )
  }
}

case class IntervalMoonCalculator(site: Site, defined: Interval, rate: Long) extends FixedRateCalculator with LinearInterpolatingCalculator with MoonCalculator

case class SampleMoonCalculator(site: Site, times: Vector[Long]) extends IrregularIntervalCalculator with LinearInterpolatingCalculator with MoonCalculator

case class SingleValueMoonCalculator(site: Site, time: Long) extends SingleValueCalculator with MoonCalculator


object MoonCalculator {

  /** Enumeration that defines the different fields for this calculator for indexed access in sequence. */
  object Fields extends Enumeration {
    type Field = Value
    val Elevation, PhaseAngle, IlluminatedFraction, SkyBrightness = Value
  }

  def apply(site: Site, defined: Interval, rate: Long = 30.seconds.toMillis): MoonCalculator = {
    new IntervalMoonCalculator(site, defined, rate)
  }
  def apply(site: Site, time: Long): MoonCalculator = {
    new SingleValueMoonCalculator(site, time)
  }
  def apply(site: Site, times: Vector[Long]): MoonCalculator = {
    new SampleMoonCalculator(site, times)
  }

  def newMoons(site: Site, interval: Interval): Seq[Long] = calculatePhases(site, interval, MoonCalc.Phase.NEW)
  def firstQuarterMoons(site: Site, interval: Interval): Seq[Long] = calculatePhases(site, interval, MoonCalc.Phase.FIRST_QUARTER)
  def fullMoons(site: Site, interval: Interval): Seq[Long] = calculatePhases(site, interval, MoonCalc.Phase.FULL)
  def lastQuarterMoons(site: Site, interval: Interval): Seq[Long] = calculatePhases(site, interval, MoonCalc.Phase.LAST_QUARTER)

  /** Calculates times of the given moon phase for the given interval. */
  protected def calculatePhases(site: Site, interval: Interval, phaseConstant: MoonCalc.Phase): Seq[Long] = {
    def sample(period: Int, res: Seq[Long], t: Long): Seq[Long] = {
      if (t > interval.end) res
      else {
        val time = MoonCalc.getMoonTime(period, phaseConstant)
        val r = if (interval.contains(time)) res :+ time else res
        sample(period+1, r, time)
      }
    }
    val period = MoonCalc.approximatePeriod(interval.start)
    sample(period, Seq(), interval.start)
  }

}

class MoonIcon(size: Int, illum: Double, waxing: Boolean) extends Icon {

  val baseAngle = if (waxing) 90 else 270


  def getIconHeight: Int = size

  def getIconWidth: Int = size

  def paintIcon(c: Component, g: Graphics, x: Int, y: Int): Unit = {
      val g2d = g.asInstanceOf[Graphics2D]

      // Dark side
      val darkSize = halfMoon(x, y, getIconWidth() - 1, getIconHeight() - 1, 0)
      g2d.setColor(Color.BLACK)
      g2d.fill(darkSize)

      // Light side
      val brightSide = halfMoon(x, y, getIconWidth() - 1, getIconHeight() - 1, 180)
      g2d.setColor(Color.WHITE)
      g2d.fill(brightSide)

      // Additional shadow or light
      if (illum < 0.5) {
        val width = (0.5 - illum) * getIconWidth().toDouble
        val shadow = halfMoon(x + size / 2 - width, y, width * 2 - 1, size - 1, 180)
        g2d.setColor(Color.BLACK)
        g2d.fill(shadow)
      } else if (illum > 0.5) {
        val width = (illum - 0.5) * getIconWidth().toDouble
        val light = halfMoon(x + size / 2 - width, y, width * 2 - 1, size - 1, 0)
        g2d.setColor(Color.WHITE)
        g2d.fill(light)
      }

      // Gray outline
      g2d.setColor(Color.GRAY)
      val circle = new Arc2D.Double(x, y, size - 1, size - 1, 0, 360, Arc2D.OPEN)
      g2d.draw(circle)
    }

    private def halfMoon(x: Double, y: Double, width: Double, height: Double, angle: Int, degrees: Int): Arc2D.Double =
      new Arc2D.Double(x, y, width, height, baseAngle + angle, degrees, Arc2D.OPEN)

    private def halfMoon(x: Double, y: Double, width: Double, height: Double, angle: Int): Arc2D.Double =
      halfMoon(x, y, width, height, angle, 180)

}
