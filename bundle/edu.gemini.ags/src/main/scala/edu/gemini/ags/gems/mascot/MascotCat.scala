package edu.gemini.ags.gems.mascot

import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.core.Target.SiderealTarget
import jsky.coords.WorldCoords
import edu.gemini.ags.impl._
import scala.collection.JavaConversions._
import collection.JavaConversions
import java.util.concurrent.CancellationException

/**
 *
 */
object MascotCat {

  // default catalog
  val defaultCatalogName = "PPMXL Catalog at CDS"

  // Default min radius for catalog query in arcmin
  val defaultMinRadius = 0.0

  // Default max radius for catalog query in arcmin
  val defaultMaxRadius = 1.2

  // Default max number of rows to return from a catalog query
  val defaultMaxRows = 1000

  // Default progress callback, called for each asterism as it is calculated
  val defaultProgress = (s: Strehl, count: Int, total: Int) => {
    print("Asterism #" + count)
    for (i <- 0 until s.stars.size) {
      print(", [%s]".format(new WorldCoords(s.stars(i).target.coordinates.ra.toAngle.toDegrees, s.stars(i).target.coordinates.dec.toAngle.toDegrees)))
    }
    println("\nStrehl over %.1f\": avg=%.1f  rms=%.1f  min=%.1f  max=%.1f\n" format (
      s.halffield * 2, s.avgstrehl * 100, s.rmsstrehl * 100, s.minstrehl * 100, s.maxstrehl * 100))
  }

  /**
   * Finds the best asterisms for the given list of SiderealTargets.
   * @param list the list of SiderealTargets to use
   * @param centerRA the base position RA coordinate
   * @param centerDec the base position Dec coordinate
   * @param bandpass determines which magnitudes are used in the calculations: (one of "B", "V", "R", "J", "H", "K")
   * @param factor multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
   * @param progress a function(strehl, count, total) called for each asterism as it is calculated
   * @param filter a filter function that returns false if the Star should be excluded
   * @return a tuple: (list of stars actually used, list of asterisms found)
   */
  def findBestAsterismInTargetsList(list: List[SiderealTarget],
                       centerRA: Double, centerDec: Double,
                       bandpass: MagnitudeBand = Mascot.defaultBandpass,
                       factor: Double = Mascot.defaultFactor,
                       progress: (Strehl, Int, Int) => Unit = defaultProgress,
                       filter: Star => Boolean = Mascot.defaultFilter)
  : (List[Star], List[Strehl]) = {
    val starList = list.map(Star.makeStar(_, centerRA, centerDec))
    Mascot.findBestAsterism(starList.toList, bandpass, factor, progress, filter)
  }

  case class StrehlResults(starList: java.util.List[Star], strehlList: java.util.List[Strehl])

  /**
   * Finds the best asterisms for the given list of SiderealTarget
   * (This version is easier to call from Java).
   * @param javaList the list of SiderealTargets to use
   * @param centerRA the base position RA coordinate
   * @param centerDec the base position Dec coordinate
   * @param bandpass determines which magnitudes are used in the calculations: (one of "B", "V", "R", "J", "H", "K")
   * @param mascotProgress optional, called for each asterism as it is calculated, can cancel the calculations by returning false
   * @return a tuple: (list of stars actually used, list of asterisms found)
   */
  def javaFindBestAsterismInTargetsList(javaList: java.util.List[SiderealTarget],
                       centerRA: Double, centerDec: Double,
                       bandpass: MagnitudeBand, factor: Double,
                       mascotProgress: MascotProgress): StrehlResults = {

    val progress = (s: Strehl, count: Int, total: Int) => {
      defaultProgress(s, count, total)
      if (mascotProgress != null && !mascotProgress.progress(s, count, total, true)) {
        throw new CancellationException("Canceled")
      }
    }
    val list = JavaConversions.asScalaBuffer(javaList).toList
    val (starList, strehlList) = findBestAsterismInTargetsList(list, centerRA, centerDec, bandpass, factor, progress,
      Mascot.defaultFilter)
    new StrehlResults(starList, strehlList)
  }

}
