package edu.gemini.ags.gems.mascot

import java.util.logging.Logger

import edu.gemini.ags.gems.mascot.Mascot.ProgressFunction
import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.core.SiderealTarget
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.target.SPTarget

import scalaz._
import Scalaz._

/**
 * Provides methods to find the best ODGW or CWFS guide stars using the Mascot Strehl algorithm
 */
object MascotGuideStar {
  val Log = Logger.getLogger(MascotGuideStar.getClass.getSimpleName)

  // default magnitude limits
  val defaultMagLimits = MagLimits()

  // Number of different position angles within the given tolerance range to try
  val numPosAngles = 10

  // Number of different base positions to try for each position angle (should be a square)
  val numBasePositions = 5 * 5

  /**
   * Finds the best ODGW or CWFS asterisms for the given table of stars.
   *
   * @param ctx the observation context
   * @param guideStarType CWFS or ODGW, defined in this class
   * @param posAngleTolerance allow the pos angle to change +- this amount in degrees
   * @param basePosTolerance allow the base position to change +- this amount in arcsec
   * @param factor multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
   * @param magLimits a set of optional magnitude limits used to filter the star list
   * @param progress a function(strehl, count, total) called for each asterism as it is calculated
   *
   * @return a list of tuples (strehlList, posAngle, ra, dec), where the strehl list in each
   * tuple contains only the asterisms that are valid for the guide star at that position angle
   * and ra,dec base position
   */
  protected [mascot] def findBestAsterismInQueryResult(queryResult: List[SiderealTarget],
                                    ctx: ObsContext,
                                    guideStarType: GuideStarType,
                                    posAngleTolerance: Double = 0,
                                    basePosTolerance: Double = 0,
                                    factor: Double = Mascot.defaultFactor,
                                    magLimits: MagLimits = defaultMagLimits,
                                    progress: ProgressFunction = Mascot.defaultProgress)
  : List[(List[Strehl], Double, Double, Double)] =
    ctx.getBaseCoordinates.asScalaOpt.foldMap { base =>
      val center = base.toNewModel
      val simple = posAngleTolerance == 0.0 && basePosTolerance == 0.0
      val guideStarFilter = guideStarType.filter(ctx, magLimits, _: Star)
      val asterismPreFilter = (lst: List[SiderealTarget]) => guideStarType.guideGroup.asterismPreFilter(lst.asImList)
      // If no tolerances were given, we can do more filtering up front
      val filter = if (simple) guideStarFilter else magLimits.filter _
      val (_, strehlList) = MascotCat.findBestAsterism(queryResult, center.ra.toAngle.toDegrees, center.dec.toAngle.toDegrees, factor, progress, filter, asterismPreFilter)
      if (simple) {
        val basePos = base
        List((strehlList, ctx.getInstrument.getPosAngleDegrees, basePos.getRaDeg, basePos.getDecDeg))
      } else {
        asterismFilter(ctx.getInstrument.getPosAngleDegrees, center, guideStarFilter, posAngleTolerance, basePosTolerance, strehlList)
      }
    }


  /**
   * Filters the given list of asterisms (Strehl objects) using the given settings.
   *
   * @param posAngle the position angle
   * @param center base coordinates
   * @param guideStarFilter a filter that returns true if the given star is acceptable
   * @param posAngleTolerance position angle can be +- this amount in degrees
   * @param basePosTolerance base position can be +- this amount in arcsec
   * @param strehlList the list of asterisms
   *
   * @return a list of tuples (strehlList, posAngle, ra, dec), where the strehl list in each
   * tuple contains only the asterisms that are valid for the guide star at that position angle
   * and ra,dec base position
   */
  private def asterismFilter(posAngle: Double,
                             center: Coordinates,
                             guideStarFilter: Star => Boolean,
                             posAngleTolerance: Double = 0.0,
                             basePosTolerance: Double = 0.0,
                             strehlList: List[Strehl]): List[(List[Strehl], Double, Double, Double)] = {

    val savedRa = center.ra.toAngle.toDegrees
    val savedDec = center.dec.toDegrees
    val settingsList = settingsToTry(posAngle, savedRa, savedDec, posAngleTolerance, basePosTolerance)

    for ((pa, ra, dec) <- settingsList) yield {
      // XXX TODO use a cache map in the filter?
      val l = strehlList.filter(_.stars.forall(guideStarFilter))
      (l, pa, ra, dec)
    }
  }

  /**
   * Returns a list of position angles to try based on the given tolerances.
   * @param pa the position angle
   * @param posAngleTolerance position angle can be +- this amount in degrees
   */
  private def positionAnglesToTry(pa: Double, posAngleTolerance: Double): List[Double] = {
    if (posAngleTolerance == 0.0) {
      List(pa)
    } else {
      val inc = posAngleTolerance * 2 / numPosAngles
      val paList = for (i <- 0 to numPosAngles / 2) yield {
        List(pa - i * inc, pa + i * inc)
      }
      paList.toList.flatMap(_.toList).tail
    }
  }


  /**
   * Returns a list of tuples (ra, dec) to try based on the given tolerances.
   *
   * @param ra the ra coordinate
   * @param dec the dec coordinate
   * @param basePosTolerance base position can be +- this amount in arcsec
   */
  private def basePositionsToTry(ra: Double, dec: Double, basePosTolerance: Double): List[(Double, Double)] = {
    if (basePosTolerance == 0.0) {
      List((ra, dec))
    } else {
      val numPos = math.sqrt(numBasePositions).asInstanceOf[Int]
      val tdeg = basePosTolerance / 3600
      val inc = tdeg * 2 / numPos
      val posList =
        for (i <- 0 to numPos / 2; j <- 0 to numPos / 2) yield
          List((ra - i * inc, dec - j * inc), (ra + i * inc, dec + j * inc))
      // filter out corner cases and sort so that smaller changes come first
      posList.toList.flatMap(_.toList).tail.filter(
        p => dist(ra, dec, p._1, p._2) <= tdeg
      ).sortWith(
        (p1, p2) => dist(ra, dec, p1._1, p1._2) < dist(ra, dec, p2._1, p2._2))
    }
  }

  // Simple distance between points, for sorting
  def dist(x1: Double, y1: Double, x2: Double, y2: Double): Double = {
    val dx = math.abs(x2 - x1)
    val dy = math.abs(y2 - y1)
    math.sqrt(dx * dx + dy * dy)
  }

  /**
   * Returns a list of tuples (pa, ra, dec) for the different position angles and
   * ra,dec coordinates to try based on the given tolerances.
   *
   * @param pa the position angle
   * @param ra the ra coordinate
   * @param dec the dec coordinate
   * @param posAngleTolerance position angle can be +- this amount in degrees
   * @param basePosTolerance base position can be +- this amount in arcsec
   */
  private def settingsToTry(pa: Double,
                    ra: Double,
                    dec: Double,
                    posAngleTolerance: Double,
                    basePosTolerance: Double)
  : List[(Double, Double, Double)] = {
    def posAngles = positionAnglesToTry(pa, posAngleTolerance)
    def posList = basePositionsToTry(ra, dec, basePosTolerance)

    val l = for (pa <- posAngles; pos <- posList) yield (pa, pos._1, pos._2)
    l.toList
  }
}
