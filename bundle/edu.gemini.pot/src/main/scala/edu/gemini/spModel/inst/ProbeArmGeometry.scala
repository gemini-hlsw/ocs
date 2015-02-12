package edu.gemini.spModel.inst

import java.awt.Shape

import edu.gemini.shared.util.immutable.{DefaultImList, ImList}

import scala.collection.JavaConverters._

/**
 * Geometry (represented by a list of shapes) for a guide probe arm.
 */
trait ProbeArmGeometry {
  def geometry: List[Shape]

  def geometryAsJava: ImList[Shape] =
    DefaultImList.create(geometry.asJava)
}
