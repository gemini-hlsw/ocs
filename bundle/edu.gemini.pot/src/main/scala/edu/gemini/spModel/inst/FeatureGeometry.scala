package edu.gemini.spModel.inst

import java.awt.Shape
import java.awt.geom.{Area, AffineTransform}

import scala.collection.JavaConverters._

/**
 * The geometry for a telescope feature, such as a science area or
 * a guide probe.
 */
trait FeatureGeometry {
  /**
   * Create Shapes representing the geometry features for the given instrument configuration.
   * @return the geometry features
   */
  def geometry: List[Shape]

  /**
   * A convenience method to calculate the geometry and then apply a transformation to it.
   * @param transform the transformation to apply
   * @return          the geometry for the features under the transformation
   */
  def transformedGeometry(transform: AffineTransform): List[Shape] = {
    geometry.map{ g =>
      val area = new Area(g)
      area.transform(transform)
      area
    }
  }

  def transformedGeometryAsJava(transform: AffineTransform) =
    transformedGeometry(transform).asJava

  /**
   * Combine the shapes representing the geometry features for the given instrument configuration.
   * @return a shape representing the union of the geometry features
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
   * @return          the geometry for the features under the transformation
   */
  def transformedFullGeometry(transform: AffineTransform): Shape = {
    val area = new Area(fullGeometry)
    area.transform(transform)
    area
  }
}
