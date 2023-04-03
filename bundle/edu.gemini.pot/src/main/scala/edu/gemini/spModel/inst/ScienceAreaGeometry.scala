package edu.gemini.spModel.inst

import edu.gemini.spModel.core.Offset
import edu.gemini.spModel.inst.FeatureGeometry._
import edu.gemini.spModel.obs.context.ObsContext

import java.awt.Shape

import scalaz._
import Scalaz._

/**
 * Geometry (represented by a single Shape) for a science area.
 */
trait ScienceAreaGeometry {

  /** Adjusted science area shapes in context, ready to be used in further
    * calculations or transformed to a screen plot. */
  def geometry(ctx: ObsContext, offset: Offset): Option[Shape] =
    offsetTransform(ctx.getPositionAngle, offset) |> { trans =>
      unadjustedGeometry(ctx).map { trans.createTransformedShape }
    }

  /** Create the shape for the science area based on the instrument configuration.
    * This shape is not adjusted for position angle or offsets. */
  def unadjustedGeometry(ctx: ObsContext): Option[Shape]
}
