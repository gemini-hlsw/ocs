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

  val ImagingFovSize          = 330.34

  val Fov: Shape    =
    new Ellipse2D.Double(
      -Radius.toArcsecs,
      -Radius.toArcsecs,
      Size.toArcsecs,
      Size.toArcsecs
    )


  def scienceAreaDimensions(fpu: Double): (Double, Double) = {
    val width = fpu
    if (width != -1) (width, ImagingFovSize) else (ImagingFovSize, ImagingFovSize)
  }

  def javaScienceAreaDimensions(fpu: Double): Array[Double] =
    scienceAreaDimensions(fpu) match { case (w,h) => Array(w,h) }

  /** Create the shape for the science area based on the instrument configuration.
   * This shape is not adjusted for position angle or offsets. */
  override def unadjustedGeometry(ctx: ObsContext): Option[Shape] =
    Fov.some
}
