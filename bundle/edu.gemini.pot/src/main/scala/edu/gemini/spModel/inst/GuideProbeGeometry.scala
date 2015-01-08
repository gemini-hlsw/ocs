package edu.gemini.spModel.inst

import java.awt.Shape
import java.awt.geom.{AffineTransform, Point2D}

import scala.collection.JavaConverters._

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

  def geometryForParams(posAngle:        Double  = 0.0,
                        guideStar:       Point2D = new Point2D.Double(0.0, 0.0),
                        offset:          Point2D = new Point2D.Double(0.0, 0.0),
                        xflipArm:        Boolean = false,
                        raFactor:        Double  = 1.0): List[Shape]

  def geometryForParamsAsJava(posAngle:        Double  = 0.0,
                              guideStar:       Point2D = new Point2D.Double(0.0, 0.0),
                              offset:          Point2D = new Point2D.Double(0.0, 0.0),
                              xflipArm:        Boolean = false,
                              raFactor:        Double  = 1.0): java.util.List[Shape] =
    geometryForParams(posAngle, guideStar, offset, xflipArm, raFactor).asJava

  def geometryForScreen(posAngle:        Double  = 0.0,
                        guideStar:       Point2D = new Point2D.Double(0.0, 0.0),
                        offset:          Point2D = new Point2D.Double(0.0, 0.0),
                        translate:       Point2D = new Point2D.Double(0.0, 0.0),
                        xflipArm:        Boolean = false,
                        raFactor:        Double  = 1.0,
                        pixelsPerArcsec: Double  = 0.0): List[Shape] = {
    import FeatureGeometry.transformPoint
    val pixelsToArcsec = AffineTransform.getScaleInstance(1.0 / pixelsPerArcsec, 1.0 / pixelsPerArcsec)
    val arcsecToPixels = AffineTransform.getScaleInstance(pixelsPerArcsec, pixelsPerArcsec)

    val trans = AffineTransform.getTranslateInstance(translate.getX, translate.getY)
    trans.concatenate(arcsecToPixels)
    geometryForParams(posAngle, transformPoint(guideStar, pixelsToArcsec),
      transformPoint(offset, pixelsToArcsec), xflipArm, raFactor).map(s => trans.createTransformedShape(s))
  }

  def geometryForScreenAsJava(posAngle:        Double  = 0.0,
                              guideStar:       Point2D = new Point2D.Double(0.0, 0.0),
                              offset:          Point2D = new Point2D.Double(0.0, 0.0),
                              translate:       Point2D = new Point2D.Double(0.0, 0.0),
                              xflipArm:        Boolean = false,
                              raFactor:        Double  = 1.0,
                              pixelsPerArcsec: Double  = 0.0): java.util.List[Shape] =
    geometryForScreen(posAngle, guideStar, offset, translate, xflipArm, raFactor, pixelsPerArcsec).asJava

  override def geometry: List[Shape] =
    List(probeArm, pickoffMirror)
}
