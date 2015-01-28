package edu.gemini.ags.gems.mascot

import edu.gemini.spModel.core.MagnitudeBand
import jsky.coords.WorldCoords
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.system.CoordinateParam.Units
import jsky.catalog.{TableQueryResult, QueryArgs}


/**
 * Provides methods to find the best ODGW or CWFS guide stars using the Mascot Strehl algorithm
 */
object MascotGuideStar {

  /**Type for CWFS guide stars */
  val CWFS = new CwfsGuideStar

  /**Type for ODGW guide stars */
  val ODGW = new OdgwGuideStar

  // default magnitude limits
  val defaultMagLimits = MagLimits()

  // Number of different position angles within the given tolerance range to try
  val numPosAngles = 10

  // Number of different base positions to try for each position angle (should be a square)
  val numBasePositions = 5 * 5

  /**
   * Finds the best ODGW or CWFS asterisms by querying the default PPMXL catalog for the given coordinates and radius.
   *
   * @param ctx the observation context
   * @param guideStarType CWFS or ODGW, defined in this class
   * @param posAngleTolerance allow the pos angle to change +- this amount in degrees
   * @param basePosTolerance allow the base position to change +- this amount in arcsec
   * @param bandpass the magnitude bandpass to use for the calculations
   *        (may be null, then uses default for guide star type)
   * @param magLimits a set of optional magnitude limits used to filter the star list
   * @param catName the long name of the catalog to use for the query
   * @param progress a function(Strehl, count, total) called for each asterism as it is calculated
   *
   * @return a list of tuples (strehlList, posAngle, ra, dec), where the strehl list in each
   * tuple contains only the asterisms that are valid for the guide star at that position angle
   * and ra,dec base position
   */
  protected [mascot] def findBestAsterism(ctx: ObsContext,
                       guideStarType: GuideStarType,
                       posAngleTolerance: Double = 0,
                       basePosTolerance: Double = 0,
                       bandpass: Option[MagnitudeBand] = None,
                       factor: Double = 1.0,
                       magLimits: MagLimits = defaultMagLimits,
                       catName: String = MascotCat.defaultCatalogName,
                       progress: (Strehl, Int, Int) => Unit = Mascot.defaultProgress)
  : List[(List[Strehl], Double, Double, Double)] = {

    val bp = bandpass.getOrElse(guideStarType.defaultBandpass)
    val basePos = ctx.getBaseCoordinates
    val coords = new WorldCoords(basePos.getRaDeg, basePos.getDecDeg)
    val maxRadius = MascotCat.defaultMaxRadius + basePosTolerance / 60.0
    val simple = posAngleTolerance == 0.0 && basePosTolerance == 0.0
    // XXX TODO add cache map arg to filter below to avoid multiple calculations for the same star?
    val guideStarFilter = guideStarType.filter(ctx, magLimits, _: Star)
    // If no tolerances were given, wen can do more filtering up front
    val filter = if (simple) guideStarFilter else magLimits.filter _
    val (_, strehlList) = MascotCat.findBestAsterism(coords, catName, bp, factor, progress, filter, maxRadius)
    if (simple) {
      List((strehlList, ctx.getInstrument.getPosAngleDegrees, basePos.getRaDeg, basePos.getDecDeg))
    } else {
      asterismFilter(ctx, guideStarFilter, posAngleTolerance, basePosTolerance, strehlList)
    }
  }


  /**
   * Finds the best ODGW or CWFS asterisms for the results of the given catalog query.
   *
   * @param queryArgs describes a catalog query
   * @param ctx the observation context
   * @param guideStarType CWFS or ODGW, defined in this class
   * @param posAngleTolerance allow the pos angle to change +- this amount in degrees
   * @param basePosTolerance allow the base position to change +- this amount in arcsec
   * @param bandpass the magnitude bandpass to use for the calculations
   *        (may be null, then uses default for guide star type)
   * @param factor multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
   * @param magLimits a set of optional magnitude limits used to filter the star list
   * @param progress a function(strehl, count, total) called for each asterism as it is calculated
   *
   * @return a list of tuples (strehlList, posAngle, ra, dec), where the strehl list in each
   * tuple contains only the asterisms that are valid for the guide star at that position angle
   * and ra,dec base position
   */
  private def findBestAsterismByQueryArgs(queryArgs: QueryArgs,
                                  ctx: ObsContext,
                                  guideStarType: GuideStarType,
                                  posAngleTolerance: Double = 0,
                                  basePosTolerance: Double = 0,
                                  bandpass: Option[MagnitudeBand] = Some(Mascot.defaultBandpass),
                                  factor: Double = 1.0,
                                  magLimits: MagLimits = defaultMagLimits,
                                  progress: (Strehl, Int, Int) => Unit = Mascot.defaultProgress)
  : List[(List[Strehl], Double, Double, Double)] = {

    val bp = bandpass.getOrElse(guideStarType.defaultBandpass)
    val simple = posAngleTolerance == 0.0 && basePosTolerance == 0.0
    val guideStarFilter = guideStarType.filter(ctx, magLimits, _: Star)
    // If no tolerances were given, wen can do more filtering up front
    val filter = if (simple) guideStarFilter else magLimits.filter _
    val (_, strehlList) = MascotCat.findBestAsterismByQueryArgs(queryArgs, bp, factor, progress, filter)
    if (simple) {
      val basePos = ctx.getBaseCoordinates
      List((strehlList, ctx.getInstrument.getPosAngleDegrees, basePos.getRaDeg, basePos.getDecDeg))
    } else {
      asterismFilter(ctx, guideStarFilter, posAngleTolerance, basePosTolerance, strehlList)
    }
  }

  /**
   * Finds the best ODGW or CWFS asterisms for the given table of stars.
   *
   * @param ctx the observation context
   * @param guideStarType CWFS or ODGW, defined in this class
   * @param posAngleTolerance allow the pos angle to change +- this amount in degrees
   * @param basePosTolerance allow the base position to change +- this amount in arcsec
   * @param bandpass the magnitude bandpass to use for the calculations
   *        (may be null, then uses default for guide star type)
   * @param factor multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
   * @param magLimits a set of optional magnitude limits used to filter the star list
   * @param progress a function(strehl, count, total) called for each asterism as it is calculated
   *
   * @return a list of tuples (strehlList, posAngle, ra, dec), where the strehl list in each
   * tuple contains only the asterisms that are valid for the guide star at that position angle
   * and ra,dec base position
   */
  protected [mascot] def findBestAsterismInQueryResult(queryResult: TableQueryResult,
                                    ctx: ObsContext,
                                    guideStarType: GuideStarType,
                                    posAngleTolerance: Double = 0,
                                    basePosTolerance: Double = 0,
                                    bandpass: Option[MagnitudeBand] = Some(Mascot.defaultBandpass),
                                    factor: Double = Mascot.defaultFactor,
                                    magLimits: MagLimits = defaultMagLimits,
                                    progress: (Strehl, Int, Int) => Unit = Mascot.defaultProgress)
  : List[(List[Strehl], Double, Double, Double)] = {

    val bp = bandpass.getOrElse(guideStarType.defaultBandpass)
    val simple = posAngleTolerance == 0.0 && basePosTolerance == 0.0
    val guideStarFilter = guideStarType.filter(ctx, magLimits, _: Star)
    // If no tolerances were given, wen can do more filtering up front
    val filter = if (simple) guideStarFilter else magLimits.filter _
    val (_, strehlList) = MascotCat.findBestAsterismInQueryResult(queryResult, bp, factor, progress, filter)
    if (simple) {
      val basePos = ctx.getBaseCoordinates
      List((strehlList, ctx.getInstrument.getPosAngleDegrees, basePos.getRaDeg, basePos.getDecDeg))
    } else {
      asterismFilter(ctx, guideStarFilter, posAngleTolerance, basePosTolerance, strehlList)
    }
  }


  /**
   * Filters the given list of asterisms (Strehl objects) using the given settings.
   *
   * @param ctx the observation context
   * @param guideStarFilter a filter that returns true if the given star is acceptable
   * @param posAngleTolerance position angle can be +- this amount in degrees
   * @param basePosTolerance base position can be +- this amount in arcsec
   * @param strehlList the list of asterisms
   *
   * @return a list of tuples (strehlList, posAngle, ra, dec), where the strehl list in each
   * tuple contains only the asterisms that are valid for the guide star at that position angle
   * and ra,dec base position
   */
  private def asterismFilter(ctx: ObsContext,
                     guideStarFilter: Star => Boolean,
                     posAngleTolerance: Double = 0.0,
                     basePosTolerance: Double = 0.0,
                     strehlList: List[Strehl]): List[(List[Strehl], Double, Double, Double)] = {

    val inst = ctx.getInstrument
    val savedPa = inst.getPosAngleDegrees
    val savedRa = ctx.getTargets.getBase.getTarget.getRa.getAs(Units.DEGREES)
    val savedDec = ctx.getTargets.getBase.getTarget.getDec.getAs(Units.DEGREES)
    val settingsList = settingsToTry(savedPa, savedRa, savedDec, posAngleTolerance, basePosTolerance)
    try {
      val result = for ((pa, ra, dec) <- settingsList) yield {
        inst.setPosAngle(pa)
        ctx.getTargets.getBase.getTarget.getRa.setAs(ra, Units.DEGREES)
        ctx.getTargets.getBase.getTarget.getDec.setAs(dec, Units.DEGREES)
        // XXX TODO use a cache map in the filter?
        val l = strehlList.filter(_.stars.forall(guideStarFilter))
        (l, pa, ra, dec)
      }
      result.toList
    }
    finally {
      // restore settings
      inst.setPosAngleDegrees(savedPa)
      ctx.getTargets.getBase.getTarget.getRa.setAs(savedRa, Units.DEGREES)
      ctx.getTargets.getBase.getTarget.getDec.setAs(savedDec, Units.DEGREES)
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

    println("XXX posAngles = " + posAngles)
    println("XXX basePos = " + ((ra, dec)))
    println("XXX posList = " + posList)

    val l = for (pa <- posAngles; pos <- posList) yield (pa, pos._1, pos._2)
    l.toList
  }
}
