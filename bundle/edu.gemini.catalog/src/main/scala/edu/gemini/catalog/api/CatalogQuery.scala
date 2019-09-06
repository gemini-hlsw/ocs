package edu.gemini.catalog.api

import edu.gemini.spModel.core.{MagnitudeBand, Coordinates, SiderealTarget, VersionToken}

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

sealed abstract class CatalogName(val id: String, val displayName: String) extends Product with Serializable {

  def supportedBands: List[MagnitudeBand] =
    Nil

  // Indicates what is the band used when a generic R band is required
  def rBand: MagnitudeBand =
    MagnitudeBand.UC

  def voTableVersion: VersionToken =
    VersionToken.unsafeFromIntegers(1, 2)

}

object CatalogName {

  case object SDSS extends CatalogName("sdss9", "SDSS9 @ Gemini")

  case object GSC234 extends CatalogName("gsc234", "GSC234 @ Gemini")

  case object PPMXL extends CatalogName("ppmxl", "PPMXL @ Gemini") {
    override val supportedBands = List(MagnitudeBand.B, MagnitudeBand.R, MagnitudeBand.I, MagnitudeBand.J, MagnitudeBand.H, MagnitudeBand.K)
    override val rBand: MagnitudeBand = MagnitudeBand.R
  }

  case object UCAC4  extends CatalogName("ucac4", "UCAC4 @ Gemini") {
    override val  supportedBands = List(MagnitudeBand._g, MagnitudeBand._r, MagnitudeBand._i, MagnitudeBand.B, MagnitudeBand.V, MagnitudeBand.UC, MagnitudeBand.J, MagnitudeBand.H, MagnitudeBand.K)
  }

  case object Gaia extends CatalogName("gaia", "GAIA @ ESA") {

    // Gaia bands (why isn't this a Set?)
    override val supportedBands: List[MagnitudeBand] =
      List(
        MagnitudeBand.V,
        MagnitudeBand.R,
        MagnitudeBand.I,
        MagnitudeBand._r,
        MagnitudeBand._i,
        MagnitudeBand._g,
        MagnitudeBand.K,
        MagnitudeBand.H,
        MagnitudeBand.J
      )

    override val rBand: MagnitudeBand =
      MagnitudeBand.R

    override val voTableVersion: VersionToken =
      VersionToken.unsafeFromIntegers(1, 3)

  }

  case object TWOMASS_PSC extends CatalogName("twomass_psc", "TwoMass PSC @ Gemini")

  case object TWOMASS_XSC extends CatalogName("twomass_xsc", "TwoMass XSC @ Gemini")

  case object SIMBAD extends CatalogName("simbad", "Simbad")

  implicit val CatalogNameEquals: Equal[CatalogName] =
    Equal.equal[CatalogName]((a, b) => a.id === b.id)
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
  override def filter(t: SiderealTarget):Boolean = filters.toList.forall(_.filter(t))

  override def isSuperSetOf(q: CatalogQuery): Boolean = q match {
    case c: ConeSearchCatalogQuery =>

      // Angular separation, or distance between the two.
      val distance = Coordinates.difference(base, c.base).distance

      // Add the given query's outer radius limit to the distance to get the
      // maximum distance from this base position of any potential guide star.
      val max = distance + c.radiusConstraint.maxLimit

      // See whether the other base position falls out of range of our
      // radius limits.
      radiusConstraint.maxLimit >= max && q.catalog === catalog
    case _ => false
  }
}

object ConeSearchCatalogQuery {
  implicit val equals: Equal[ConeSearchCatalogQuery] = Equal.equalA[ConeSearchCatalogQuery]
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

  def apply(search: String):CatalogQuery = NameCatalogQuery(search, CatalogName.SIMBAD)

  implicit val equals: Equal[CatalogQuery] = Equal.equalA[CatalogQuery]
}
