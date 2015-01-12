package edu.gemini.ags.gems

import edu.gemini.catalog.api.{MagnitudeConstraints, RadiusConstraint}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core.{Magnitude, Coordinates, Offset, Angle}
import edu.gemini.shared.skyobject
import edu.gemini.spModel.gems.{GemsGuideProbeGroup, GemsGuideStarType}
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

/**
 * Used to query catalogs and filter and categorize query results.
 * See OT-20
 */
case class CatalogSearchCriterion(name: String, magLimits: MagnitudeConstraints, radiusLimits: RadiusConstraint, offset: Option[Offset] = None, posAngle: Option[Angle] = None) {

  /**
   * If offset and pos angle are specified, then we want the coordinates of the
   * offset position when rotated for the position angle.
   *
   * @param base the base position
   * @return the adjusted base position (base + (offset position rotated by position angle))
   */
  def adjustedBase(base: Coordinates): Coordinates = {
    (offset |@| posAngle) { (off, a) =>
      val pa = a.toRadians
      if (pa != 0.0) {
        val p = off.p.toDegrees
        val q = off.q.toDegrees
        val cosa = Math.cos(pa)
        val sina = Math.sin(pa)
        val ra = p * cosa + q * sina
        val dec = -p * sina + q * cosa
        val raAngle = base.ra.offset(Angle.fromDegrees(ra))
        val decAngle = base.dec.offset(Angle.fromDegrees(dec))
        Coordinates(raAngle, decAngle._1).some
      } else {
        none[Coordinates]
      }
    }.flatten | base
  }

  /**
   * If there is an offset but there isn't a posAngle, then we have to adjust the
   * search radius to take into account any position angle. That means the
   * outer limit increases by the distance from the base to the offset and the
   * inner limit decreases by the same distance (never less than 0 though).
   *
   * @return the (possibly ) adjusted radius limits
   */
  def adjustedLimits: RadiusConstraint =
    if (offset.isDefined && posAngle.isEmpty) {
      radiusLimits.adjust(offset.get)
    } else {
      radiusLimits
    }

  /**
   * Sets the offset to a specific value.
   */
  case class Matcher(adjBase: Coordinates, adjLimits: RadiusConstraint) {

    /**
     * @param obj the SkyObject to match
     * @return true if the object matches the magnitude and radius limits
     */
    def matches(obj: SiderealTarget): Boolean = {
      matches(obj.magnitudes) && matches(obj.coordinates)
    }

    private def matches(coords: Coordinates): Boolean = {
      val distance = Coordinates.difference(adjBase, coords).distance
      val minRadius = adjLimits.minLimit
      val maxRadius = adjLimits.maxLimit
      distance >= minRadius && distance <= maxRadius
    }

    private def matches(magList: List[Magnitude]): Boolean =
      magList.exists(matches)

    private def matches(mag: Magnitude): Boolean = magLimits.contains(mag)
  }

  /**
   * This can be used as a predicate to filter on a List[SkyObject].
   *
   * @param base the base position
   * @return a new Matcher for the given base position
   */
  def matcher(base: Coordinates): Matcher =
    Matcher(adjustedBase(base), adjustedLimits)

}

/**
 * See OT-24
 */
case class GemsCatalogSearchCriterion(key: GemsCatalogSearchKey, criterion: CatalogSearchCriterion)

/**
 * Results of a GeMS catalog search
 * See OT-24
 */
case class GemsCatalogSearchResults(criterion: GemsCatalogSearchCriterion, results: List[skyobject.SkyObject]) {
  def this(criterion: GemsCatalogSearchCriterion, results: java.util.List[skyobject.SkyObject]) = this(criterion, results.asScala.toList)

  def resultsAsJava: java.util.List[skyobject.SkyObject] = results.asJava
}

/**
 * Represents the GeMS catalog star options
 * See OT-24
 */
case class GemsCatalogSearchKey(starType: GemsGuideStarType, group: GemsGuideProbeGroup)
