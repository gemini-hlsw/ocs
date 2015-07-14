package edu.gemini.ags.gems

import edu.gemini.catalog.api._
import edu.gemini.catalog.votable._
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core.{Angle, MagnitudeBand, Coordinates}
import edu.gemini.spModel.gemini.gems.GemsInstrument
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.obs.context.ObsContext

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.math._

import scalaz._
import Scalaz._

import jsky.util.gui.StatusLogger

/**
 * Implements GeMS guide star search.
 * The catalog search will provide the inputs to the analysis phase, which actually assigns guide stars to guiders.
 * See OT-26
 */
case class GemsVoTableCatalog(backend: VoTableBackend = RemoteBackend, catalog: CatalogName = ucac4) {
  private val DefaultSaturationMagnitude = 0.0

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
  def search4Java(obsContext: ObsContext, basePosition: Coordinates, options: GemsGuideStarSearchOptions, nirBand: Option[MagnitudeBand], statusLogger: StatusLogger, timeout: Int = 10): java.util.List[GemsCatalogSearchResults] =
    Await.result(search(obsContext, basePosition, options, nirBand, statusLogger), timeout.seconds).asJava

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
    val criterions = options.searchCriteria(obsContext, nirBand).asScala.toList
    val inst = options.getInstrument

    val resultSequence = inst match {
      case GemsInstrument.flamingos2 => searchCatalog(basePosition, criterions, statusLogger)
      case i                         => searchOptimized(basePosition, obsContext.getConditions, criterions, i, statusLogger)
    }

    // sort on criteria order
    resultSequence.map(_.sortWith({
      case (x, y) =>
        criterions.indexOf(x.criterion) < criterions.indexOf(y.criterion)
    }))
  }

  private def searchCatalog(basePosition: Coordinates, criterions: List[GemsCatalogSearchCriterion], statusLogger: StatusLogger): Future[List[GemsCatalogSearchResults]] = {
    val queryArgs = for {
      c <- criterions
      q = CatalogQuery.catalogQuery(basePosition, c.criterion.radiusConstraint, c.criterion.magConstraint, catalog)
    } yield (q, c)

    val qm = queryArgs.toMap
    VoTableClient.catalogs(queryArgs.map(_._1), backend).map(l => l.collect {
      case QueryResult(q: CatalogQueryWithMagnitudeFilters, results:CatalogQueryResult) => GemsCatalogSearchResults(qm.get(q).get, results.targets.rows)
    })
  }

  /**
   * Searches the given catalogs for the given base position according to the given criteria.
   * This method attempts to merge the criteria to avoid multiple catalog queries and then
   * runs the catalog searches in parallel in background threads and notifies the
   * searchResultsListener when done.
   *
   * @param basePosition the base position to search for
   * @param criterions list of search criteria
   * @param inst the instrument option for the search
   * @return a list of threads used for background catalog searches
   */
  private def searchOptimized(basePosition: Coordinates, conditions: Conditions, criterions: List[GemsCatalogSearchCriterion], inst: GemsInstrument, statusLogger: StatusLogger): Future[List[GemsCatalogSearchResults]] = {
    val radiusConstraints = getRadiusConstraints(inst, criterions)
    val magConstraints = optimizeMagnitudeConstraints(criterions)

    val queries = for {
      radiusLimits <- radiusConstraints
      magLimits    <- magConstraints
    } yield CatalogQuery.catalogQuery(basePosition, radiusLimits, magLimits, catalog)

    VoTableClient.catalogs(queries, backend).flatMap {
      case l if l.exists(_.result.containsError) =>
        Future.failed(CatalogException(l.map(_.result.problems).suml))
      case l =>
        Future.successful {
          val targets = l.foldMap { _.result.targets.rows }
          assignToCriterion(basePosition, criterions, targets)
        }
    }
  }

  /**
   * Assign targets to matching criterions
   */
  private def assignToCriterion(basePosition: Coordinates, criterions: List[GemsCatalogSearchCriterion], targets: List[SiderealTarget]): List[GemsCatalogSearchResults] = {

    def matchCriteria(basePosition: Coordinates, criter: GemsCatalogSearchCriterion, targets: List[SiderealTarget]): List[SiderealTarget] = {
      val matcher = criter.criterion.matcher(basePosition)
      targets.filter(matcher.matches).distinct
    }

    for {
      c <- criterions
    } yield GemsCatalogSearchResults(c, matchCriteria(basePosition, c, targets))
  }

  // Returns a list of radius limits used in the criteria.
  // If inst is flamingos2, use separate limits, since the difference in size between the OIWFS and Canopus
  // areas is too large to get good results.
  // Otherwise, for GSAOI, merge the radius limits into one, since the Canopus and GSAOI radius are both about
  // 1 arcmin.
  protected [gems] def getRadiusConstraints(inst: GemsInstrument, criterions: List[GemsCatalogSearchCriterion]): List[RadiusConstraint] = {
    inst match {
      case GemsInstrument.flamingos2 => criterions.map(_.criterion.adjustedLimits)
      case _                         => optimizeRadiusConstraint(criterions).toList
    }
  }

  // Combines multiple radius limits into one
  protected [gems] def optimizeRadiusConstraint(criterList: List[GemsCatalogSearchCriterion]): Option[RadiusConstraint] = {
    criterList.nonEmpty option {
      val result = criterList.foldLeft((Double.MinValue, Double.MaxValue)) { (prev, current) =>
        val c = current.criterion
        val radiusConstraint = c.adjustedLimits
        val maxLimit = radiusConstraint.maxLimit
        val correctedMax = (c.offset |@| c.posAngle) { (o, _) =>
          // If an offset and pos angle were defined, normally an adjusted base position
          // would be used, however since we are merging queries here, use the original
          // base position and adjust the radius limits
          maxLimit + o.distance
        } | maxLimit
        (max(correctedMax.toDegrees, prev._1), min(radiusConstraint.minLimit.toDegrees, prev._2))
      }
      RadiusConstraint.between(Angle.fromDegrees(result._1), Angle.fromDegrees(result._2))
    }
  }

  // Sets the min/max magnitude limits in the given query arguments
  protected [gems] def optimizeMagnitudeConstraints(criterions: List[GemsCatalogSearchCriterion]): List[MagnitudeConstraints] = {
    val constraintsPerBand = criterions.map(_.criterion.magConstraint).groupBy(_.referenceBand).toList
    // Get max/min limits per band
    constraintsPerBand.flatMap {
      case (_, Nil) =>
        None
      case (_, h :: Nil) =>
        Some(h)
      case (_, h :: tail) =>
        tail.foldLeft(h.some) { (a, b) =>
          a >>= (_ union b)
        }
    }
  }
}
