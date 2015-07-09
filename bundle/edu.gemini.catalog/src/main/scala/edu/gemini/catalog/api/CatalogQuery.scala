package edu.gemini.catalog.api

import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.core.Target.SiderealTarget

import scalaz._
import Scalaz._

trait QueryResultsFilter {
  def filter(t: SiderealTarget): Boolean
}

case class RadiusFilter(base: Coordinates, rc: RadiusConstraint) extends QueryResultsFilter {
  def filter(t: SiderealTarget): Boolean = rc.targetsFilter(base)(t)
}

case class MagnitudeQueryFilter(mc: MagnitudeConstraints) extends QueryResultsFilter {
  def filter(t: SiderealTarget): Boolean = mc.filter(t)
}

case class AdjustedMagnitudeValueFilter(mr: MagnitudeConstraints, rangeAdjustment: MagnitudeRangeAdjustment) extends QueryResultsFilter {
  def filter(t: SiderealTarget): Boolean = rangeAdjustment(mr).filter(t)
}

sealed trait CatalogQuery {
  val id: Option[Int]
  val base: Coordinates
  val radiusConstraint: RadiusConstraint
  val catalog: CatalogName

  val filters: List[QueryResultsFilter]
  def filter(t: SiderealTarget):Boolean = filters.forall(_.filter(t))

  def isSuperSetOf(c: CatalogQuery) = {
    // Angular separation, or distance between the two.
    val distance = Coordinates.difference(base, c.base).distance

    // Add the given query's outer radius limit to the distance to get the
    // maximum distance from this base position of any potential guide star.
    val max = distance + c.radiusConstraint.maxLimit

    // See whether the other base position falls out of range of our
    // radius limits.
    radiusConstraint.maxLimit > max
  }
}

trait CatalogQueryWithRange extends CatalogQuery { this: CatalogQuery =>
  val magnitudeConstraints: MagnitudeConstraints
}

object CatalogQuery {
  private case class BandConstrainedCatalogQuery(id: Option[Int], base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeConstraints: MagnitudeConstraints, catalog: CatalogName) extends CatalogQueryWithRange {
    override val filters: List[QueryResultsFilter] = List(RadiusFilter(base, radiusConstraint), MagnitudeQueryFilter(magnitudeConstraints))
  }

  private case class RangeConstrainedCatalogQuery(id: Option[Int], base: Coordinates, radiusConstraint: RadiusConstraint, catalog: CatalogName) extends CatalogQuery {
    override val filters: List[QueryResultsFilter] = List(RadiusFilter(base, radiusConstraint))
  }

  /*private case class CatalogQueryWithAdjustedBand(override val id: Option[Int], base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeConstraints: MagnitudeConstraints, rangeAdjustment: MagnitudeRangeAdjustment, catalog: CatalogName) extends CatalogQuery with CatalogQueryWithRange {
    override val filters: List[QueryResultsFilter] = List(RadiusFilter(base, radiusConstraint), AdjustedMagnitudeValueFilter(magnitudeConstraints, rangeAdjustment))
  }*/

  /**
   * Builds a catalog query with a radius constraint
   *
   * @param base Base Coordinates
   * @param radiusConstraint Radius to restrict the search
   * @param catalog Catalog used to search targets
   */
  def catalogQuery(base: Coordinates, radiusConstraint: RadiusConstraint, catalog: CatalogName): CatalogQuery
    = RangeConstrainedCatalogQuery(None, base, radiusConstraint, catalog)

  /**
   * Builds a catalog query with a radius constraint and constraints on a single band
   *
   * @param base Base Coordinates
   * @param radiusConstraint Radius to restrict the search
   * @param magnitudeConstraints Constraint on targets on a particular band
   * @param catalog Catalog used to search targets
   */
  def catalogQuery(base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeConstraints: MagnitudeConstraints, catalog: CatalogName): CatalogQuery
    = BandConstrainedCatalogQuery(None, base, radiusConstraint, magnitudeConstraints, catalog)

  def catalogQuery(id: Int, base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeConstraints: MagnitudeConstraints, catalog: CatalogName): CatalogQuery
    = BandConstrainedCatalogQuery(id.some, base, radiusConstraint, magnitudeConstraints, catalog)

  /**
   * Builds a catalog query with a radius constraint, a range applied to a dynamically extracted band and supporting adjustments to the range
   *
   * @param base Base Coordinates
   * @param radiusConstraint Radius to restrict the search
   * @param magnitudeConstraint Range of values for the magnitude checks
   * @param rangeAdjustment Function that can modify the range for a used for a given query, e.g. updates based on conditions
   * @param catalog Catalog used to search targets
   */
  /*def catalogQueryWithAdjustedRange(base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeConstraint: MagnitudeConstraints, rangeAdjustment: MagnitudeRangeAdjustment, catalog: CatalogName = ucac4): CatalogQueryWithRange
    = CatalogQueryWithAdjustedBand(None, base, radiusConstraint, magnitudeConstraint, rangeAdjustment, catalog)*/

  /**
   * Builds a catalog query with a radius constraint, a range applied to a dynamically extracted band and supporting adjustments to the range
   * This call is used by GEMS and takes an ID used to index parallel queries
   *
   * @param id QueryId
   * @param base Base Coordinates
   * @param radiusConstraint Radius to restrict the search
   * @param magnitudeConstraints Range of values for the magnitude checks
   * @param rangeAdjustment Function that can modify the range for a used for a given query, e.g. updates based on conditions
   * @param catalog Catalog used to search targets
   */
  /*def catalogQueryForGems(id: Int, base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeConstraints: MagnitudeConstraints, rangeAdjustment: MagnitudeRangeAdjustment, catalog: CatalogName = ucac4): CatalogQueryWithRange
    = CatalogQueryWithAdjustedBand(id.some, base, radiusConstraint, magnitudeConstraints, rangeAdjustment, catalog)*/
}

sealed abstract class CatalogName(val id: String)

case object sdss extends CatalogName("sdss9")
case object gsc234 extends CatalogName("gsc234")
case object ppmxl extends CatalogName("ppmxl")
case object ucac4 extends CatalogName("ucac4")
case object twomass_psc extends CatalogName("twomass_psc")
case object twomass_xsc extends CatalogName("twomass_xsc")
