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

  sealed abstract class Gaia(id: String, displayName: String) extends CatalogName(id, displayName) {

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

  }

  case object GaiaEsa extends Gaia("gaiaEsa", "GAIA @ ESA")

  case object GaiaGemini extends Gaia("gaiaGemini", "GAIA @ Gemini")

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
final case class ConeSearchCatalogQuery(
  id:                   Option[Int],
  base:                 Coordinates,
  radiusConstraint:     RadiusConstraint,
  magnitudeConstraints: List[MagnitudeConstraints],
  catalog:              CatalogName,
  isLgs:                Boolean
) extends CatalogQuery {

  val filters: NonEmptyList[QueryResultsFilter] =
    NonEmptyList(
      RadiusFilter(base, radiusConstraint),
      magnitudeConstraints.map(MagnitudeQueryFilter.apply): _*
    )

  override def filter(t: SiderealTarget): Boolean =
    filters.toList.forall(_.filter(t))

  private def closeEnough(c: ConeSearchCatalogQuery): Boolean = {
    // Angular separation, or distance between the two.
    val distance = Coordinates.difference(base, c.base).distance

    // Add the given query's outer radius limit to the distance to get the
    // maximum distance from this base position of any potential guide star.
    val max = distance + c.radiusConstraint.maxLimit

    // See whether the other base position falls out of range of our
    // radius limits.
    radiusConstraint.maxLimit >= max
  }

  // Compare the List[MagnitudeConstraints] with tolerance.
  private def closeMagnitudes(c: ConeSearchCatalogQuery): Boolean = {
    def closeMagConstraint(m0: MagConstraint, m1: MagConstraint): Boolean =
      (m0.brightness - m1.brightness).abs < 0.000001

    def closeOption[A](o0: Option[A], o1: Option[A])(f: (A, A) => Boolean) =
      (o0, o1) match {
        case (Some(a0), Some(a1)) => f(a0, a1)
        case (None,     None    ) => true
        case _                    => false
      }

    val m0 = magnitudeConstraints.groupBy(_.searchBands)
    val m1 = c.magnitudeConstraints.groupBy(_.searchBands)

    (m0.keySet == m1.keySet) && m0.forall {
      case (key, v0) =>
        (v0.size === m1(key).size) && v0.zip(m1(key)).forall { case (mc0, mc1) =>
          closeMagConstraint(mc0.faintnessConstraint, mc1.faintnessConstraint) &&
            closeOption(mc0.saturationConstraint, mc1.saturationConstraint)(closeMagConstraint)
        }
    }
  }

  override def isSuperSetOf(q: CatalogQuery): Boolean =
    q match {
      case c: ConeSearchCatalogQuery =>
        (q.catalog === catalog) && closeEnough(c) && closeMagnitudes(c)

      case _                         =>
        false
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

  def coneSearch(
    c:   edu.gemini.spModel.core.Coordinates,
    r:   RadiusConstraint,
    m:   MagnitudeConstraints,
    n:   CatalogName,
    lgs: Boolean = false
  ): CatalogQuery =
    ConeSearchCatalogQuery(None, c, r, List(m), n, lgs)

  def nameSearch(search: String): CatalogQuery =
    NameCatalogQuery(search, CatalogName.SIMBAD)

  implicit val equals: Equal[CatalogQuery] = Equal.equalA[CatalogQuery]
}
