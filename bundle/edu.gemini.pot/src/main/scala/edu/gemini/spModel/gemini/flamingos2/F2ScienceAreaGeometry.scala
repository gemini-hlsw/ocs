package edu.gemini.spModel.gemini.flamingos2

import java.awt.Shape
import java.awt.geom.{Rectangle2D, Area, Ellipse2D}

import edu.gemini.shared.util.immutable.DefaultImList
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.{FPUnit, LyotWheel}
import edu.gemini.spModel.inst.ScienceAreaGeometry

import scala.collection.JavaConverters._

class F2ScienceAreaGeometry(inst0: Flamingos2) extends ScienceAreaGeometry[Flamingos2] {
  import F2ScienceAreaGeometry._

  override def geometry: List[Shape] = {
    Option(inst0).toList.flatMap { inst =>
      lazy val plateScale = inst.getLyotWheel.getPlateScale
      lazy val scienceAreaWidth = {
        val scienceAreaArray = inst.getScienceArea
        scienceAreaArray(0)
      }
      inst.getFpu match {
        case Flamingos2.FPUnit.FPU_NONE    => List(imagingFOV(plateScale))
        case Flamingos2.FPUnit.CUSTOM_MASK => List(mosFOV(plateScale))
        case _ if inst.getFpu.isLongslit   => List(longSlitFOV(plateScale, scienceAreaWidth))
        case _                             => Nil
      }
    }
  }

  // We need to override this due to type system.
  override def geometryAsJava: edu.gemini.shared.util.immutable.ImList[Shape] =
    DefaultImList.create(geometry.asJava)

  def scienceAreaDimensions: (Double, Double) = {
    Option(inst0).map { inst =>
      val lyotWheel = inst.getLyotWheel
      if (Set(LyotWheel.OPEN, LyotWheel.HIGH, LyotWheel.LOW).contains(lyotWheel)) {
        lazy val plateScale = lyotWheel.getPlateScale
        inst.getFpu match {
          case FPUnit.FPU_NONE =>
            val size = ImagingFOVSize * plateScale
            (size, size)
          case FPUnit.CUSTOM_MASK =>
            (MOSFOVWidth * plateScale, ImagingFOVSize * plateScale)
          case fpu if fpu.isLongslit =>
            (fpu.getSlitWidth * lyotWheel.getPixelScale, LongSlitFOVHeight * plateScale)
          case _ =>
            (0.0, 0.0)
        }
      } else (0.0, 0.0)
    }.getOrElse(0.0, 0.0)
  }

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

    val area = new Area(circle)
    area.intersect(new Area(rectangle))
    area
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

}

object F2ScienceAreaGeometry {
  // Geometry features for F2, in arcseconds.
  val LongSlitFOVHeight   = 164.1
  val LongSlitFOVSouthPos = 112.0
  val LongSlitFOVNorthPos = 52.1
  val ImagingFOVSize      = 230.12
  val MOSFOVWidth         = 75.16
}