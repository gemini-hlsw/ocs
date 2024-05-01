package edu.gemini.spModel.gemini.igrins2

import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.inst.ScienceAreaGeometry
import edu.gemini.spModel.obs.context.ObsContext
import scalaz.Scalaz._

import java.awt.Shape
import java.awt.geom.{Area, Ellipse2D, Rectangle2D}

object Igrins2ScienceAreaGeometry extends ScienceAreaGeometry {
  val ScienceFovHeight: Angle = Angle.fromArcsecs(5.0)
  val ScienceFovWidth: Angle = Angle.fromArcsecs(0.3)
  val SVCSize: Angle   = Angle.fromArcsecs(46) //REL-4446 Updated from 40" to 46"
  val SVCRadius: Angle = (SVCSize / 2).get

  val SVCFieldOfView: Shape    =
    new Ellipse2D.Double(
      -SVCRadius.toArcsecs,
      -SVCRadius.toArcsecs,
      SVCSize.toArcsecs,
      SVCSize.toArcsecs
    )

  val scienceSlitFOV: Shape =
    new Rectangle2D.Double(
      -ScienceFovWidth.toArcsecs / 2.0,
      -ScienceFovHeight.toArcsecs / 2.0,
      ScienceFovWidth.toArcsecs,
      ScienceFovHeight.toArcsecs
    )

  /** Create the shape for the science area based on the instrument configuration.
   * This shape is not adjusted for position angle or offsets. */
  override def unadjustedGeometry(ctx: ObsContext): Option[Shape] = {
    val fov = new Area(SVCFieldOfView)
    fov.subtract(new Area(scienceSlitFOV))
    fov.some
  }

}
