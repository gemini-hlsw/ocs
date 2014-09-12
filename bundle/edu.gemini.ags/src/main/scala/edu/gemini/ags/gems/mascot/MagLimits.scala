package edu.gemini.ags.gems.mascot

/**
 * Defines limits for magnitude bands (Do we need upper and lower bounds?)
 */
final class MagLimits(val bmag: Double,
                      val vmag: Double,
                      val rmag: Double,
                      val jmag: Double,
                      val hmag: Double,
                      val kmag: Double) {

  /**
   * Returns true if the given star is within the mag limits
   */
  def filter(star: Star): Boolean = {
    star.bmag <= bmag &&
      star.vmag <= vmag &&
      star.rmag <= rmag &&
      star.jmag <= jmag &&
      star.hmag <= hmag &&
      star.kmag <= kmag
  }
}


object MagLimits {

  // value that matches any mag
  val defaultMag = 99

  /**
   * Allows creating without the "new" keyword
   */
  def apply(bmag: Double = defaultMag,
            vmag: Double = defaultMag,
            rmag: Double = defaultMag,
            jmag: Double = defaultMag,
            hmag: Double = defaultMag,
            kmag: Double = defaultMag): MagLimits = new MagLimits(bmag, vmag, rmag, jmag, hmag, kmag)
}
