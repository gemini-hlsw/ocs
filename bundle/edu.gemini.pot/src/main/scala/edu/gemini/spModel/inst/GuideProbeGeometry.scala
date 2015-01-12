package edu.gemini.spModel.inst

import java.awt.Shape
import java.awt.geom.{AffineTransform, Point2D}

import scala.collection.JavaConverters._

trait GuideProbeGeometry extends FeatureGeometry {
  /**
   * Define the geometry of the probe arm for the guide probe for a guide star at the origin.
   * @return the geometry for the probe arm in arcsec
   */
  protected def probeArm: Shape

  /**
   * Define the geometry for the pickoff mirror for the guide probe for a guide star at the origin.
   * @return the geometry for the pickoff mirror in arcsec
   */
  protected def pickoffMirror: Shape

  /**
   * Calculate the geometry of the probe arm for the given parameters.
   * @param posAngle  the position angle in radians
   * @param guideStar the position of the selected guide star in arcsec
   * @param offset    the position of the offset in arcsec
   * @return          the geometry of the probe in arcsec
   */
  def geometryForParams(posAngle:        Double  = 0.0,
                        guideStar:       Point2D = new Point2D.Double(0.0, 0.0),
                        offset:          Point2D = new Point2D.Double(0.0, 0.0)): List[Shape]

  /**
   * Caclulate the geometry of the probe arm for the given parameters and return the results as a Java list.
   * @see geometryForParams
   */
  def geometryForParamsAsJava(posAngle:        Double  = 0.0,
                              guideStar:       Point2D = new Point2D.Double(0.0, 0.0),
                              offset:          Point2D = new Point2D.Double(0.0, 0.0)): java.util.List[Shape] =
    geometryForParams(posAngle, guideStar, offset).asJava

  /**
   * Calculate the geometry of the probe arm based on screen coordinates (for the TPE).
   * @param posAngle        the position angle in radians
   * @param guideStar       screen coordinates of the selected guide star
   * @param offset          screen coordinates of the offset in use
   * @param translate       any screen coordinates to use to translate the final result
   * @param xFlipArm        boolean as to whether the probe arm should be reflected in the x-axis
   * @param raFactor        RA scaling, representing a multiple to the x-coordinate (should be 1.0 or -1.0)
   * @param pixelsPerArcsec pixels per arcsec in the current screen representation
   * @return                the geometry of the probe in screen coordinates
   */
  def geometryForScreen(posAngle:        Double  = 0.0,
                        guideStar:       Point2D = new Point2D.Double(0.0, 0.0),
                        offset:          Point2D = new Point2D.Double(0.0, 0.0),
                        translate:       Point2D = new Point2D.Double(0.0, 0.0),
                        xFlipArm:        Boolean = false,
                        raFactor:        Double  = 1.0,
                        pixelsPerArcsec: Double  = 0.0): List[Shape] = {
    import FeatureGeometry.transformPoint

    val xFlipFactor = if (xFlipArm) -1.0 else 1.0

    val screenToArcsec = AffineTransform.getScaleInstance(raFactor, xFlipFactor)
    screenToArcsec.scale(1.0 / pixelsPerArcsec, 1.0 / pixelsPerArcsec)

    val arcsecToScreen = AffineTransform.getTranslateInstance(translate.getX, translate.getY)
    arcsecToScreen.scale(pixelsPerArcsec, pixelsPerArcsec)
    arcsecToScreen.scale(raFactor, xFlipFactor)

    geometryForParams(posAngle, transformPoint(guideStar, screenToArcsec),
      transformPoint(offset, screenToArcsec)).map(s => arcsecToScreen.createTransformedShape(s))
  }

  /**
   * Calculate the geometry of the probe arm based on screen coordinates and return the results as a Java list.
   * @see geometryForScreen
   */
  def geometryForScreenAsJava(posAngle:        Double  = 0.0,
                              guideStar:       Point2D = new Point2D.Double(0.0, 0.0),
                              offset:          Point2D = new Point2D.Double(0.0, 0.0),
                              translate:       Point2D = new Point2D.Double(0.0, 0.0),
                              xFlipArm:        Boolean = false,
                              raFactor:        Double  = 1.0,
                              pixelsPerArcsec: Double  = 0.0): java.util.List[Shape] =
    geometryForScreen(posAngle, guideStar, offset, translate, xFlipArm, raFactor, pixelsPerArcsec).asJava

  override def geometry: List[Shape] =
    List(probeArm, pickoffMirror)
}
