package edu.gemini.ags.gems

import edu.gemini.catalog.api._
import edu.gemini.catalog.votable.{QueryResult, VoTableClient}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core.{Magnitude, MagnitudeBand, Coordinates}
import edu.gemini.spModel.gemini.gems.GemsInstrument
import edu.gemini.spModel.obs.context.ObsContext

import scala.collection.immutable.TreeSet
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scalaz._
import Scalaz._

import jsky.util.gui.StatusLogger

object GemsVoTableCatalog {
  val DefaultSaturationMagnitude = 0.0

  /**
   * Searches for the given base position according to the given options.
   * Multiple queries are performed in parallel in background threads.
   * This method is synchronous and can be used form the Java side of the OT
   *
   * @param obsContext   the context of the observation (needed to adjust for selected conditions)
   * @param basePosition the base position to search for
   * @param options      the search options
   * @param nirBand      optional NIR magnitude band (default is H)
   * @param timeout      Timeout in seconds
   * @return list of search results
   */
  def search4Java(obsContext: ObsContext, basePosition: Coordinates, options: GemsGuideStarSearchOptions, nirBand: Option[MagnitudeBand], statusLogger: StatusLogger, timeout: Int = 10): java.util.List[GemsCatalogSearchResults] = {
    import scala.collection.JavaConverters._
    Await.result(search(obsContext, basePosition, options, nirBand, statusLogger), timeout.seconds).asJava
  }
    /**

   * Searches for the given base position according to the given options.
   * Multiple queries are performed in parallel in background threads.
   *
   * @param obsContext   the context of the observation (needed to adjust for selected conditions)
   * @param basePosition the base position to search for
   * @param options      the search options
   * @param nirBand      optional NIR magnitude band (default is H)
   * @return  Future with a list of search results
   */
  def search(obsContext: ObsContext, basePosition: Coordinates, options: GemsGuideStarSearchOptions, nirBand: Option[MagnitudeBand], statusLogger: StatusLogger): Future[List[GemsCatalogSearchResults]] = {

    import scala.collection.JavaConverters._
    val criterList = options.searchCriteria(obsContext, nirBand).asScala.toList
    val inst = options.getInstrument
    val resultSequence = inst match {
      case GemsInstrument.flamingos2 => searchCatalog(basePosition, criterList, statusLogger)
      case i                         => searchOptimized(basePosition, criterList, i, statusLogger)
    }
    // flatten by criteria
    val map = for {
      r <- resultSequence
    } yield r.groupBy(_.criterion).map {
        case (q, v) => GemsCatalogSearchResults(q, v.map(_.results).suml)
      }
    // sort on criteria order
    map.map(_.toList.sortWith({
      case (x, y) =>
        criterList.indexOf(x.criterion) < criterList.indexOf(y.criterion)
    }))
  }

  private def searchCatalog(basePosition: Coordinates, criterList: List[GemsCatalogSearchCriterion], statusLogger: StatusLogger): Future[List[GemsCatalogSearchResults]] = {
    val queryArgs = for {
      c <- criterList
      q = CatalogQuery(basePosition, c.criterion.radiusLimits, c.criterion.magLimits.some)
    } yield (q, c)
    val qm = queryArgs.toMap
    VoTableClient.catalog(queryArgs.map(_._1)).map(l => l.map(k => GemsCatalogSearchResults(qm.get(k.query).get, k.result.targets.rows)))
  }

  /**
   * Searches the given catalogs for the given base position according to the given criteria.
   * This method attempts to merge the criteria to avoid multiple catalog queries and then
   * runs the catalog searches in parallel in background threads and notifies the
   * searchResultsListener when done.
   *
   * @param basePosition the base position to search for
   * @param criterList list of search criteria
   * @param inst the instrument option for the search
   * @return a list of threads used for background catalog searches
   */
  private def searchOptimized(basePosition: Coordinates, criterList: List[GemsCatalogSearchCriterion], inst: GemsInstrument, statusLogger: StatusLogger): Future[List[GemsCatalogSearchResults]] = {
    val radiusLimitsList = getRadiusLimits(inst, criterList)
    val magLimitsList = optimizeMagnitudeLimits(criterList)

    val queries = for {
      radiusLimits <- radiusLimitsList
      magLimits <- magLimitsList
      queryArgs = CatalogQuery(basePosition, radiusLimits, magLimits.some)
    } yield queryArgs
    VoTableClient.catalog(queries).map(l => {
      val targets = l.map(k => k.result.targets).suml
      filter(basePosition, criterList, targets.rows)
    })
  }

  private def filter(basePosition: Coordinates, criterList: List[GemsCatalogSearchCriterion], targets: List[SiderealTarget]): List[GemsCatalogSearchResults] = {
    for {
      c <- criterList
    } yield GemsCatalogSearchResults(c, matchCriteria(basePosition, c, targets))
  }

  private def matchCriteria(basePosition: Coordinates, criter: GemsCatalogSearchCriterion, targets: List[SiderealTarget]): List[SiderealTarget] = {
    val matcher = criter.criterion.matcher(basePosition)
    targets.filter(matcher.matches).distinct
  }

  // Returns a list of radius limits used in the criteria.
  // If inst is flamingos2, use separate limits, since the difference in size between the OIWFS and Canopus
  // areas is too large to get good results.
  // Otherwise, for GSAOI, merge the radius limits into one, since the Canopus and GSAOI radius are both about
  // 1 arcmin.
  protected [gems] def getRadiusLimits(inst: GemsInstrument, criterList: List[GemsCatalogSearchCriterion]): List[RadiusConstraint] = {
    import scala.collection.JavaConverters._
    inst match {
      case GemsInstrument.flamingos2 => criterList.map(_.criterion.adjustedLimits)
      case _                         => List(GemsUtils4Java.optimizeRadiusConstraint(criterList.asJava))
    }
  }

  // Sets the min/max magnitude limits in the given query arguments
  protected [gems] def optimizeMagnitudeLimits(criterList: List[GemsCatalogSearchCriterion]): List[MagnitudeConstraints] = {
    // Calculate the max faintess per band out of the criteria
    val faintLimitPerBand = for {
      criteria <- criterList
      m = criteria.criterion.magLimits
      b = m.band
      fl = m.faintnessConstraint
    } yield (b, fl)
    val faintnessMap:Map[MagnitudeBand, FaintnessConstraint] = faintLimitPerBand.groupBy(_._1).map { case (_, v) => v.maxBy(_._2)(FaintnessConstraint.order.toScalaOrdering)}

    // Calculate the min saturation limit per band out of the criteria
    val saturationLimitPerBand = for {
        criteria <- criterList
        m = criteria.criterion.magLimits
        b = m.band
        sl = m.saturationConstraint.getOrElse(SaturationConstraint(DefaultSaturationMagnitude))
      } yield (b, sl)
    val saturationMap = saturationLimitPerBand.groupBy(_._1).map { case (_, v) => v.minBy(_._2)(SaturationConstraint.order.toScalaOrdering)}

    (for {
      b <- faintnessMap
      mc = if (saturationMap.contains(b._1)) MagnitudeConstraints(b._1, b._2, saturationMap.get(b._1)) else new MagnitudeConstraints(new Magnitude(b._2.brightness, b._1))
    } yield MagnitudeConstraints(b._1, b._2, saturationMap.get(b._1))).toList
  }
}