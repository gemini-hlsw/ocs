package edu.gemini.ags.gems.mascot

import MascotConf._

import edu.gemini.spModel.core._
import edu.gemini.spModel.core.Target.SiderealTarget

/**
 * An object in a Mascot starlist, extracted from the results of a query to a suitable catalog.
 * x and y are calculated (offsets from the center position in arcsec).
 * m is calculated from the mag values.
 */
case class Star(target: SiderealTarget,
                 x: Double, // 1 index in the original sources
                 y: Double, // 2
                 m: Double)

object Star {

  /**
   * Utility method that returns a Star object for the given target.
   * centerX, centerY are the WCS coordinates (deg) of the image center/base pos.
   */
  def makeStar(target: SiderealTarget, centerX: Double, centerY: Double): Star = {
    val (x, y) = calculateXy(target.coordinates.ra.toAngle.toDegrees, target.coordinates.dec.toDegrees, centerX, centerY)
    val (m, rmag2) = calculateM(target.magnitudeIn(MagnitudeBand.B).map(_.value).getOrElse(invalidMag), target.magnitudeIn(MagnitudeBand.V).map(_.value).getOrElse(invalidMag), target.magnitudeIn(MagnitudeBand.R).map(_.value).getOrElse(invalidMag))
    Star(target, x, y, m)
  }

  /**
   * Utility method that returns a Star object for the given values.
   * centerX,centerY are the WCS coordinates (deg) of the image center/base pos.
   * ra,dec are the WCS coordinates (deg) of the star and the values are the magnitudes.
   */
  def makeStar(name: String, centerX: Double, centerY: Double,
                 bmag: Double, vmag: Double,
                 rmag: Double, jmag: Double,
                 hmag: Double, kmag: Double,
                 ra: Double, dec: Double): Star = {
    val coordinates = Coordinates(RightAscension.fromAngle(Angle.fromDegrees(ra)), Declination.fromAngle(Angle.fromDegrees(dec)).getOrElse(Declination.zero))
    val magnitudes = List(new Magnitude(bmag, MagnitudeBand.B), new Magnitude(vmag, MagnitudeBand.V), new Magnitude(rmag, MagnitudeBand.R), new Magnitude(jmag, MagnitudeBand.J), new Magnitude(hmag, MagnitudeBand.H), new Magnitude(kmag, MagnitudeBand.K))
    val target = SiderealTarget(name, coordinates, None, magnitudes, None)
    makeStar(target, centerX, centerY)
  }
  /**
   * Returns the (x, y) offsets (in arcsec) from the center position for the given (ra, dec) (deg)
   */
  private def calculateXy(ra: Double, dec: Double, raCenter: Double, decCenter: Double): (Double, Double) = {
    val distStarX = (raCenter - ra) * 3600.0
    val decRad = dec * math.Pi / 180.0
    val distStarY = (dec - decCenter) * 3600.0
    (distStarX * math.cos(decRad), distStarY)
  }

  // Calculates and returns (m, rmag) based on the given mag values
  private def calculateM(bmag: Double, vmag: Double, rmag: Double): (Double, Double) = {
    var m = 0
    var rmag2 = rmag
    if (rmag == invalidMag) {
      if (vmag != invalidMag && bmag != invalidMag) {
        rmag2 = check_mag(bmag, vmag)
        m = if (rmag2 < 18.0) 1 else 0
      } else {
        //m = if (vmag != invalidMag && vmag < 17) 1 else 0
        m = if (bmag != invalidMag && bmag < 17) 1 else 0
      }
    } else {
      m = if (rmag2 < 18.0) 2 else 0
    }
    (m, rmag2)
  }

  /* DOCUMENT check_mag(bMag,vMag)
    Added by FR: I believe this returns the R mag from B and V
    if R is not present.
    SEE ALSO:
  */
  private def check_mag(bMag: Double, vMag: Double): Double = {
    val a = Array(0.0530768, 0.794274, 0.212565, -0.867596, 0.77699, -0.161851)
    val x = bMag - vMag

    val funcMag = a(0) + a(1) * x + a(2) * math.pow(x, 2.0) + a(3) * math.pow(x, 3) + a(4) * math.pow(x, 4) + a(5) * math.pow(x, 5)

    val rMag = vMag - funcMag

    rMag
  }

}
