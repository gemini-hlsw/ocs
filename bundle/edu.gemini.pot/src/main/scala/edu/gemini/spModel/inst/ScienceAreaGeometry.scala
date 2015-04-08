package edu.gemini.spModel.inst

import java.awt.Shape

import edu.gemini.shared.util.immutable.DefaultImList
import edu.gemini.spModel.obscomp.SPInstObsComp

import scala.collection.JavaConverters._

/**
 * Geometry (represented by a single Shape) for a science area.
 */
trait ScienceAreaGeometry {
  /**
   * Create the shapes for the science area based on the instrument configuration.
   * @return a list representing the shapes, or an empty list if no such shape exists
   */
  def geometry: List[Shape]

  /**
   * Return a list of the shapes comprising the geometry of the science area for Java.
   * @return an immutable list of the shapes, or an empty list if no such shape exists
   */
  def geometryAsJava: edu.gemini.shared.util.immutable.ImList[Shape] =
    DefaultImList.create(geometry.asJava)

  /**
   * Return the width and height bounds of the science area in arcsec x arcsec, which is needed by
   * various editors and P2 checks.
   * TODO: This is currently called from SPInstObsComp subclass getScienceArea methods.
   * TODO: Ultimately, when all the science areas are removed, this should be separated out
   * TODO: from SPInstObsComp and accessed directly through ScienceAreaGeometry instead of
   * TODO: from the instruments themselves.
   * @return     a rectangular bound of the size of the science area
   */
  def scienceAreaDimensions: (Double, Double)

  def scienceAreaDimensionsAsJava = {
    val bounds = scienceAreaDimensions
    Array(bounds._1, bounds._2)
  }
}
