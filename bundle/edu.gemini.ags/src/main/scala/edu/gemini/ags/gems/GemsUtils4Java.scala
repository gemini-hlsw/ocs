package edu.gemini.ags.gems

import edu.gemini.catalog.api.{SaturationConstraint, RadiusConstraint, MagnitudeConstraints}
import edu.gemini.ags.impl._
import edu.gemini.shared.util.immutable
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.shared.skyobject
import scala.math._
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

/**
 * Utility methods for Java classes to access scala classes/methods
 */
object GemsUtils4Java {
  // Returns true if the target magnitude is within the given limits
  def containsMagnitudeInLimits(target: SPTarget, magLimits: MagnitudeConstraints): Boolean =
    // TODO This method has too many conversions, should be simplified
    target.getMagnitude(magLimits.band.toOldModel).asScalaOpt.map(m => magLimits.contains(m.toNewModel)).getOrElse(true)

  def mapMagintudes(constraint: Option[SaturationConstraint], mapOp: immutable.MapOp[SaturationConstraint, java.lang.Double]): immutable.Option[java.lang.Double] =
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

  def toCoordinates(coords: skyobject.coords.SkyCoordinates): Coordinates = {
    val c = coords.toHmsDeg(0L)
    Coordinates(RightAscension.fromAngle(c.getRa.toNewModel), Declination.fromAngle(c.getDec.toNewModel).getOrElse(Declination.zero))
  }

  def toSiderealTarget(skyObject: skyobject.SkyObject): SiderealTarget = skyObject.toNewModel

  def translateBands(bands: java.util.Set[skyobject.Magnitude.Band]): java.util.Set[MagnitudeBand] = bands.asScala.map(_.toNewModel).asJava

  def toOldBand(band: MagnitudeBand): skyobject.Magnitude.Band = band.toOldModel
}