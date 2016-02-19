package edu.gemini.mascot.gui.contour

import edu.gemini.ags.gems.mascot.Strehl
import breeze.linalg._
import breeze.util._

/**
 * Utility class to create a contour plot from a Strehl object.
 */
object StrehlContourPlot {

  // Create and return a contour plot for the given Strehl object.
  def create(s: Strehl, size: Int) : ContourPlot = {
    val m = s.strehl_map
    val numCols = m.cols
    val data = (for (i <- 0 until numCols) yield m(i, ::).t.toDenseVector.toArray).toArray
    val cmap = ColorMap.getColormap("YlGn", ContourPlot.N_CONTOURS - 1, true)
    ContourPlot.createPlot(size, size, data, cmap)
  }

}
