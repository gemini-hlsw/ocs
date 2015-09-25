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

/**
 * Represents a query on a catalog
 */
sealed trait CatalogQuery {
  val id: Option[Int]
  val catalog: CatalogName

  def filter(t: SiderealTarget):Boolean
  def isSuperSetOf(c: CatalogQuery):Boolean
}

/**
 * CatalogQuery using the ConeSearch method with additional constraints on radius and magnitude
 */
case class ConeSearchCatalogQuery(id: Option[Int], base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeConstraints: List[MagnitudeConstraints], catalog: CatalogName) extends CatalogQuery {

  val filters: NonEmptyList[QueryResultsFilter] = NonEmptyList(RadiusFilter(base, radiusConstraint), magnitudeConstraints.map(MagnitudeQueryFilter.apply): _*)
  override def filter(t: SiderealTarget):Boolean = filters.list.forall(_.filter(t))

  override def isSuperSetOf(q: CatalogQuery) = q match {
    case c: ConeSearchCatalogQuery =>
      // Angular separation, or distance between the two.
      val distance = Coordinates.difference(base, c.base).distance

      // Add the given query's outer radius limit to the distance to get the
      // maximum distance from this base position of any potential guide star.
      val max = distance + c.radiusConstraint.maxLimit

      // See whether the other base position falls out of range of our
      // radius limits.
      radiusConstraint.maxLimit >= max
    case _ => false
  }
}

object ConeSearchCatalogQuery {
  implicit val equals = Equal.equalA[ConeSearchCatalogQuery]
}

/**
 * Name based query, typically without filtering
 */
case class NameCatalogQuery(search: String, catalog: CatalogName) extends CatalogQuery {
  override val id: Option[Int] = None
  def filter(t: SiderealTarget) = true
  def isSuperSetOf(c: CatalogQuery) = false
}

object CatalogQuery {
  // Useful constructors
  def apply(id: Int, base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeConstraints: List[MagnitudeConstraints], catalog: CatalogName):CatalogQuery = ConeSearchCatalogQuery(id.some, base, radiusConstraint, magnitudeConstraints, catalog)

  def apply(base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeConstraints: List[MagnitudeConstraints], catalog: CatalogName):CatalogQuery = ConeSearchCatalogQuery(None, base, radiusConstraint, magnitudeConstraints, catalog)

  def apply(base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeConstraints: MagnitudeConstraints, catalog: CatalogName):CatalogQuery = ConeSearchCatalogQuery(None, base, radiusConstraint, List(magnitudeConstraints), catalog)

  def apply(base: Coordinates, radiusConstraint: RadiusConstraint, catalog: CatalogName):CatalogQuery = ConeSearchCatalogQuery(None, base, radiusConstraint, Nil, catalog)

  def apply(search: String):CatalogQuery = NameCatalogQuery(search, simbad)

  implicit val equals = Equal.equalA[CatalogQuery]
}

sealed abstract class CatalogName(val id: String)

case object sdss extends CatalogName("sdss9")
case object gsc234 extends CatalogName("gsc234")
case object ppmxl extends CatalogName("ppmxl")
case object ucac4 extends CatalogName("ucac4")
case object twomass_psc extends CatalogName("twomass_psc")
case object twomass_xsc extends CatalogName("twomass_xsc")
case object simbad extends CatalogName("simbad")
