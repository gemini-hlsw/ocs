package edu.gemini.spModel.gemini.gmos


import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.util.immutable.ImPolygon
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.AngleSyntax._
import edu.gemini.spModel.inst.ProbeArmGeometry
import edu.gemini.spModel.inst.ProbeArmGeometry.ArmAdjustment
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.telescope.IssPort

import java.awt.Shape
import java.awt.geom.{Area, Rectangle2D}

import scalaz._
import Scalaz._

object GmosOiwfsProbeArm extends ProbeArmGeometry {
  val instance = this

  // The following values (in arcsec) are used to calculate the position of the
  // OIWFS arm and are described in the paper "Opto-Mechanical Design of the
  // Gemini Multi-Object Spectrograph On-Instrument Wavefront Sensor".
  // Location of base stage in arcsec
  val PickoffArmLength      = 358.46
  val PickoffMirrorSize     =  20.00
  val ProbeArmLength        = PickoffArmLength - PickoffMirrorSize / 2.0
  val ProbeArmTaperedWidth  =  15.00
  val ProbeArmTaperedLength = 180.00

  val T   = Offset(427.52.arcsecs[OffsetP], 101.84.arcsecs[OffsetQ])

  // Length of stage arm in arcsec
  val BX  = 124.89
  val BX2 = BX * BX

  // Length of pick-off arm in arcsec
  val MX  = 358.46
  val MX2 = MX * MX

  import edu.gemini.spModel.inst.FeatureGeometry._

  override protected val guideProbeInstance = GmosOiwfsGuideProbe.instance

  override def unadjustedGeometry(ctx: ObsContext): Option[Shape] =
    Some(new Area(probeArm) <| (_.add(new Area(pickoffMirror))))

  private val probeArm: Shape = {
    val hm  = PickoffMirrorSize    / 2.0
    val htw = ProbeArmTaperedWidth / 2.0

    val (x0, y0) = (hm, -htw)
    val (x1, y1) = (x0 + ProbeArmTaperedLength, -hm)
    val (x2, y2) = (x0 + ProbeArmLength, y1)
    val (x3, y3) = (x2, hm)
    val (x4, y4) = (x1, y3)
    val (x5, y5) = (x0, htw)

    ImPolygon(List((x0, y0), (x1, y1), (x2, y2), (x3, y3), (x4, y4), (x5, y5)))
  }

  private val pickoffMirror: Shape = {
    val xy = -PickoffMirrorSize / 2.0
    new Rectangle2D.Double(xy, xy, PickoffMirrorSize, PickoffMirrorSize)
  }

  override def armAdjustment(ctx: ObsContext, guideStar: Coordinates, offset: Offset): Option[ArmAdjustment] = {
    import ProbeArmGeometry._

    val ifuOpt = ctx.getInstrument match {
      case gmosn: InstGmosNorth => Some(gmosn.getFPUnit.getWFSOffset)
      case gmoss: InstGmosSouth => Some(gmoss.getFPUnit.getWFSOffset)
      case _                    => None
    }

    for {
      gsOffset <- guideStarOffset(ctx, guideStar)
      ifu      <- ifuOpt
    } yield {
      val ifuOffset = Offset(ifu.arcsecs[OffsetP], OffsetQ.Zero)
      val flip = ctx.getIssPort == IssPort.SIDE_LOOKING
      val posAngle = ctx.getPositionAngle
      val angle = armAngle(posAngle, gsOffset, offset, ifuOffset, flip)
      ArmAdjustment(angle, gsOffset)
    }

  }

  /** Calculates the probe arm angle at the position angle for the given guide
    * star location and offset.
    *
    * @param posAngle  position angle
    * @param gsOffset  guide star position as an offset from the base
    * @param offset    offset from the base
    * @param ifuOffset value of any offset adjustment that may need to be made
    *                  for the instrument configuration
    * @param flip      flipped in the x-axis, i.e. if the instrument ISS port is
    *                  side looking
    * @return          angle of the probe arm
    */
  private def armAngle(posAngle: Angle,
                       gsOffset: Offset,
                       offset: Offset,
                       ifuOffset: Offset,
                       flip: Boolean): Angle = {
    val p = {
      val adj = (offset - ifuOffset).rotate(posAngle)
      val t   = (if (flip) T.flipQ else T).rotate(posAngle)
      t + gsOffset - adj
    }.toPoint

    val r     = math.hypot(p.getX, p.getY)
    val r2    = r * r

    val alpha = math.atan2(p.getX, p.getY)
    val phi   = {
      val acosArg = (r2 - (BX2 + MX2)) / (2 * BX * MX)
      val acosArgAdj = if (acosArg > 1.0) 1.0 else if (acosArg < -1.0) -1.0 else acosArg
      math.acos(acosArgAdj) * (if (flip) -1 else 1)
    }
    val theta = {
      val thetaP = math.asin((MX / r) * math.sin(phi))
      if (MX2 > (r2 + BX2)) math.Pi - thetaP else thetaP
    }

    Angle.fromRadians(phi - theta - alpha - math.Pi / 2.0)
  }
}