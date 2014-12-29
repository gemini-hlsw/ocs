package edu.gemini.spModel.inst

import java.awt.Shape
import java.awt.geom.{AffineTransform, Area, Point2D}

import edu.gemini.spModel.obscomp.SPInstObsComp

/**
 * As the instrument configuration may change, we want this to be a parameter to
 * the methods instead of a parameter supplied at construction time.
 * @tparam I the type of instrument
 */
trait ScienceArea[I <: SPInstObsComp] {
  /**
   * Create a Shape representing the science area for the given instrument configuration.
   * @param inst the instrument configuration to use
   * @return     the science area for the instrument configuration
   */
  def scienceArea(inst: I): Shape

  /**
   * A convenience method to calculate the science area and then apply a transformation
   * to it.
   * @param inst      the instrument configuration to use
   * @param transform the transformation to apply
   * @return          the science area for the instrument configuration under the transformation
   */
  def transformedScienceArea(inst: I, transform: AffineTransform): Shape = {
    val area = new Area(scienceArea(inst))
    area.transform(transform)
    area
  }

  /**
   * Take the science area as constructed by the scienceArea method and apply the
   * necessary series of affine transformations to it to adjust it to a given screen
   * base position for a specific position angle and a given pixel density.
   * @param inst            the instrument configuration to use
   * @param base            the base position on the display
   * @param posAngle        the position angle
   * @param pixelsPerArcsec the pixel density per arcsec
   * @return                the science area adjusted for the parameters
   */
  def transformedScienceArea(inst: I,
                             base: Point2D = new Point2D.Double(0.0, 0.0),
                             posAngle: Double = 0.0,
                             pixelsPerArcsec: Double = 1.0): Shape = {
    // We scale to get the desired pixelsPerArcsec, and translate for screen base position,
    // and finally rotate it for the position angle.
    // Transforms are applied in reverse order of concatenation, i.e. from right to left.
    val transform = new AffineTransform()
    transform.rotate(-posAngle, base.getX, base.getY)
    transform.translate(base.getX, base.getY)
    transform.scale(pixelsPerArcsec, pixelsPerArcsec)

    transformedScienceArea(inst, transform)
  }
}
