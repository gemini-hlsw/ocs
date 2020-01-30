package edu.gemini.spModel.gemini.ghost

import java.awt.Shape
import java.awt.geom.Ellipse2D

import edu.gemini.spModel.inst.ScienceAreaGeometry
import edu.gemini.spModel.obs.context.ObsContext
import scalaz._
import Scalaz._

object GhostScienceAreaGeometry extends ScienceAreaGeometry {
  private val size = 7.5 * 60 // arcmin
  private val radius = size / 2

  /** Create the shape for the science area based on the instrument configuration.
   * This shape is not adjusted for position angle or offsets. */
  override def unadjustedGeometry(ctx: ObsContext): Option[Shape] =
    new Ellipse2D.Double(-radius, -radius, size, size).some
}
