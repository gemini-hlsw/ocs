package edu.gemini.spModel.gemini.ghost

import java.awt.Shape
import java.awt.geom.Ellipse2D

import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.inst.ScienceAreaGeometry
import edu.gemini.spModel.obs.context.ObsContext

import scalaz._
import Scalaz._

object GhostScienceAreaGeometry extends ScienceAreaGeometry {
  val Size: Angle   = Angle.fromArcsecs(444)
  val Radius: Angle = (Size / 2).get

  val Fov: Shape    =
    new Ellipse2D.Double(
      -Radius.toArcsecs,
      -Radius.toArcsecs,
      Size.toArcsecs,
      Size.toArcsecs
    )

  /** Create the shape for the science area based on the instrument configuration.
   * This shape is not adjusted for position angle or offsets. */
  override def unadjustedGeometry(ctx: ObsContext): Option[Shape] =
    Fov.some
}
