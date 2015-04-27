package edu.gemini.spModel.inst

import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.core._
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.inst.FeatureGeometry._
import edu.gemini.spModel.inst.ProbeArmGeometry.ArmAdjustment
import edu.gemini.spModel.obs.context.ObsContext

import java.awt.Shape
import java.awt.geom.AffineTransform

import scalaz._
import Scalaz._


/**
 * Geometry (represented by a list of shapes) for a guide probe arm.
 */
trait ProbeArmGeometry {

  /** Adjusted guide probe shapes in context, ready to be used in further
   * calculations or transformed to a screen plot. */
  def geometry(ctx: ObsContext, guideStar: Coordinates, offset: Offset): Option[Shape] =
    armAdjustment(ctx, guideStar, offset).flatMap { adj =>
      val angle = adj.angle
      val gs    = adj.guideStar.toPoint
      val trans = AffineTransform.getRotateInstance(angle.toRadians, gs.getX, gs.getY) <|
                    (_.translate(gs.getX, gs.getY))

      unadjustedGeometry(ctx).map { trans.createTransformedShape }
    }

  /** Create a list of Shape representing the probe arm and all its components
    * (e.g. pickoff mirror).  This shape is not adjusted for position angle or
    * offsets.
   * @return the list of shapes
   */
  def unadjustedGeometry(ctx: ObsContext): Option[Shape]

  /**
   * An instance of the probe being represented by this class.
   * @return the probe instance
   */
  protected def guideProbeInstance: GuideProbe

  /**
   * For a given context, guide star coordinates, and offset, calculate the arm adjustment that will be used for the
   * guide star at those coordinates.
   * @param ctx       context representing the configuration
   * @param guideStar guide star for which to calculate the adjustment
   * @param offset    offset for which to calculate the adjustment
   * @return          probe arm adjustments for this data
   */
  def armAdjustment(ctx: ObsContext, guideStar: Coordinates, offset: Offset): Option[ArmAdjustment]
}

object ProbeArmGeometry {
  /**
   * A representation of the adjustment made to the default list of shapes when using a specified guide star.
   * @param angle     the angle which will be used by the probe arm
   * @param guideStar the coordinates (in arcsec) where the probe arm will be placed
   */
  case class ArmAdjustment(angle: Angle, guideStar: Offset)

  def guideStarOffset(ctx: ObsContext, guideStarCoords: Coordinates): Offset = {
    val baseCoords = ctx.getBaseCoordinates.toNewModel
    Coordinates.difference(baseCoords, guideStarCoords).offset
  }
}
