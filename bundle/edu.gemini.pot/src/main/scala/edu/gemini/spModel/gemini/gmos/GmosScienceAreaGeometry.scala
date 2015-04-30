package edu.gemini.spModel.gemini.gmos

import edu.gemini.shared.util.immutable.ImPolygon
import edu.gemini.spModel.inst.ScienceAreaGeometry
import edu.gemini.spModel.gemini.gmos.GmosCommonType._
import edu.gemini.spModel.gemini.gmos.GmosCommonType.FPUnitMode.{BUILTIN, CUSTOM_MASK}
import edu.gemini.spModel.obs.context.ObsContext

import java.awt.Shape
import java.awt.geom.{AffineTransform, Rectangle2D, Area}

import scalaz._
import Scalaz._

object GmosScienceAreaGeometry extends ScienceAreaGeometry {
  val instance = this

  // Various sizes used in the calculation of the science area in arcsec.
  val ImagingFovSize          = 330.34
  val ImagingFovInnerSize     = 131.33 * 2

  val ImagingCenterCcdWidth   = 165.60
  val ImagingGapWidth         =   3.00

  val MosFovSize              = ImagingFovSize - 2 * 8.05
  val MOSFovInnerSize         = 139.38 * 2

  val LongSlitFovHeight       = 108.00
  val LongSlitFovBridgeHeight =   3.20

  override def unadjustedGeometry(ctx: ObsContext): Option[Shape] = {
    def gmosGeo[P <: FPUnit](mode: FPUnitMode, fpu: P, isSouth: Boolean): Option[Shape] =
      mode match {
        case BUILTIN if fpu.isImaging       => Some(imagingFov.toShape)
        case BUILTIN if fpu.isSpectroscopic => Some(longSlitFOV(scienceAreaDimensions(fpu)._1))
        case BUILTIN if fpu.isIFU           => Some(ifuFOV(fpu, isSouth))
        case BUILTIN if fpu.isNS            => Some(nsFOV(scienceAreaDimensions(fpu)._1))
        case CUSTOM_MASK                    => Some(mosFov.toShape)
        case _                              => None
      }

    ctx.getInstrument match {
      case gn: InstGmosNorth => gmosGeo(gn.getFPUnitMode, gn.getFPUnit, isSouth = false)
      case gs: InstGmosSouth => gmosGeo(gs.getFPUnitMode, gs.getFPUnit, isSouth = true )
      case _                 => None
    }
  }

  def scienceAreaDimensions[P <: FPUnit](fpu: P): (Double, Double) = {
    val width = fpu.getWidth
    if (width != -1) (width, ImagingFovSize) else (ImagingFovSize, ImagingFovSize)
  }

  def javaScienceAreaDimensions[P <: FPUnit](fpu: P): Array[Double] =
    scienceAreaDimensions(fpu) |> { case (w,h) => Array(w,h) }

  case class ImagingFov(ccdLeft: Shape, ccdCenter: Shape, ccdRight: Shape) {
    def toList: List[Shape] = List(ccdLeft, ccdCenter, ccdRight)

    def toShape: Shape =
      new Area(ccdLeft) <| (_.add(new Area(ccdCenter))) <| (_.add(new Area(ccdRight)))
  }

  private def fov(size: Double, innerSize: Double): ImagingFov = {
    val half     = size / 2
    val corner   = (size - innerSize) / 2
    val lrWidth  = (size - ImagingCenterCcdWidth - ImagingGapWidth * 2) / 2

    val left = {
      val (x0, y0) = (-half,           -half + corner)
      val (x1, y1) = (-half + corner,  -half)
      val (x2, y2) = (-half + lrWidth, -half)
      val (x3, y3) = (x2,              -y2)
      val (x4, y4) = (x1,              -y1)
      val (x5, y5) = (x0,              -y0)
      ImPolygon(List((x0,y0), (x1,y1), (x2,y2), (x3,y3), (x4,y4), (x5,y5)))
    }

    val right   = AffineTransform.getScaleInstance(-1.0, 1.0).createTransformedShape(left)
    val center  = new Rectangle2D.Double(-ImagingCenterCcdWidth/2, -half, ImagingCenterCcdWidth, size)

    ImagingFov(left, center, right)
  }

  val imagingFov = fov(ImagingFovSize, ImagingFovInnerSize)
  val mosFov     = fov(MosFovSize, MOSFovInnerSize)

  /**
   * Create a field of view consisting of three slits along the y axis.
   * @param slitWidth       the width of the slit, as determined by the TpeSciArea
   * @return                the shape matching the specifications
   */
  private def longSlitFOV(slitWidth: Double): Shape = {
    val slitHeight   = LongSlitFovHeight
    val bridgeHeight = LongSlitFovBridgeHeight

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
    val slitHeight = LongSlitFovHeight
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
  val IfuFovOffset = 30.0

  // The rectangles that are used to define the IFU field of view. In some cases, we do not use both,
  // hence the use method to determine if they should be used in creating the science area.
  sealed case class IFUFOVRectangle(rect: Rectangle2D) {
    def use(widthAdjust: Int): Boolean = true
  }
  object IFUFOVSmallerRectangle extends IFUFOVRectangle(new Rectangle2D.Double(-30.0 - IfuFovOffset, 0.0, 3.5, 5.0)) {
    override def use(widthAdjust: Int): Boolean =
      widthAdjust == 0
  }
  object IFUFOVLargerRectangle  extends IFUFOVRectangle(new Rectangle2D.Double( 30.0 - IfuFovOffset, 0.0, 7.5, 5.0))
  val IFUFOVs = Set(IFUFOVSmallerRectangle, IFUFOVLargerRectangle)
}