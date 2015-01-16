package edu.gemini.ags.gems

import edu.gemini.ags.gems.mascot.{Star, MascotConf}
import edu.gemini.catalog.api.{SaturationConstraint, RadiusConstraint, MagnitudeConstraints}
import edu.gemini.ags.impl._
import edu.gemini.shared.util.immutable
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.shared.skyobject
import edu.gemini.skycalc
import scala.math._
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

/**
 * Utility methods for Java classes to access scala classes/methods
 */
object GemsUtils4Java {
  // Returns true if the target magnitude is within the given limits
  def containsMagnitudeInLimits(target: SiderealTarget, magLimits: MagnitudeConstraints): Boolean =
    target.magnitudeIn(magLimits.band).map(m => magLimits.contains(m)).getOrElse(true)

  def mapMagnitudes(constraint: Option[SaturationConstraint], mapOp: immutable.MapOp[SaturationConstraint, java.lang.Double]): immutable.Option[java.lang.Double] =
    constraint.map(m => mapOp.apply(m)).asGeminiOpt

  // Combines multiple radius limits into one
  def optimizeRadiusConstraint(criterList: java.util.List[GemsCatalogSearchCriterion]): RadiusConstraint = {
    val result = criterList.asScala.foldLeft((Double.MinValue, Double.MaxValue)) { (prev, current) =>
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

  /**
   * Sorts the targets list, putting the brightest stars first and returns the sorted array.
   */
  def sortTargetsByBrightness(targetsList: java.util.List[SiderealTarget]): java.util.List[SiderealTarget] =
    targetsList.asScala.sortBy(_.magnitudeIn(MagnitudeBand.R)).asJava

  /**
   * Returns a list of unique targets in the given search results.
   */
  def uniqueTargets(list: java.util.List[GemsCatalogSearchResults]): java.util.List[Target.SiderealTarget] = {
    import collection.breakOut
    new java.util.ArrayList(list.asScala.map(_.results).flatten.groupBy(_.name).map(_._2.head)(breakOut).asJava)
  }

  // Set of conversors of new model to old model and vice versa for use in Java, they should disappear in time
  def toCoordinates(coords: skyobject.coords.SkyCoordinates): Coordinates = {
    val c = coords.toHmsDeg(0L)
    Coordinates(RightAscension.fromAngle(c.getRa.toNewModel), Declination.fromAngle(c.getDec.toNewModel).getOrElse(Declination.zero))
  }

  def toSiderealTarget(skyObject: skyobject.SkyObject): SiderealTarget = skyObject.toNewModel

  def translateBands(bands: java.util.Set[skyobject.Magnitude.Band]): java.util.Set[MagnitudeBand] = bands.asScala.map(_.toNewModel).asJava

  def toOldBand(band: MagnitudeBand): skyobject.Magnitude.Band = band.toOldModel

  def toNewBand(band: skyobject.Magnitude.Band): MagnitudeBand = band.toNewModel

  def toNewAngle(angle: skycalc.Angle): Angle = angle.toNewModel

  def toSPTarget(siderealTarget: SiderealTarget):SPTarget = new SPTarget(siderealTarget.toOldModel)
}