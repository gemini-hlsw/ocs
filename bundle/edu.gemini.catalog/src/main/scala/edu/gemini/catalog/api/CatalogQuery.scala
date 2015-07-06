package edu.gemini.catalog.api

import edu.gemini.spModel.core.{MagnitudeBand, Magnitude, Coordinates}
import edu.gemini.spModel.core.Target.SiderealTarget

import scalaz._
import Scalaz._

trait QueryResultsFilter {
  def filter(t: SiderealTarget): Boolean
}

case class RadiusFilter(base: Coordinates, rc: RadiusConstraint) extends QueryResultsFilter {
  def filter(t: SiderealTarget): Boolean = rc.targetsFilter(base)(t)
}

case class MagnitudeFilter(mc: Option[MagnitudeConstraints]) extends QueryResultsFilter {
  def filter(t: SiderealTarget): Boolean = mc.map(_.filter(t)).getOrElse(true)
}

case class MagnitudeValueFilter(m: Option[Magnitude], mr: Option[MagnitudeRange]) extends QueryResultsFilter {
  def filter(t: SiderealTarget): Boolean = m.exists(k => mr.map(_.filter(t, k)).getOrElse(true))
}

case class AdjustedMagnitudeValueFilter(m: Option[Magnitude], mr: Option[MagnitudeRange], rangeAdjustment: (Option[MagnitudeRange], Magnitude) => Option[MagnitudeRange]) extends QueryResultsFilter {
  def filter(t: SiderealTarget): Boolean = m.exists(k => rangeAdjustment(mr, k).map(_.filter(t, k)).getOrElse(true))
}

sealed trait CatalogQuery {
  val id: Option[Int] = None
  val base: Coordinates
  val radiusConstraint: RadiusConstraint
  val catalog: CatalogName
  // TODO Move these filters out of CatalogQuery
  val magnitudeConstraints: Option[MagnitudeConstraints]
  val magnitudeRange: Option[MagnitudeRange]
  //val filters: List[QueryResultsFilter] = List(RadiusFilter(base, radiusConstraint), MagnitudeFilter(magnitudeConstraints))

  //def filter: SiderealTarget => Boolean = (t) => filters.forall(_.filter(t))
  //def filter: SiderealTarget => Boolean = (t) => radiusConstraint.targetsFilter(base)(t) && magnitudeConstraints.map(_.filter(t)).getOrElse(true)
  def filter: SiderealTarget => Boolean = (t) => RadiusFilter(base, radiusConstraint).filter(t) && MagnitudeFilter(magnitudeConstraints).filter(t)
  def filterOnMagnitude(t: SiderealTarget, m:Option[Magnitude], rangeAdjustment: (Option[MagnitudeRange], Magnitude) => Option[MagnitudeRange]): Boolean =
    RadiusFilter(base, radiusConstraint).filter(t) && MagnitudeFilter(magnitudeConstraints).filter(t) &&
      AdjustedMagnitudeValueFilter(m, magnitudeRange, rangeAdjustment).filter(t)

  def withMagnitudeConstraints(magnitudeConstraints: Option[MagnitudeConstraints]): CatalogQuery = this
  def withMagnitudeRange(magnitudeRange: Option[MagnitudeRange]): CatalogQuery = this

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
    override def withMagnitudeConstraints(magnitudeConstraints: Option[MagnitudeConstraints]): CatalogQuery = copy(magnitudeConstraints = magnitudeConstraints)
  }

  private case class RangeConstrainedCatalogQuery(base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeRange: Option[MagnitudeRange], catalog: CatalogName) extends CatalogQuery {
    override val magnitudeConstraints = None
  }

  private case class RangeConstrainedCatalogQueryWithBand(base: Coordinates, radiusConstraint: RadiusConstraint, mag: SiderealTarget => Option[Magnitude], magnitudeRange: Option[MagnitudeRange], catalog: CatalogName) extends CatalogQuery {
    override val magnitudeConstraints = None
    override def filter: SiderealTarget => Boolean = (t) => {
      RadiusFilter(base, radiusConstraint).filter(t) && mag(t).exists(m => MagnitudeValueFilter(m.some, magnitudeRange).filter(t))
    }
  }

  private case class GemsCatalogQuery(override val id: Option[Int], base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeRange: Option[MagnitudeRange], catalog: CatalogName) extends CatalogQuery {
    override val magnitudeConstraints: Option[MagnitudeConstraints] = None
    override def withMagnitudeRange(magnitudeRange: Option[MagnitudeRange]): CatalogQuery = copy(magnitudeRange = magnitudeRange)
  }

  // TODO Ensure that it makes sense to default to ucac4
  def catalogQuery(base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeConstraints: Option[MagnitudeConstraints], catalog: CatalogName = ucac4): CatalogQuery
    = BandConstrainedCatalogQuery(base, radiusConstraint, magnitudeConstraints, catalog)

  def catalogQueryWithoutBand(base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeRange: Option[MagnitudeRange], catalog: CatalogName = ucac4): CatalogQuery
    = RangeConstrainedCatalogQuery(base, radiusConstraint, magnitudeRange, catalog)

  def catalogQueryRangeOnBand(base: Coordinates, radiusConstraint: RadiusConstraint, mag: SiderealTarget => Option[Magnitude], magnitudeRange: Option[MagnitudeRange], catalog: CatalogName = ucac4): CatalogQuery
    = RangeConstrainedCatalogQueryWithBand(base, radiusConstraint, mag, magnitudeRange, catalog)

  def catalogQueryForGems(id: Int, base: Coordinates, radiusConstraint: RadiusConstraint, magnitudeRange: Option[MagnitudeRange], catalog: CatalogName = ucac4): CatalogQuery
    = GemsCatalogQuery(Some(id), base, radiusConstraint, magnitudeRange, catalog)
}

sealed abstract class CatalogName(val id: String)

case object sdss extends CatalogName("sdss9")
case object gsc234 extends CatalogName("gsc234")
case object ppmxl extends CatalogName("ppmxl")
case object ucac4 extends CatalogName("ucac4")
case object twomass_psc extends CatalogName("twomass_psc")
case object twomass_xsc extends CatalogName("twomass_xsc")
