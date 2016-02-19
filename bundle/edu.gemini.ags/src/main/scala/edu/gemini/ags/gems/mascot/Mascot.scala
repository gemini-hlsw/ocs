package edu.gemini.ags.gems.mascot

import java.util.logging.Logger

import edu.gemini.ags.gems.mascot.util.AllPairsAndTriples
import edu.gemini.ags.gems.mascot.util.YUtils._

import MascotUtils._
import MascotConf._
import breeze.linalg._
import edu.gemini.spModel.core.{BandsList, MagnitudeBand}

import scala.annotation.tailrec
import scalaz._
import Scalaz._

/**
 */
object Mascot {
  val Log = Logger.getLogger(Mascot.getClass.getSimpleName)

  type ProgressFunction = (Strehl, Int, Int) => Boolean

  // Default star filter
  val defaultFilter = (s: Star) => true

  // Default progress callback, called for each asterism as it is calculated
  val defaultProgress:ProgressFunction = (s: Strehl, count: Int, total: Int) => {
    Log.info(s"Asterism #$count")
    for (i <- s.stars.indices) {
      Log.finer(s"[${s.stars(i).x}%.1f,${s.stars(i).y}%.1f]")
    }
    Log.info(f"Strehl over ${s.halffield * 2}%.1f: avg=${s.avgstrehl * 100}%.1f  rms=${s.rmsstrehl * 100}%.1f  min=${s.minstrehl * 100}%.1f  max=${s.maxstrehl * 100}%.1f")
    true
  }

  case class StarTriple(n1: Star, n2: Option[Star], n3: Option[Star]) {
    def toList: List[Star] = List(n1.some, n2, n3).flatten
  }


  // The default mag bandpass
  val defaultBandpass:MagnitudeBand = MagnitudeBand.R

  // multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
  val defaultFactor = 1.0

  @Deprecated
  def computeStrehl4Java(bandsList: BandsList, factor: Double, n1: Star, n2: Option[Star] = None, n3: Option[Star] = None): Option[Strehl] =
    computeStrehl(factor, StarTriple(n1, n2, n3))

  /**
   * Performs the strehl algorithm on the given 1, 2 or 3 stars (2 and 3 are optional)
   * @param factor multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
   * @param st star triplet
   * @return a Some(Strehl) object containing the results of the computations, or None if the positions can't be used
   */
  def computeStrehl(factor: Double, st: StarTriple): Option[Strehl] = {
    st match {
      case StarTriple(n1, Some(n2), n3) if !doesItFit(n1, n2, n3) =>
        Log.warning("Skipped. Does not fit.")
        None
      case s                                  =>
        //          sdata = mascot_compute_strehl();
        //          grow,sall,sdata;
        //          window,3;
        //          disp_strehl_map,sdata;
        Strehl(s.toList, factor).some
    }
  }

  case class AsterismSearchStage(stars: List[Strehl], count: Int, continue: Boolean)

  /**
   * Finds the best asterisms for the given list of stars.
   * @param starList unfiltered list of stars from a catalog query
   * @param factor multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
   * @param filter a filter function that returns false if the Star should be excluded
   * @param progress a function(strehl, count, total) called for each asterism as it is calculated
   * @return a tuple: (list of stars actually used, list of asterisms found)
   */
  def findBestAsterism(starList: List[Star],
                       factor: Double = defaultFactor,
                       progress: ProgressFunction = defaultProgress,
                       filter: Star => Boolean = defaultFilter)
  : (List[Star], List[Strehl]) = {
    // sort by selected mag and select
    val sortedStarList = starList.sortWith((s1,s2) => s1.r < s2.r)
    val filteredStarList = selectStarsOnMag(sortedStarList).filter(filter)

    val ns = filteredStarList.length
    val trips = AllPairsAndTriples.allTrips(filteredStarList)
    val pairs = AllPairsAndTriples.allPairs(filteredStarList)
    val total = trips.length + pairs.length + ns

    Log.info(s"Mascot.findBestAsterism: input stars: $ns, total asterisms: $total")

    // Compute strehl for a sets of stars
    @tailrec
    def doStars(result: List[Strehl], count: Int, starSets: List[(Star, Option[Star], Option[Star])]):AsterismSearchStage = starSets match {
      case Nil =>
        AsterismSearchStage(result, count, continue = true)
      case s :: Nil =>
        val strehl = computeStrehl(factor, (StarTriple.apply _).tupled(s))
        // check if we continue
        val continue = strehl.map(progress(_, count, total))
        AsterismSearchStage(strehl.map(_ :: result).getOrElse(result), count, continue.getOrElse(true))
      case s :: tail =>
        val strehl = computeStrehl(factor, (StarTriple.apply _).tupled(s))
        // check if we continue
        val continue = strehl.map(progress(_, count, total))
        if (continue === false.some) {
          AsterismSearchStage(strehl.map(_ :: result).getOrElse(result), count, continue = false)
        } else {
          // Continue if the position is skipped or if progress says continue
          doStars(strehl.map(_ :: result).getOrElse(result), count + 1, tail)
        }
    }

    // Search each possible combination of triples, doubles and singles supporting cancellation
    def go(): List[Strehl] = {
      val triples = if (ns >= 3) doStars(Nil, 1, trips) else AsterismSearchStage(Nil, 1, continue = true)
      val doubles = if (ns >= 2 && triples.continue) doStars(triples.stars, triples.count, pairs.map(t => (t._1, t._2, None))) else triples
      val singles = if (ns >= 1 && doubles.continue) doStars(doubles.stars, doubles.count, filteredStarList.map(t => (t, None, None))) else doubles
      singles.stars
    }

    (filteredStarList, sortBestAsterisms(go()))
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
  def selectStarsOnMag(starList: List[Star]): List[Star] = {
    selectStarsNotTooClose(starList.filter(star => {
      star.r >= mag_min_threshold && star.r <= mag_max_threshold
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
        val dd = abs(starMat(0, ::).t.toDenseVector - starMat(0, i), starMat(1, ::).t.toDenseVector - starMat(1, i))
        val ok = dd.mapValues(d => if (d >= crowd_rad) 1.0 else 0.0)
        valid(i + 1 until valid.size) :*= ok(i + 1 until ok.size)
      }
      crowd_rad += 2
    } while (sum(valid) > nstar_limit)
    crowd_rad -= 2

    Log.info(s"Select stars: found optimum crowding radius=$crowd_rad")

    starList.zipWithIndex collect {
      case (s, i) if valid(i) != 0.0 => s
    }
  }

  /**
   * Sorts the list of asterisms by descending avg strehl values.
   * See sort_best_asterisms in yorick original.
   */
  def sortBestAsterisms(sall: List[Strehl]) : List[Strehl] =
    if (sall.size < 2)
      sall
    else
      sall.sortWith((s1, s2) => s1.avgstrehl > s2.avgstrehl)

}
