package edu.gemini.spModel.inst

import java.awt.Shape
import java.awt.geom.{Point2D, Area, AffineTransform}

import scala.collection.JavaConverters._

/**
 * The geometry for a telescope feature, such as a science area or
 * a guide probe.
 */
trait FeatureGeometry {
  /**
   * Create Shapes representing the geometry features for the given instrument configuration.
   * @return the geometry features in arcsec
   */
  def geometry: List[Shape]

  /**
   * A convenience method to calculate the geometry and then apply a transformation to it.
   * @see geometry
   * @param transform the transformation to apply
   * @return          the geometry for the features under the transformation in arcsec
   */
  def transformedGeometry(transform: AffineTransform): List[Shape] = {
    geometry.map{ g =>
      val area = new Area(g)
      area.transform(transform)
      area
    }
  }

  /**
   * A convenience method to calculate the geometry and then apply a transformation to it and return
   * the results as a Java list.
   * @see transformedGeometry
   * @see geometry
   */
  def transformedGeometryAsJava(transform: AffineTransform): java.util.List[Shape] =
    transformedGeometry(transform).asJava

  /**
   * Combine the shapes representing the geometry features for the given instrument configuration.
   * @return a shape representing the union of the geometry features in arcsec
   */
  def fullGeometry: Shape = {
    val allShapes = geometry
    if (allShapes.length <= 1)
      allShapes.headOption.getOrElse(new Area())
    else {
      val fullArea = new Area()
      geometry.foreach(g => fullArea.add(new Area(g)))
      fullArea
    }
  }

  /**
   * A convenience method to calculate the full geometry and then apply a transformation to it.
   * @param transform the transformation to apply
   * @return          the geometry for the features under the transformation in arcsec
   */
  def transformedFullGeometry(transform: AffineTransform): Shape = {
    val area = new Area(fullGeometry)
    area.transform(transform)
    area
  }
}

object FeatureGeometry {
  /**
   * Convenience method to execute a transformation on a point and return the result.
   * @param p     the point to transform
   * @param trans the transformation to execute
   * @return      the transformed point
   */
  def transformPoint(p: Point2D, trans: AffineTransform): Point2D = {
    val pTrans = new Point2D.Double()
    trans.transform(p, pTrans)
    pTrans
  }
}