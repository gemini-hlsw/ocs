package edu.gemini.ags.gems.mascot

import MascotConf._

import breeze.linalg._
import breeze.util._

/**
 * An object in a Mascot starlist, extracted from the results of a query to a suitable catalog.
 * x and y are calculated (offsets from the center position in arcsec).
 * m is calculated from the mag values.
 */
final class Star(val name: String,
                 val x: Double, // 1 index in the original sources
                 val y: Double, // 2
                 val bmag: Double, // 3
                 val vmag: Double, // 4
                 val rmag: Double, // 5
                 val jmag: Double, // 6
                 val hmag: Double, // 7
                 val kmag: Double, // 8
                 val m: Double, // 9
                 val ra: Double, // 10
                 val dec: Double) {

  /**
   * Returns the magnitude for the given bandpass (one of "B", "V", "R", "J", "H", "K").
   * Defaults to rmag.
   */
  def mag(bandpass: String): Double = {
    bandpass match {
      case "B" => bmag
      case "V" => vmag
      case "R" => rmag
      case "J" => jmag
      case "H" => hmag
      case "K" => kmag
      case _ => rmag
    }
  }

  override def toString =
    "[" + x + "," + y + "," + bmag + "," + vmag + "," + rmag + "," + jmag +
      "," + hmag + "," + kmag + "," + m + "," + ra + "," + dec + "]"

}


object Star {

  /**
   * Allows creating Star objects without the "new" keyword
   */
  def apply(x: Double,
            y: Double,
            bmag: Double,
            vmag: Double,
            rmag: Double,
            jmag: Double,
            hmag: Double,
            kmag: Double,
            m: Double,
            ra: Double,
            dec: Double): Star = {

    new Star(null, x, y, bmag, vmag, rmag, jmag, hmag, kmag, m, ra, dec)
  }

  /**
   * Allows creating Star objects without the "new" keyword
   */
  def apply(name: String,
            x: Double,
            y: Double,
            bmag: Double,
            vmag: Double,
            rmag: Double,
            jmag: Double,
            hmag: Double,
            kmag: Double,
            m: Double,
            ra: Double,
            dec: Double): Star = {

    new Star(name, x, y, bmag, vmag, rmag, jmag, hmag, kmag, m, ra, dec)
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
    val (x, y) = calculateXy(ra, dec, centerX, centerY)
    val (m, rmag2) = calculateM(bmag, vmag, rmag)
    Star(name, x, y, bmag, vmag, rmag2, jmag, hmag, kmag, m, ra, dec)
  }

  /**
   * Returns the (x, y) offsets (in arcsec) from the center position for the given (ra, dec) (deg)
   */
  private def calculateXy(ra: Double, dec: Double, raCenter: Double, decCenter: Double): (Double, Double) = {
    val distStarX = (raCenter - ra) * 3600.0;
    val decRad = dec * math.Pi / 180.0;
    val distStarY = (dec - decCenter) * 3600.0;
    (distStarX * math.cos(decRad), distStarY)
  }

  // Calculates and returns (m, rmag) based on the given mag values
  private def calculateM(bmag: Double, vmag: Double, rmag: Double): (Double, Double) = {
    var m = 0;
    var rmag2 = rmag
    if (rmag == invalidMag) {
      if (vmag != invalidMag && bmag != invalidMag) {
        rmag2 = check_mag(bmag, vmag);
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
    val x = bMag - vMag;

    val funcMag = a(0) + a(1) * x + a(2) * math.pow(x, 2.0) + a(3) * math.pow(x, 3) + a(4) * math.pow(x, 4) + a(5) * math.pow(x, 5);

    val rMag = vMag - funcMag;

    rMag;
  }

}
