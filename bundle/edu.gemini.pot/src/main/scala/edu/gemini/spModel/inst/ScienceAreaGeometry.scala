package edu.gemini.spModel.inst

import java.awt.Shape
import java.awt.geom.{Point2D, AffineTransform, Area}

import edu.gemini.spModel.obscomp.SPInstObsComp

/**
 * As the instrument configuration may change, we want this to be a parameter to
 * the methods instead of a parameter supplied at construction time.
 * @tparam I the type of instrument
 */
trait ScienceAreaGeometry[I <: SPInstObsComp] extends FeatureGeometry {
  def basicScienceArea: Option[Shape]

  override def geometry: List[Shape] =
    basicScienceArea.toList

  /**
   * Take the science area as constructed by the scienceArea method and apply the
   * necessary series of affine transformations to it to adjust it to a given screen
   * base position for a specific position angle and a given pixel density.
   * @param base            the base position on the display
   * @param posAngle        the position angle
   * @param pixelsPerArcsec the pixel density per arcsec
   * @return                the science area adjusted for the parameters
   */
  def scienceArea(base: Point2D = new Point2D.Double(0.0, 0.0),
                  posAngle: Double = 0.0,
                  pixelsPerArcsec: Double = 1.0): Option[Shape] = {
    basicScienceArea.map { s =>
      // We scale to get the desired pixelsPerArcsec, and translate for screen base position,
      // and finally rotate it for the position angle.
      // Transforms are applied in reverse order of concatenation, i.e. from right to left.
      val transform = new AffineTransform()
      transform.rotate(-posAngle, base.getX, base.getY)
      transform.translate(base.getX, base.getY)
      transform.scale(pixelsPerArcsec, pixelsPerArcsec)

      val a = new Area(s)
      a.transform(transform)
      a
    }
  }

  def scienceAreaAsJava(base: Point2D = new Point2D.Double(0.0, 0.0),
                         posAngle: Double = 0.0,
                         pixelsPerArcsec: Double = 1.0): Shape =
    scienceArea(base, posAngle, pixelsPerArcsec).orNull
}
