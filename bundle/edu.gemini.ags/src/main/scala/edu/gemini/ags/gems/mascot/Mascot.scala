package edu.gemini.ags.gems.mascot

import edu.gemini.ags.gems.mascot.util.AllPairsAndTriples
import edu.gemini.ags.gems.mascot.util.YUtils._

import MascotUtils._
import MascotConf._
import breeze.linalg._
import edu.gemini.spModel.core.MagnitudeBand

import scalaz._
import Scalaz._

/**
 */
object Mascot {

  // Default star filter
  val defaultFilter = (s: Star) => true

  // Default progress callback, called for each asterism as it is calculated
  val defaultProgress = (s: Strehl, count: Int, total: Int) => {
    print("Asterism #" + count)
    for (i <- 0 until s.stars.size) {
      print(", [%.1f,%.1f]" format (s.stars(i).x, s.stars(i).y))
    }
    println("\nStrehl over %.1f\": avg=%.1f  rms=%.1f  min=%.1f  max=%.1f\n" format (
      s.halffield * 2, s.avgstrehl * 100, s.rmsstrehl * 100, s.minstrehl * 100, s.maxstrehl * 100))
  }

  // The default mag bandpass
  val defaultBandpass:MagnitudeBand = MagnitudeBand.R

  // multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
  val defaultFactor = 1.0

  /**
   * Performs the strehl algorithm on the given 1, 2 or 3 stars (2 and 3 are optional)
   * @param bandpass determines which magnitudes are used in the calculations: (one of "B", "V", "R", "J", "H", "K")
   * @param factor multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
   * @param n1 the first star to use
   * @param n2 the optional second star to use
   * @param n3 the optional third star to use
   * @return a Some(Strehl) object containing the results of the computations, or None if the positions can't be used
   */
  def computeStrehl(bandpass: MagnitudeBand, factor: Double, n1: Star, n2: Option[Star] = None, n3: Option[Star] = None): Option[Strehl] = {
    n2 match {
      case Some(v2) if !doesItFit(n1, v2, n3) =>
        println("Skipped. Does not fit.")
        None
      case _                                  =>
        //          sdata = mascot_compute_strehl();
        //          grow,sall,sdata;
        //          window,3;
        //          disp_strehl_map,sdata;
        Strehl(List(n1.some, n2, n3).flatten, bandpass, factor).some
    }
  }

  /**
   * Finds the best asterisms for the given list of stars.
   * @param starList unfiltered list of stars from a catalog query
   * @param bandpass determines which magnitudes are used in the calculations: (one of "B", "V", "R", "J", "H", "K")
   * @param factor multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
   * @param filter a filter function that returns false if the Star should be excluded
   * @param progress a function(strehl, count, total) called for each asterism as it is calculated
   * @return a tuple: (list of stars actually used, list of asterisms found)
   */
  def findBestAsterism(starList: List[Star],
                       bandpass: MagnitudeBand = defaultBandpass,
                       factor: Double = defaultFactor,
                       progress: (Strehl, Int, Int) => Unit = defaultProgress,
                       filter: Star => Boolean = defaultFilter)
  : (List[Star], List[Strehl]) = {
    // sort by selected mag and select
    val sortedStarList = starList.sortWith((s1,s2) => s1.target.magnitudeIn(bandpass) < s2.target.magnitudeIn(bandpass))
    val filteredStarList = selectStarsOnMag(sortedStarList, bandpass).filter(filter)

    val ns = filteredStarList.length
    var count = 0
    val trips = AllPairsAndTriples.allTrips(filteredStarList)
    val pairs = AllPairsAndTriples.allPairs(filteredStarList)
    val total = trips.length + pairs.length + ns
    var result = List[Strehl]()

    println("XXX Mascot.findBestAsterism: input stars: " + ns + ", total asterisms: " + total)

    if (ns >= 3) {
      for ((n1, n2, n3) <- trips) {
        count += 1
        computeStrehl(bandpass, factor, n1, n2.some, n3.some).foreach { s =>
          progress(s, count, total)
          result = s :: result
        }
      }
    }

    if (ns >= 2) {
      for ((n1, n2) <- pairs) {
        count += 1
        computeStrehl(bandpass, factor, n1, n2.some).foreach { s =>
          progress(s, count, total)
          result = s :: result
        }
      }
    }

    if (ns >= 1) {
      for (n1 <- filteredStarList) {
        count += 1
        computeStrehl(bandpass, factor, n1).foreach { s =>
          progress(s, count, total)
          result = s :: result
        }
      }
    }

    (filteredStarList, sortBestAsterisms(result))
  }

  //func select_stars_on_mag(void)
  ///* DOCUMENT select_stars_on_mag(void)
  //   Downselect stars within magnitude range in starlist.
  //   SEE ALSO:select_stars_not_too_close
  // */
  //{
  //  extern starlist;
  //
  //  mag = allstarlist(5,);
  //  w = where( (mag>=mag_min_threshold) & (mag<=mag_max_threshold) );
  //  starlist = allstarlist(,w);
  //
  //  status = select_stars_not_too_close()
  //}
  def selectStarsOnMag(starList: List[Star], bandpass: MagnitudeBand = defaultBandpass): List[Star] = {
    selectStarsNotTooClose(starList.filter(star => {
      val mag = star.target.magnitudeIn(bandpass).map(_.value)
      mag >= mag_min_threshold && mag <= mag_max_threshold
    }))
  }


  //func select_stars_not_too_close(void)
  ///* DOCUMENT select_stars_not_too_close(void)
  //   Remove from starlist the faint stars around bright stars.
  //   Here is how it is done:
  //   starlist is already sorted from the brightest to the faintest star.
  //   The list is walked, starting from the brightest star.
  //   For each star, we remove from the starlist all the fainter stars
  //   that are closer than crowding_radius. Eventually, this provides a list
  //   of the brightest stars that pad the field as regularly as possible.
  //   We do that iteratively (increasing crowding_radius at each iteration)
  //   to end up with no more than nstar_limit stars.
  //   SEE ALSO:
  // */
  //{
  //  extern starlist;
  //
  //  if (starlist==[]) return;
  //
  //  ns = dimsof(starlist)(0);
  //  valid = array(1,ns);
  //
  //  crowd_rad = float(crowding_radius);
  //
  //  do {
  //    for (i=1;i<=ns-1;i++) { // for each stars:
  //      // look at the distance to next (fainter) stars:
  //      dd = abs(starlist(1,)-starlist(1,i),starlist(2,)-starlist(2,i));
  //      ok = (dd>=crowd_rad);
  //      valid(i+1:) *= ok(i+1:);
  //    }
  //    crowd_rad += 2;
  //  } while (sum(valid)>nstar_limit);
  //  crowd_rad -= 2;
  //
  //  write,format="Select stars: found optimum crowding radius=%.0f\"\n",
  //    crowd_rad;
  //
  //  starlist = starlist(,where(valid));
  //
  //  status = disp_stars();
  //}
  def selectStarsNotTooClose(starList: List[Star]): List[Star] = {
    val ns = starList.size
    val starMat = DenseMatrix.zeros[Double](2, ns)
    for (i <- 0 until ns) {
      starMat(0, i) = starList(i).x
      starMat(1, i) = starList(i).y
    }
    val valid = DenseVector.ones[Double](ns)
    var crowd_rad = crowding_radius

    do {
      for (i <- 0 until ns - 1) {
        // for each star, look at the distance to next (fainter) stars:
        val dd = abs(starMat(0, ::).toDenseVector - starMat(0, i), starMat(1, ::).toDenseVector - starMat(1, i))
        val ok = dd.mapValues(d => if (d >= crowd_rad) 1.0 else 0.0)
        valid(i + 1 until valid.size) :*= ok(i + 1 until ok.size)
      }
      crowd_rad += 2
    } while (valid.sum > nstar_limit)
    crowd_rad -= 2

    println("Select stars: found optimum crowding radius=%d\"\n" format crowd_rad)

    starList.zipWithIndex collect {
      case (s, i) if valid(i) != 0.0 => s
    }
  }

  /**
   * Sorts the list of asterisms by descending avg strehl values.
   * See sort_best_asterisms in yorick original.
   */
  def sortBestAsterisms(sall: List[Strehl]) : List[Strehl] = {
    if (sall.size < 2)
      sall
    else
      sall.sortWith((s1, s2) => s1.avgstrehl > s2.avgstrehl)
  }

}
