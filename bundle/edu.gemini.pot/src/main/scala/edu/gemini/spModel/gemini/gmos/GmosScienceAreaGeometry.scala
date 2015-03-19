package edu.gemini.spModel.gemini.gmos

import java.awt.Shape
import java.awt.geom.{Rectangle2D, Area}

import edu.gemini.shared.util.immutable.{DefaultImList, ImPolygon}
import edu.gemini.spModel.inst.ScienceAreaGeometry
import edu.gemini.spModel.core.Site

import scala.collection.JavaConverters._

class GmosScienceAreaGeometry[I  <: InstGmosCommon[D,F,P,SM],
                              D  <: Enum[D]  with GmosCommonType.Disperser,
                              F  <: Enum[F]  with GmosCommonType.Filter,
                              P  <: Enum[P]  with GmosCommonType.FPUnit,
                              SM <: Enum[SM] with GmosCommonType.StageMode](inst: I) extends ScienceAreaGeometry[I] {
  import GmosScienceAreaGeometry._

  override def geometry: List[Shape] = {
    Option(inst).toList.flatMap { inst =>
      lazy val width   = inst.getScienceArea()(0)
      lazy val isSouth = inst.getSite.contains(Site.GS)
      inst.getFPUnitMode match {
        case GmosCommonType.FPUnitMode.BUILTIN if inst.isImaging       => List(fov(ImagingFOVSize, ImagingFOVInnerSize))
        case GmosCommonType.FPUnitMode.BUILTIN if inst.isSpectroscopic => List(longSlitFOV(width))
        case GmosCommonType.FPUnitMode.BUILTIN if inst.isIFU           => List(ifuFOV(inst.getFPUnit, isSouth))
        case GmosCommonType.FPUnitMode.BUILTIN if inst.isNS            => List(nsFOV(width))
        case GmosCommonType.FPUnitMode.CUSTOM_MASK                     => List(fov(MOSFOVSize, MOSFOVInnerSize))
        case _                                                         => Nil
      }
    }
  }

  // We need to override this due to the subtypes of InstGmosCommon. Otherwise, the supermethod fails to properly
  // be called and an exception is thrown.
  override def geometryAsJava: edu.gemini.shared.util.immutable.ImList[Shape] =
    DefaultImList.create(geometry.asJava)

  override def scienceAreaDimensions: (Double, Double) = {
    val width = inst.getFPUnit.getWidth
    if (width != -1)
      (ImagingFOVSize, width)
    else
      (ImagingFOVSize, ImagingFOVSize)
  }

  /**
   * Return a field of view for the specified size.
   * Note that this needs to be offset by the base screen position, rotated by the position angle, and
   * scaled by the pixels per arcsecond to represent the final field of view in the TPE.
   * @param size            the basic size of a square, in arcsec, centred at the base pos
   * @param innerSize       the length of a side after cutting off the corners, in arcsec
   * @return                the shape matching the specifications
   */
  private def fov(size: Double, innerSize: Double): Shape = {
    val d1         = size / 2
    val d2         = innerSize / 2

    val cornerSize = d1 - d2

    val (x0, y0) = (-d1 + cornerSize, -d1)
    val (x1, y1) = ( x0 + innerSize ,  y0)
    val (x2, y2) = ( x1 + cornerSize,  y1 + cornerSize)
    val (x3, y3) = ( x2             ,  y2 + innerSize)
    val (x4, y4) = ( x3 - cornerSize,  y3 + cornerSize)
    val (x5, y5) = ( x4 - innerSize ,  y4)
    val (x6, y6) = ( x5 - cornerSize,  y5 - cornerSize)
    val (x7, y7) = ( x6             ,  y6 - innerSize)
    val points   = List((x0,y0), (x1,y1), (x2,y2), (x3,y3),
                        (x4,y4), (x5,y5), (x6,y6), (x7,y7))
    ImPolygon(points)
  }

  /**
   * Create a field of view consisting of three slits along the y axis.
   * @param slitWidth       the width of the slit, as determined by the TpeSciArea
   * @return                the shape matching the specifications
   */
  private def longSlitFOV(slitWidth: Double): Shape = {
    val slitHeight   = LongSlitFOVHeight
    val bridgeHeight = LongSlitFOVBridgeHeight

    val x = -slitWidth / 2
    val d = slitHeight + bridgeHeight

    val s = new Area
    (-1 to 1).foreach { i =>
      val y = - (slitHeight / 2) + (i * d)
      val slit = new Rectangle2D.Double(x, y, slitWidth, slitHeight)
      s.add(new Area(slit))
    }
    s
  }

  /**
   * Create the Nod & Shuffle field of view.
   * @param slitWidth       the width of the slit, as determined by the TpeSciArea
   * @return                the shape matching the specifications
   */
  private def nsFOV(slitWidth: Double): Shape = {
    val slitHeight = LongSlitFOVHeight
    val x = -slitWidth  / 2
    val y = -slitHeight / 2
    new Rectangle2D.Double(x, y, slitWidth, slitHeight)
  }

  /**
   * Create the IFU field of view.
   * @param fpu             the FPU type
   * @return                the shape matching the specifications
   */
  private def ifuFOV(fpu: GmosCommonType.FPUnit, isSouth: Boolean): Shape = {
    // The widths are slightly smaller for the following FPUnit types.
    val widthAdjust = {
      val fpus = Set[GmosCommonType.FPUnit](
        GmosSouthType.FPUnitSouth.IFU_N,
        GmosSouthType.FPUnitSouth.IFU_N_B,
        GmosSouthType.FPUnitSouth.IFU_N_R
      )
      if (fpus.contains(fpu)) -2 else 0
    }

    // These are used to center the right rectangle on the base position.
    val width0  = (IFUFOVLargerRectangle.rect.getWidth + widthAdjust) / 2.0
    val height0 =  IFUFOVLargerRectangle.rect.getHeight / 2.0

    // For the following FPUnit types, we show the left or right half of both slits, with the larger
    // (north) or smaller (south) of the two centered about the base position.
    val (widthFactor, xAdjust) = {
      val fpus = Set[GmosCommonType.FPUnit](
        GmosNorthType.FPUnitNorth.IFU_2,
        GmosNorthType.FPUnitNorth.IFU_3,
        GmosSouthType.FPUnitSouth.IFU_2,
        GmosSouthType.FPUnitSouth.IFU_3,
        GmosSouthType.FPUnitSouth.IFU_N_B,
        GmosSouthType.FPUnitSouth.IFU_N_R
      )
      if (fpus.contains(fpu)) (0.5, width0 / 2.0) else (1.0, 0.0)
    }

    // Create and add the two slits.
    val area = new Area
    for {
      fov <- IFUFOVs
      if fov.use(widthAdjust)

      rect = fov.rect
      width = (rect.getWidth + widthAdjust) * widthFactor
      height = rect.getHeight
      x = {
        val tx = rect.getX - width0 + xAdjust

        // OT-10: If we are in the case of the smaller rectangle for GMOS-S, we need to flip
        // along the X-axis about the base position.
        (isSouth, fov) match {
          case (true, IFUFOVSmallerRectangle) => -tx - width
          case _ => tx
        }
      }
      y = rect.getY - height0
      slit = new Rectangle2D.Double(x, y, width, height)
    } area.add(new Area(slit))
    area
  }
}


object GmosScienceAreaGeometry {
  // Various sizes used in the calculation of the science area in arcsec.
  val ImagingFOVSize          = 330.34
  val ImagingFOVInnerSize     = 131.33 * 2

  val MOSFOVSize              = ImagingFOVSize - 2 * 8.05
  val MOSFOVInnerSize         = 139.38 * 2

  val LongSlitFOVHeight       = 108.0
  val LongSlitFOVBridgeHeight = 3.2


  // IFU visualisation in TPE
  //
  // Currently TPE shows the true focal plane geometry which has fixed IFU sub-fields
  // offset either side of the base position. Whilst this is factually correct, what users
  // will expect is that the larger of the two sub-fields be positioned on their target (as
  // defined by the target component). This is the first case in which the instrument
  // aperture is not symmetric about the pointing position (in this case it is offset by ~
  // 30 arcsec from it).
  //
  // {Phil: proposed solution}
  // The tricky aspect is that the base position displayed in TPE currently has two
  // meanings (a) it shows the position of the target RA,dec and (b) it also shows the
  // telescope pointing direction (essentially the direction in which the mount points) and
  // is used as the origin for drawing the PWFS and OIWFS patrol fields. These need not be
  // the same for an off-axis. (A similar case will occur with the bHROS fibre feed).
  //
  // There are several options, only one of which will avoid completely confusing the
  // users. The proposed solution is: firstly, the larger IFU sub-aperture should be drawn
  // at the base position. Secondly, the PWFS and OIWFS patrol fields must be offset (by
  // the opposite amount that the IFU field is off-axis). This 'pseudo' base position (we
  // would call it the pointing position) is the centre for drawing the PWFS and OIWFS
  // patrol fields. (The pointing position need not be displayed in TPE).
  //
  // [source: bmiller e-mail 19Dec01 and 14Jan 02, generalised by ppuxley]**********
  //
  // Allan: Note: For GMOS-S it is the smaller rect that is centered on the base position
  // (The display is reversed).
  // The offset from the base position and the dimensions of the IFU FOV in arcsec.
  val IFUFOVOffset = 30.0

  // The rectangles that are used to define the IFU field of view. In some cases, we do not use both,
  // hence the use method to determine if they should be used in creating the science area.
  sealed case class IFUFOVRectangle(rect: Rectangle2D) {
    def use(widthAdjust: Int): Boolean = true
  }
  object IFUFOVSmallerRectangle extends IFUFOVRectangle(new Rectangle2D.Double(-30.0 - IFUFOVOffset, 0.0, 3.5, 5.0)) {
    override def use(widthAdjust: Int): Boolean =
      widthAdjust == 0
  }
  object IFUFOVLargerRectangle  extends IFUFOVRectangle(new Rectangle2D.Double( 30.0 - IFUFOVOffset, 0.0, 7.5, 5.0))
  val IFUFOVs = Set(IFUFOVSmallerRectangle, IFUFOVLargerRectangle)
}