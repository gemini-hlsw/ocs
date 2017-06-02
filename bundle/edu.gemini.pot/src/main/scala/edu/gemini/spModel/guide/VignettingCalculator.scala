package edu.gemini.spModel.guide

import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.core.{Declination, Angle, RightAscension, Coordinates}
import edu.gemini.spModel.inst.{ProbeArmGeometry, ScienceAreaGeometry}
import edu.gemini.spModel.inst.FeatureGeometry.approximateArea
import edu.gemini.spModel.obs.context.ObsContext

import java.awt.geom.Area
import java.util.logging.Logger

import scala.annotation.tailrec
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

sealed trait VignettingCalculator {

  private val LOGGER = Logger.getLogger(classOf[VignettingCalculator].getName)

  /** Calculates the percentage of the science area that is obscured by the
    * probe arm when tracking the given guide star candidate.  The return value
    * is therefore always [0, 1] where 0 is no vignetting and 1 is completely
    * blocked.
    *
    * Returns the worst-case vignetting result for any offset at which the probe
    * arm vignettes. */
  def calc(guideStar: Coordinates): Double

  /** Selects the first candidate that least vignettes the science area.
    * If two candidates each vignette by the same amount, the first in the given
    * iterable is selected.
    *
    * No other factors are taken into account (brightness, quality, whether the
    * candidate is reachable at all offsets, etc) so the caller should filter
    * the candidates accordingly before calling. */
  def min[A](candidates: List[A])(f: A => Coordinates): Option[A] =
    minCalc(candidates)(f).map { case (a,_) => a }

  /** Selects the first candidate that least vignettes the science area.
    * If two candidates each vignette by the same amount, the first in the given
    * iterable is selected.
    *
    * No other factors are taken into account (brightness, quality, whether the
    * candidate is reachable at all offsets, etc) so the caller should filter
    * the candidates accordingly before calling.
    *
    * @return candidate with minimum vignetting and its vignetting ratio [0, 1] */
  def minCalc[A](candidates: List[A])(f: A => Coordinates): Option[(A, Double)] = {

    // To be deleted ...
    def formatCoordinates(c: Coordinates): String = s"coordinates(${formatRa(c.ra)}, ${formatDec(c.dec)})"
    def formatRa(ra: RightAscension): String      = Angle.formatHMS(ra.toAngle)
    def formatDec(dec: Declination): String       = Declination.formatDMS(dec)

    // Explicitly recurse in order to stop if we see a 0 vignetting option,
    // which should be relatively common.
    @tailrec
    def go(rem: List[A], curMin: Option[(A, Double)]): Option[(A, Double)] =
      rem match {
        case Nil     => curMin
        case a :: as =>
          val vignetting = calc(f(a))
          if (curMin.forall(_._2 > vignetting)) {
            curMin.foreach { case (t,d) =>
              LOGGER.info(f"AGS rejecting ${formatCoordinates(f(t))}. Vignettes ${d*100}%.2f%%.")
            }
            val newMin = Some((a, vignetting))
            if (vignetting == 0.0) newMin else go(as, newMin)
          } else go(as, curMin)
      }

    go(candidates, None)
  }
}

object VignettingCalculator {

  def apply(ctx: ObsContext, probeArm: ProbeArmGeometry, scienceArea: ScienceAreaGeometry): VignettingCalculator =
    new VignettingCalculator {
      // area of detector or slit for this context
      val whole = scienceArea.unadjustedGeometry(ctx).map(approximateArea)

      // list of science (offset) positions paired with detector shape at that
      // position
      val offs  = ctx.getSciencePositions.asScala.toList.flatMap { skycalcOff =>
        val offset = skycalcOff.toNewModel
        scienceArea.geometry(ctx, offset).map { shape => (offset, shape) }
      }

      override def calc(guideStar: Coordinates): Double =
        offs match {
          case Nil => 0.0
          case os  => os.map { case (off, sciShape) =>
            val probeShape = probeArm.geometry(ctx, guideStar, off) | new Area()
            val vigShape   = new Area(sciShape) <| (_.intersect(new Area(probeShape)))
            whole.map { area => approximateArea(vigShape) / area } | 0.0
          }.max
        }
    }
}
