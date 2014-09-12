package edu.gemini.ags.gems.mascot

import jsky.coords.{WorldCoords, CoordinateRadius}
import jsky.catalog.{TableQueryResult, QueryArgs, BasicQueryArgs}
import edu.gemini.catalog.skycat.table._
import edu.gemini.ags.gems.mascot.MascotConf._
import edu.gemini.shared.skyobject.{Magnitude, SkyObject}
import jsky.catalog.skycat.{SkyObjectFactoryRegistrar, SkycatConfigFile}
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
      print(", [%s]".format(new WorldCoords(s.stars(i).ra, s.stars(i).dec)))
    }
    println("\nStrehl over %.1f\": avg=%.1f  rms=%.1f  min=%.1f  max=%.1f\n" format (
      s.halffield * 2, s.avgstrehl * 100, s.rmsstrehl * 100, s.minstrehl * 100, s.maxstrehl * 100))
  }


  /**
   * Finds the best asterisms by querying the default PPMXL catalog for the given coordinates and radius.
   *
   * @param coords coordinates of the center point of a cone query
   * @param bandpass determines which magnitudes are used in the calculations:
   *        (one of "B", "V", "R", "J", "H", "K"). Default: "R"
   * @param factor multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
   * @param progress a function(strehl, count, total) called for each asterism as it is calculated.
   *        By default, prints to stdout
   * @param filter a filter function that returns false if the Star should be excluded.
   *        The default filter always returns true.
   * @param maxRadius max radius from center coordinates in arcmin (default 1.2)
   * @param minRadius min radius from center (default 0)
   * @param maxRows max number of rows to return from the catalog query: default: 1000
   * @return a tuple: (list of stars actually used, list of asterisms found)
   */
  def findBestAsterism(coords: WorldCoords,
                       catName: String = defaultCatalogName,
                       bandpass: String = Mascot.defaultBandpass,
                       factor: Double = Mascot.defaultFactor,
                       progress: (Strehl, Int, Int) => Unit = defaultProgress,
                       filter: Star => Boolean = Mascot.defaultFilter,
                       maxRadius: Double = defaultMaxRadius,
                       minRadius: Double = defaultMinRadius,
                       maxRows: Int = defaultMaxRows)
  : (List[Star], List[Strehl]) = {
    val configFile = SkycatConfigFile.getConfigFile
    val cat = configFile.getCatalog(catName)
    assert(cat != null)
    val queryArgs = new BasicQueryArgs(cat)
    val region = new CoordinateRadius(coords, minRadius, maxRadius)
    queryArgs.setRegion(region)
    queryArgs.setMaxRows(maxRows)
    findBestAsterismByQueryArgs(queryArgs, bandpass, factor, progress, filter)
  }


  /**
   * Finds the best asterisms for the results of the given catalog query.
   * @param queryArgs describes a catalog query
   * @param bandpass determines which magnitudes are used in the calculations: (one of "B", "V", "R", "J", "H", "K")
   * @param factor multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
   * @param progress a function(strehl, count, total) called for each asterism as it is calculated
   * @param filter a filter function that returns false if the Star should be excluded
   * @return a tuple: (list of stars actually used, list of asterisms found)
   */
  def findBestAsterismByQueryArgs(queryArgs: QueryArgs,
                       bandpass: String = Mascot.defaultBandpass,
                       factor: Double = Mascot.defaultFactor,
                       progress: (Strehl, Int, Int) => Unit = defaultProgress,
                       filter: Star => Boolean = Mascot.defaultFilter)
  : (List[Star], List[Strehl]) = {
    val cat = queryArgs.getCatalog
    val queryResult = cat.query(queryArgs)
    findBestAsterismInQueryResult(queryResult.asInstanceOf[TableQueryResult], bandpass, factor, progress, filter)
  }

  /**
   * Finds the best asterisms for the given table of stars.
   * @param queryResult unfiltered table of data from a catalog query
   * @param bandpass determines which magnitudes are used in the calculations: (one of "B", "V", "R", "J", "H", "K")
   * @param factor multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
   * @param progress a function(strehl, count, total) called for each asterism as it is calculated
   * @param filter a filter function that returns false if the Star should be excluded
   * @return a tuple: (list of stars actually used, list of asterisms found)
   */
  def findBestAsterismInQueryResult(queryResult: TableQueryResult,
                       bandpass: String = Mascot.defaultBandpass,
                       factor: Double = Mascot.defaultFactor,
                       progress: (Strehl, Int, Int) => Unit = defaultProgress,
                       filter: Star => Boolean = Mascot.defaultFilter)
  : (List[Star], List[Strehl]) = {
    val rowCount = queryResult.getRowCount
    val skyObjectFactory = getSkyObjectFactory(queryResult.getCatalog.getId)
    val dataVector = queryResult.getDataVector
    val center = queryResult.getQueryArgs.getRegion.getCenterPosition
    val list = for (i <- 0 until rowCount) yield {
      toSkyObject(queryResult, dataVector.get(i).asInstanceOf[java.util.Vector[Object]], skyObjectFactory)
    }
    findBestAsterismInSkyObjectList(list.toList, center.getX, center.getY, bandpass, factor, progress, filter)
  }

  /**
   * Finds the best asterisms for the given list of SkyObjects.
   * @param list the list of SkyObjects to use
   * @param centerRA the base position RA coordinate
   * @param centerDec the base position Dec coordinate
   * @param bandpass determines which magnitudes are used in the calculations: (one of "B", "V", "R", "J", "H", "K")
   * @param factor multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
   * @param progress a function(strehl, count, total) called for each asterism as it is calculated
   * @param filter a filter function that returns false if the Star should be excluded
   * @return a tuple: (list of stars actually used, list of asterisms found)
   */
  def findBestAsterismInSkyObjectList(list: List[SkyObject],
                       centerRA: Double, centerDec: Double,
                       bandpass: String = Mascot.defaultBandpass,
                       factor: Double = Mascot.defaultFactor,
                       progress: (Strehl, Int, Int) => Unit = defaultProgress,
                       filter: Star => Boolean = Mascot.defaultFilter)
  : (List[Star], List[Strehl]) = {
    val starList = for (skyObject <- list) yield {
      val name = skyObject.getName
      val coords = skyObject.getCoordinates.toHmsDeg(0L)
      val ra = coords.getRa.toDegrees.getMagnitude
      val dec = coords.getDec.toDegrees.getMagnitude
      val bmag = getMagnitudeValue(skyObject, Magnitude.Band.B)
      val vmag = getMagnitudeValue(skyObject, Magnitude.Band.V)
      val rmag = getMagnitudeValue(skyObject, Magnitude.Band.R)
      val jmag = getMagnitudeValue(skyObject, Magnitude.Band.J)
      val hmag = getMagnitudeValue(skyObject, Magnitude.Band.H)
      val kmag = getMagnitudeValue(skyObject, Magnitude.Band.K)
      Star.makeStar(name, centerRA, centerDec, bmag, vmag, rmag, jmag, hmag, kmag, ra, dec)
    }
    Mascot.findBestAsterism(starList.toList, bandpass, factor, progress, filter)
  }

  case class StrehlResults(starList: java.util.List[Star], strehlList: java.util.List[Strehl])

  /**
   * Finds the best asterisms for the given list of SkyObjects
   * (This version is easier to call from Java).
   * @param javaList the list of SkyObjects to use
   * @param centerRA the base position RA coordinate
   * @param centerDec the base position Dec coordinate
   * @param bandpass determines which magnitudes are used in the calculations: (one of "B", "V", "R", "J", "H", "K")
   * @param mascotProgress optional, called for each asterism as it is calculated, can cancel the calculations by returning false
   * @return a tuple: (list of stars actually used, list of asterisms found)
   */
  def javaFindBestAsterismInSkyObjectList(javaList: java.util.List[SkyObject],
                       centerRA: Double, centerDec: Double,
                       bandpass: String, factor: Double,
                       mascotProgress: MascotProgress): StrehlResults = {

    val progress = (s: Strehl, count: Int, total: Int) => {
      defaultProgress(s, count, total)
      if (mascotProgress != null && !mascotProgress.progress(s, count, total, true)) {
        throw new CancellationException("Canceled")
      }
    }
    val list = JavaConversions.asScalaBuffer(javaList).toList
    val (starList, strehlList) = findBestAsterismInSkyObjectList(list, centerRA, centerDec, bandpass, factor, progress,
      Mascot.defaultFilter)
    new StrehlResults(starList, strehlList)
  }


  // Returns the magnitude for the given band, if valid, otherwise invalidMag
  private def getMagnitudeValue(skyObject: SkyObject, band: Magnitude.Band): Double = {
    val mag = skyObject.getMagnitude(band)
    if (mag.isEmpty) invalidMag else mag.getValue.getBrightness
  }

  // Returns the SkyObject for the given catalog name
  private def getSkyObjectFactory(name: String): SkyObjectFactory = {
    val fact = SkyObjectFactoryRegistrar.instance.lookup(name)
    if (fact.isEmpty) {
      throw new IllegalArgumentException("Unsupported catalog: " + name)
    }
    fact.getValue
  }

  // Creates a SkyObject, if possible, using the given table query result
  // and row of data.
  private def toSkyObject(table: TableQueryResult, row: java.util.Vector[Object], fact: SkyObjectFactory): SkyObject = {
    val columnIdentifiers = table.getColumnIdentifiers
    val cat = MascotCatJavaUtils.wrap(columnIdentifiers, row)
    fact.create(cat._1, cat._2)
  }
}
