package edu.gemini.catalog.api

import edu.gemini.spModel.core.{Magnitude, Coordinates}
import edu.gemini.spModel.core.Target.SiderealTarget

import scalaz._
import Scalaz._

sealed trait CatalogQuery {
  val id: Option[Int] = None
  val base: Coordinates
  val radiusConstraint: RadiusConstraint
  val magnitudeConstraints: Option[MagnitudeConstraints]
  val magnitudeRange: Option[MagnitudeRange]
  val catalog: CatalogName

  def filter: SiderealTarget => Boolean = (t) => radiusConstraint.targetsFilter(base)(t) && magnitudeConstraints.map(_.filter(t)).getOrElse(true)
  def filterOnMagnitude(t: SiderealTarget, m:Magnitude): Boolean = radiusConstraint.targetsFilter(base)(t) && magnitudeConstraints.map(_.filter(t)).getOrElse(true) && magnitudeRange.map(_.filter(t, m)).getOrElse(true)
  def filterOnMagnitude(t: SiderealTarget, m: Option[Magnitude]): Boolean = radiusConstraint.targetsFilter(base)(t) && magnitudeConstraints.map(_.filter(t)).getOrElse(true) && m.isDefined && magnitudeRange.map(_.filter(t, m.get)).getOrElse(true)

  def withMagnitudeConstraints(magnitudeConstraints: Option[MagnitudeConstraints]): CatalogQuery

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

object CatalogQuery {
  private case class BandConstrainedCatalogQuery(base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeConstraints: Option[MagnitudeConstraints], catalog: CatalogName) extends CatalogQuery {
    override val magnitudeRange: Option[MagnitudeRange] = None
    def withMagnitudeConstraints(magnitudeConstraints: Option[MagnitudeConstraints]): CatalogQuery = copy(magnitudeConstraints = magnitudeConstraints)
  }

  private case class RangeConstrainedCatalogQuery(base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeRange: Option[MagnitudeRange], catalog: CatalogName) extends CatalogQuery {
    override val magnitudeConstraints = None
    def withMagnitudeConstraints(magnitudeConstraints: Option[MagnitudeConstraints]): CatalogQuery = this
  }

  private case class GemsCatalogQuery(override val id: Option[Int], base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeConstraints: Option[MagnitudeConstraints], catalog: CatalogName) extends CatalogQuery {
    // FIXME Likely Gems should use a Magnitude Range rather than Magnitude Constraints
    override val magnitudeRange: Option[MagnitudeRange] = None
    def withMagnitudeConstraints(magnitudeConstraints: Option[MagnitudeConstraints]): CatalogQuery = copy(magnitudeConstraints = magnitudeConstraints)
  }

  // TODO Ensure that it makes sense to default to ucac4
  def catalogQuery(base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeConstraints: Option[MagnitudeConstraints], catalog: CatalogName = ucac4): CatalogQuery
    = BandConstrainedCatalogQuery(base, radiusConstraint, magnitudeConstraints, catalog)

  def catalogQueryWithoutBand(base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeRange: Option[MagnitudeRange], catalog: CatalogName = ucac4): CatalogQuery
    = RangeConstrainedCatalogQuery(base, radiusConstraint, magnitudeRange, catalog)

  def catalogQueryForGems(id: Int, base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeConstraints: Option[MagnitudeConstraints], catalog: CatalogName = ucac4): CatalogQuery
    = GemsCatalogQuery(Some(id), base, radiusConstraint, magnitudeConstraints, catalog)
}

sealed abstract class CatalogName(val id: String)

case object sdss extends CatalogName("sdss9")
case object gsc234 extends CatalogName("gsc234")
case object ppmxl extends CatalogName("ppmxl")
case object ucac4 extends CatalogName("ucac4")
case object twomass_psc extends CatalogName("twomass_psc")
case object twomass_xsc extends CatalogName("twomass_xsc")
