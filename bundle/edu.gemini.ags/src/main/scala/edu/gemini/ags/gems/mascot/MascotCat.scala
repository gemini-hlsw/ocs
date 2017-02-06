package edu.gemini.ags.gems.mascot

import java.util.logging.Logger

import edu.gemini.ags.gems.mascot.Mascot.ProgressFunction
import edu.gemini.spModel.core.{BandsList, MagnitudeBand}
import edu.gemini.spModel.core.SiderealTarget
import java.util.concurrent.CancellationException

/**
 *
 */
object MascotCat {
  val Log = Logger.getLogger(MascotCat.getClass.getSimpleName)

  // default catalog
  val defaultCatalogName = "PPMXL Catalog at CDS"

  // Default min radius for catalog query in arcmin
  val defaultMinRadius = 0.0

  // Default max radius for catalog query in arcmin
  val defaultMaxRadius = 1.2

  // Default max number of rows to return from a catalog query
  val defaultMaxRows = 1000

  // Default progress callback, called for each asterism as it is calculated
  val defaultProgress: ProgressFunction = (s: Strehl, count: Int, total: Int) => {
    Log.fine(s"Asterism #$count")
    for (i <- s.stars.indices) {
      Log.finer(s.stars(i).target.coordinates.toString)
    }
    Log.fine(f"Strehl over ${s.halffield * 2}%.1f: avg=${s.avgstrehl * 100}%.1f rms=${s.rmsstrehl * 100}%.1f min=${s.minstrehl * 100}%.1f max=${s.maxstrehl * 100}%.1f")
    true
  }

  /**
   * Finds the best asterisms for the given list of SiderealTargets.
   * @param list the list of SiderealTargets to use
   * @param centerRA the base position RA coordinate
   * @param centerDec the base position Dec coordinate
   * @param factor multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
   * @param progress a function(strehl, count, total) called for each asterism as it is calculated
   * @param filter a filter function that returns false if the Star should be excluded
   * @return a tuple: (list of stars actually used, list of asterisms found)
   */
  def findBestAsterism(list: List[SiderealTarget],
                       centerRA: Double, centerDec: Double,
                       factor: Double = Mascot.defaultFactor,
                       progress: ProgressFunction = defaultProgress,
                       filter: Star => Boolean = Mascot.defaultFilter)
  : (List[Star], List[Strehl]) = {
    val starList = list.map(Star.makeStar(_, centerRA, centerDec))
    Mascot.findBestAsterism(starList, factor, progress, filter)
  }

  case class StrehlResults(starList: List[Star], strehlList: List[Strehl])

  /**
   * Finds the best asterisms for the given list of SiderealTarget
   *
   * @param javaList the list of SiderealTargets to use
   * @param centerRA the base position RA coordinate
   * @param centerDec the base position Dec coordinate
   * @param band determines which magnitudes are used in the calculations: (one of "B", "V", "R", "J", "H", "K")
   * @param mascotProgress optional, called for each asterism as it is calculated, can cancel the calculations by returning false
   * @return a tuple: (list of stars actually used, list of asterisms found)
   */
  def findBestAsterismInTargetsList(javaList: List[SiderealTarget],
                                    centerRA: Double, centerDec: Double,
                                    band: BandsList, factor: Double,
                                    mascotProgress: Option[MascotProgress]): StrehlResults = {
    val progress:ProgressFunction = (s: Strehl, count: Int, total: Int) => {
      defaultProgress(s, count, total)
      mascotProgress.foreach { p =>
        if (!p.progress(s, count, total, true)) {
          throw new CancellationException("Canceled")
        }
      }
      true
    }

    val (starList, strehlList) = findBestAsterism(javaList, centerRA, centerDec, factor, progress, Mascot.defaultFilter)
    StrehlResults(starList, strehlList)
  }

  /**
   * Finds the best asterisms for the given list of SiderealTarget
   *
   * @param javaList the list of SiderealTargets to use
   * @param centerRA the base position RA coordinate
   * @param centerDec the base position Dec coordinate
   * @param band determines which magnitudes are used in the calculations: (one of "B", "V", "R", "J", "H", "K")
   * @param shouldStop called for each asterism as it is calculated, can cancel the calculations by returning true
   * @return a tuple: (list of stars actually used, list of asterisms found)
   */
  def findBestAsterismInTargetsList(javaList: List[SiderealTarget],
                                    centerRA: Double, centerDec: Double,
                                    band: BandsList, factor: Double,
                                    shouldStop: (Strehl, Boolean) => Boolean): StrehlResults = {
    val progress:ProgressFunction = (s: Strehl, count: Int, total: Int) => {
      defaultProgress(s, count, total)
      // TODO: Why is the usable parameter always true?
      shouldStop(s, true)
    }

    val (starList, strehlList) = findBestAsterism(javaList, centerRA, centerDec, factor, progress, Mascot.defaultFilter)
    StrehlResults(starList, strehlList)
  }

}
