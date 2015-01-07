package edu.gemini.spModel.inst

import java.awt.Shape
import java.awt.geom.{Point2D, Area, AffineTransform}

trait GuideProbeGeometry extends FeatureGeometry {
  /**
   * Define the geometry of the probe arm for the guide probe.
   * @return
   */
  def probeArm: Shape

  /**
   * Define the geometry for the pickoff mirror for the guide probe.
   * @return
   */
  def pickoffMirror: Shape

  override def geometry: List[Shape] =
    List(probeArm, pickoffMirror)
}
