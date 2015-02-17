package edu.gemini.spModel.inst

import java.awt.Shape
import java.awt.geom.Point2D

import edu.gemini.shared.util.immutable.{DefaultImList, ImList, Option => GOption}
import edu.gemini.skycalc
import edu.gemini.skycalc.Offset
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.SPTarget

import scala.collection.JavaConverters._

/**
 * Geometry (represented by a list of shapes) for a guide probe arm.
 */
trait ProbeArmGeometry {
  /**
   * Create a list of Shape representing the probe arm and all its components (e.g. pickoff mirror).
   * @return the list of shapes
   */
  def geometry: List[Shape]

  def geometryAsJava: ImList[Shape] =
    DefaultImList.create(geometry.asJava)

  /**
   * An instance of the probe being represented by this class.
   * @return the probe instance
   */
  protected def guideProbeInstance: GuideProbe

  /**
   * For a given context, guide star coordinates, and offset, calculate the arm adjustment that will be used for the
   * guide star at those coordinates.
   * @param ctx             the context representing the configuration
   * @param guideStarCoords the guide star for which to calculate the adjustment
   * @param offset          the offset for which to calculate the adjustment
   * @return                the probe arm adjustments for this data
   */
  def armAdjustment(ctx: ObsContext, guideStarCoords: Coordinates, offset: Offset): Option[ArmAdjustment]

  def armAdjustment(ctx: ObsContext, guideStar: SPTarget, offset: Offset): Option[ArmAdjustment] = {
    import ProbeArmGeometry._
    armAdjustment(ctx, guideStar.getTarget.getSkycalcCoordinates.toNewModel, offset)
  }

  def armAdjustment(ctx: ObsContext, guideStar: SiderealTarget, offset: Offset): Option[ArmAdjustment] =
    armAdjustment(ctx, guideStar.coordinates, offset)

  def armAdjustmentAsJava(ctx: ObsContext, guideStarCoords: Coordinates, offset: Offset): GOption[ArmAdjustment] =
    armAdjustment(ctx, guideStarCoords, offset).asGeminiOpt

  def armAdjustmentAsJava(ctx: ObsContext, guideStar: SPTarget, offset: Offset): GOption[ArmAdjustment] =
    armAdjustment(ctx, guideStar, offset).asGeminiOpt

  def armAdjustmentAsJava(ctx: ObsContext, guideStar: SiderealTarget, offset: Offset): GOption[ArmAdjustment] =
    armAdjustment(ctx, guideStar, offset).asGeminiOpt

  /**
   * For a given context and offset, calculate the arm adjustment that will be used for the primary / selected guide
   * star.
   * @param ctx    the context representing the configuration
   * @param offset the offset for which to calculate the adjustment
   * @return       the probe arm adjustments for this data
   */
  def armAdjustment(ctx: ObsContext, offset: Offset): Option[ArmAdjustment] =
    (for {
      c            <- Option(ctx)
      guideTargets <- ctx.getTargets.getPrimaryGuideProbeTargets(guideProbeInstance).asScalaOpt
      guideStar    <- guideTargets.getPrimary.asScalaOpt
    } yield armAdjustment(ctx, guideStar, offset)).flatten

  def armAdjustmentAsJava(ctx: ObsContext, offset: Offset): GOption[ArmAdjustment] =
    armAdjustment(ctx, offset).asGeminiOpt
}

object ProbeArmGeometry {
  // This is a hideous hack of code to convert between coordinate systems, using code from the edu.gemini.ags package
  // object, since this package is not available here and would currently introduce a circular dependency.

  import edu.gemini.skycalc.{Coordinates => SkycalcCoordinates}

  // This code is largely copy-pasted from edu.gemini.ags impl/package.scala.
  implicit class SkycalcCoordinates2New(val coordinates: SkycalcCoordinates) extends AnyVal {
    def toNewModel: Coordinates = {
      val ra = RightAscension.fromAngle(coordinates.getRa.toNewModel)
      val dec = Declination.fromAngle(coordinates.getDec.toNewModel).getOrElse(Declination.zero)
      Coordinates(ra, dec)
    }
  }

  private implicit class OldAngle2New(val angle: skycalc.Angle) extends AnyVal {
    def toNewModel: Angle = Angle.fromDegrees(angle.toDegrees.getMagnitude)
  }


  private lazy val maxArcsecs = 360 * 60 * 60d
  private implicit class CanonicalValue(val v: Double) extends AnyVal {
    def toCanonicalValue: Double = {
      val v1 = math.IEEEremainder(v, maxArcsecs)
      val v2 = v1 - maxArcsecs
      if (math.abs(v1) <= math.abs(v2)) v1 else v2
    }
  }

  implicit class CanonicalPoint(val p: Point2D) extends AnyVal {
    def toCanonicalForm: Point2D =
      new Point2D.Double(p.getX.toCanonicalValue, p.getY.toCanonicalValue)
  }
}

/**
 * A representation of the adjustment made to the default list of shapes when using a specified guide star.
 * @param angle           the angle which will be used by the probe arm
 * @param guideStarCoords the coordinates (in arcsec) where the probe arm will be placed
 */
// TODO: Should we add the guide star itself here and then just calculate the coordinates? We already calculate
// TODO: the coordinates for GMOS, but this seems like it could certainly be probe independent.
case class ArmAdjustment(angle: Double, guideStarCoords: Point2D)