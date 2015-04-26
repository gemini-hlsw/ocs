package edu.gemini.spModel.inst

import edu.gemini.spModel.core.{OffsetP, OffsetQ, Offset, Angle}
import edu.gemini.spModel.core.AngleSyntax._

import java.awt.geom.{Point2D, AffineTransform}

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
}

