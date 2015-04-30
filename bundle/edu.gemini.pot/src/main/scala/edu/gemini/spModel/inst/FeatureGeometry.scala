package edu.gemini.spModel.inst

import edu.gemini.spModel.core.{OffsetP, OffsetQ, Offset, Angle}
import edu.gemini.spModel.core.AngleSyntax._

import java.awt.Shape
import java.awt.geom.{Area, FlatteningPathIterator, Point2D, AffineTransform}
import java.awt.geom.PathIterator.{SEG_CLOSE, SEG_LINETO, SEG_MOVETO}
import java.util.logging.Logger

import scala.annotation.tailrec
import scalaz._
import Scalaz._

/**
 * Functions to manipulate the geometry for a telescope feature, e.g. a science
 * area or a guide probe arm.
 */
object FeatureGeometry {
  def offsetTransform(posAngle: Angle, offset: Offset): AffineTransform =
    posAngleTransform(posAngle) <| {
      _.translate(-offset.p.arcsecs, -offset.q.arcsecs)
    }

  def posAngleTransform(posAngle: Angle): AffineTransform =
    AffineTransform.getRotateInstance(-posAngle.toRadians)

  /** Transform to screen coordinates.
   * @param base   base position screen coordinates
   * @param scale  screen scale or pixels per arcsec
   * @param rot    rotation of the image from north
   * @param flipRa true if RA increases going right
   * @return transform that will map to screen coordinates
   */
  def screenTransform(base: Point2D, scale: Double, rot: Angle, flipRa: Boolean): AffineTransform =
    AffineTransform.getTranslateInstance(base.getX, base.getY) <|
      (_.scale(scale, scale))                                  <|
      (_.rotate(-rot.toRadians))                               <|
      (_.scale(if (flipRa) -1 else 1, 1))

  implicit class PointOps(p: Point2D) {
    def toOffset: Offset =
      Offset(p.getX.arcsecs[OffsetP], p.getY.arcsecs[OffsetQ]) * -1
  }

  implicit class OffsetOps(o: Offset) {
    def toPoint: Point2D =
      new Point2D.Double(-o.p.arcsecs, -o.q.arcsecs)

    def rotate(posAngle: Angle): Offset = {
      val result = new Point2D.Double
      posAngleTransform(posAngle).transform(o.toPoint, result)
      result.toOffset
    }
  }

  /** Approximate area of the given shape, assuming it doesn't have holes.
    * Holes, unfortunately are double counted as if they were disjoint shapes by
    * the algorithm. */
  def approximateArea(s: Shape): Double =
    // ideal flatness and recursion depth limits are unclear but we are just
    // looking for rough estimates for the purpose of vignetting calculation
    approximateArea(s, 0.5, 6)

  def approximateArea(s: Shape, flatness: Double, depth: Int): Double = {
    val fpi    = new FlatteningPathIterator(new Area(s).getPathIterator(null), flatness, depth)
    val coords = Array.fill(6)(0.0) // as required by path iterator ...

    case class AreaCalc(start: (Double, Double), prev: (Double, Double), sum: Double) {
      def addPoint(next: (Double, Double)): AreaCalc = {
        val (x0, y0) = prev
        val (x1, y1) = next
        AreaCalc(start, next, sum + x0 * y1 - y0 * x1)
      }

      @tailrec
      final def close: Double =
        if (prev == start) (sum/2.0).abs
        else addPoint(start).close
    }

    @tailrec
    def go(area: Double, cur: Option[AreaCalc]): Double =
      if (fpi.isDone) area
      else {
        val segType = fpi.currentSegment(coords)
        fpi.next()
        segType match {
          case SEG_CLOSE  =>
            go(area + (cur.map(_.close) | 0.0), None)

          case SEG_MOVETO =>
            val point = (coords(0), coords(1))
            go(area, Some(AreaCalc(point, point, 0.0)))

          case SEG_LINETO =>
            val point = (coords(0), coords(1))
            go(area, cur.map(_.addPoint(point)))

          case _          =>
            // presumably impossible
            Logger.getLogger(getClass.getName).severe("FlatteningPathIterator returned a curve segment!")
            0.0
        }
      }

    go(0, None)
  }
}

