package jsky.app.ot.tpe

import java.awt.geom.Point2D

import edu.gemini.spModel.core.{Coordinates, Declination, RightAscension}
import jsky.coords.CoordinateConverter

/**
  * Utility methods to convert coordinates for Java
  */
object CoordinatesUtilities {
  /**
    * Convert the given user coordinates location to world coordinates.
    */
  def userToWorldCoords(cc: CoordinateConverter, x: Double, y: Double): Coordinates = {
    val p = new Point2D.Double(x, y)
    cc.userToWorldCoords(p, false)
    Coordinates(RightAscension.fromDegrees(p.x), Declination.fromDegrees(p.y).getOrElse(Declination.zero))
  }
}
