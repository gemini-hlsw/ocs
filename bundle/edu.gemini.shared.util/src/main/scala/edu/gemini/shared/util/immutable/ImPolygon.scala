package edu.gemini.shared.util.immutable

import java.awt.{Rectangle, Polygon, Shape}
import java.awt.geom._

/**
 * An immutable class representing a polygon as a collection of points.
 * @param points the points, in reverse order, representing the vertices of the polygon, which is assumed to be closed
 *               and thus does not require the first point to also be the last. The order is reversed to make calls to
 *               addPoint more efficient, so that points can be added to the list via a cons operation instead of
 *               an append.
 */
sealed class ImPolygon private (points: List[(Double,Double)] = Nil) extends Shape {
  private lazy val closedPath = {
    // We need to move to the rightmost point, which happens when the Path is empty (i.e. the current point of the
    // Path is null).
    val path = points.foldRight {
      val initialPath = new Path2D.Double
      if (points.isEmpty)
        initialPath.moveTo(0,0)
      initialPath
    }{ case ((x,y), p) =>
        if (p.getCurrentPoint == null) p.moveTo(x,y)
        else p.lineTo(x,y)
        p
    }

    path.closePath()
    path
  }

  // Calculate the bounds.
  private lazy val bounds = closedPath.getBounds2D

  // Return a copy (for immutability) of the bounds, which is created by getBounds without requiring typecasting.
  // Note that a Rectangle has integer values, so this may not tightly bound the polygon.
  override def getBounds: Rectangle =
    bounds.getBounds

  // Return a new object representing the bounds as a Rectangle2D: getBounds2D instantiates a new Rectangle2D.
  override def getBounds2D: Rectangle2D =
    bounds.getBounds2D

  override def getPathIterator(at: AffineTransform): PathIterator =
    closedPath.getPathIterator(at)

  // Flatness is ignored because a polygon is already flat.
  override def getPathIterator(at: AffineTransform, flatness: Double): PathIterator =
    getPathIterator(at)

  override def contains(x: Double, y: Double): Boolean =
    points.length > 2 && bounds.contains(x, y) && closedPath.contains(x, y)

  override def contains(p: Point2D): Boolean =
    contains(p.getX, p.getY)

  override def contains(x: Double, y: Double, w: Double, h: Double): Boolean =
    points.nonEmpty && bounds.contains(x, y, w, h) && closedPath.contains(x, y, w, h)

  override def contains(r: Rectangle2D): Boolean =
    contains(r.getX, r.getY, r.getWidth, r.getHeight)

  override def intersects(x: Double, y: Double, w: Double, h: Double): Boolean =
    points.nonEmpty && bounds.intersects(x, y, w, h) && closedPath.intersects(x, y, w, h)

  override def intersects(r: Rectangle2D): Boolean =
    intersects(r.getX, r.getY, r.getWidth, r.getHeight)

  def addPoint(x: Double, y: Double): ImPolygon =
    new ImPolygon((x,y) :: points)

  def addPoint(p: Point2D): ImPolygon =
    addPoint(p.getX, p.getY)
}

object ImPolygon {
  def apply() =
    new ImPolygon(Nil)

  def apply(points: List[(Double,Double)]) =
    new ImPolygon(points.reverse)

  def apply(rect: Rectangle2D): ImPolygon = {
    val (minx, maxx) = (rect.getMinX, rect.getMaxX)
    val (miny, maxy) = (rect.getMinY, rect.getMaxY)
    ImPolygon(List((minx,miny),(minx,maxy),(maxx,maxy),(maxx,miny)))
  }

  def apply(pol: Polygon): ImPolygon =
    ImPolygon((pol.xpoints.map(_.toDouble), pol.ypoints.map(_.toDouble)).zipped.toList)
}