package edu.gemini.spModel.gemini.flamingos2

import edu.gemini.spModel.gemini.flamingos2.Flamingos2.LyotWheel.{OPEN, LOW, HIGH}
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.FPUnit.{CUSTOM_MASK, FPU_NONE}

import edu.gemini.spModel.inst.ScienceAreaGeometry
import edu.gemini.spModel.obs.context.ObsContext

import java.awt.Shape
import java.awt.geom.{Rectangle2D, Area, Ellipse2D}

import scalaz._
import Scalaz._

object F2ScienceAreaGeometry extends ScienceAreaGeometry {

  override def unadjustedGeometry(ctx: ObsContext): Option[Shape] =
    ctx.getInstrument match {
      case f2: Flamingos2 =>
        val plateScale       = f2.getLyotWheel.getPlateScale
        val scienceAreaWidth = scienceAreaDimensions(f2)._1
        f2.getFpu match {
          case Flamingos2.FPUnit.FPU_NONE    => Some(imagingFOV(plateScale))
          case Flamingos2.FPUnit.CUSTOM_MASK => Some(mosFOV(plateScale))
          case _ if f2.getFpu.isLongslit     => Some(longSlitFOV(plateScale, scienceAreaWidth))
          case _                             => None
        }

      case _              => None
    }

  def scienceAreaDimensions(f2: Flamingos2): (Double, Double) =
    f2.getLyotWheel match {
      case OPEN | HIGH | LOW =>
        val plateScale = f2.getLyotWheel.getPlateScale
        f2.getFpu match {
          case FPU_NONE =>
            val size = ImagingFOVSize * plateScale
            (size, size)
          case CUSTOM_MASK =>
            (MOSFOVWidth * plateScale, ImagingFOVSize * plateScale)
          case fpu if fpu.isLongslit =>
            (fpu.getSlitWidth * f2.getLyotWheel.getPixelScale, LongSlitFOVHeight * plateScale)
        }
      case _ => (0.0, 0.0)
    }

  def javaScienceAreaDimensions(f2: Flamingos2): Array[Double] =
    scienceAreaDimensions(f2) match { case (w, h) => Array(w, h) }

  /**
   * Create the F2 imaging field of view.
   * @param plateScale the plate scale in arcsec/mm
   * @return           a shape representing the FOV
   */
  private def imagingFOV(plateScale: Double): Shape = {
    val size   = ImagingFOVSize * plateScale
    val radius = size / 2.0
    new Ellipse2D.Double(-radius, -radius, size, size)
  }

  /**
   * Create the F2 MOS field of view shape.
   * @param plateScale the plate scale in arcsec/mm
   * @return           a shape representing the FOV
   */
  private def mosFOV(plateScale: Double): Shape = {
    val width  = MOSFOVWidth * plateScale
    val height = ImagingFOVSize * plateScale
    val radius = height / 2.0

    // The FOV is the intersection of a rectangle and a circle.
    val circle = new Ellipse2D.Double(-radius, -radius, height, height)
    val rectangle = new Rectangle2D.Double(-width/2.0, -radius, width, height)
    new Area(circle) <| (_.intersect(new Area(rectangle)))
  }

  /**
   * Create the F2 long slit field of view shape.
   * @param plateScale       the plate scale in arcsec/mm
   * @param scienceAreaWidth the width of the science area for the F2 configuration
   * @return                 a shape representing the FOV
   */
  private def longSlitFOV(plateScale: Double, scienceAreaWidth: Double): Shape = {
    val slitHeight = LongSlitFOVHeight * plateScale
    val slitSouth  = LongSlitFOVSouthPos * plateScale

    val x = -scienceAreaWidth / 2.0
    val y = slitSouth - slitHeight
    new Rectangle2D.Double(x, y, x + scienceAreaWidth, y + slitHeight)
  }

  // Geometry features for F2, in arcseconds.
  val LongSlitFOVHeight   = 164.10
  val LongSlitFOVSouthPos = 112.00
  val LongSlitFOVNorthPos =  52.10
  val ImagingFOVSize      = 230.12
  val MOSFOVWidth         =  75.16
}