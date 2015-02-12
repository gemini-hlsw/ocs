package edu.gemini.spModel.inst

import java.awt.Shape

import edu.gemini.shared.util.immutable.DefaultImList
import edu.gemini.spModel.obscomp.SPInstObsComp

import scala.collection.JavaConverters._

/**
 * Geometry (represented by a single Shape) for a science area.
 */
trait ScienceAreaGeometry[I <: SPInstObsComp] {
  /**
   * Create the shapes for the science area based on the instrument configuration.
   * @return a list representing the shapes, or an empty list if no such shape exists
   */
  def geometry(inst: I): List[Shape]

  /**
   * Return a list of the shapes comprising the geometry of the science area for Java.
   * @return an immutable list of the shapes, or an empty list if no such shape exists
   */
  def geometryAsJava(inst: I): edu.gemini.shared.util.immutable.ImList[Shape] =
    DefaultImList.create(geometry(inst).asJava)
}
