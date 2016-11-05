package edu.gemini.spModel.gemini.flamingos2

import java.awt.Shape
import java.awt.geom.{Area, Point2D, Rectangle2D}

import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.util.immutable.ImPolygon
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gems.Gems
import edu.gemini.spModel.inst.ProbeArmGeometry
import edu.gemini.spModel.inst.ProbeArmGeometry.ArmAdjustment
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._

import scalaz._
import Scalaz._

// The geometry is dependent on the plate scale determined by the choice of Lyot wheel,
// and thus we must use a class instead of an object.
object F2OiwfsProbeArm extends ProbeArmGeometry {
  // Simplified Java access.
  val instance = this

  import edu.gemini.spModel.inst.FeatureGeometry._

  override protected val guideProbeInstance = Flamingos2OiwfsGuideProbe.instance

  override def unadjustedGeometry(ctx: ObsContext): Option[Shape] =
    ctx.getInstrument match {
      case f2: Flamingos2 => Some(f2Geometry(f2))
      case _              => None
    }

  private def f2Geometry(f2: Flamingos2): Shape = {
    val probeArm: Shape = {
      val plateScale = f2.getLyotWheel.getPlateScale
      val scaledLength = ProbePickoffArmLength * plateScale
      val hm = PickoffMirrorSize * plateScale / 2.0
      val htw = ProbeArmTaperedWidth / 2.0

      val (x0, y0) = (hm, -htw)
      val (x1, y1) = (x0 + ProbeArmTaperedLength, -hm)
      val (x2, y2) = (x0 + scaledLength, y1)
      val (x3, y3) = (x2, hm)
      val (x4, y4) = (x1, y3)
      val (x5, y5) = (x0, htw)

      val points = List((x0, y0), (x1, y1), (x2, y2), (x3, y3), (x4, y4), (x5, y5))
      ImPolygon(points)
    }

    val pickoffMirror: Shape = {
      val plateScale = f2.getLyotWheel.getPlateScale
      val scaledMirrorSize = PickoffMirrorSize * plateScale
      val xy = -scaledMirrorSize / 2.0
      new Rectangle2D.Double(xy, xy, scaledMirrorSize, scaledMirrorSize)
    }

    new Area(probeArm) <| (_.add(new Area(pickoffMirror)))
  }

  // Size of probe arm components in mm.
  val PickoffMirrorSize          = 19.8
  val ProbePickoffArmTotalLength = 203.40
  val ProbeBaseArmLength         = 109.63
  val ProbePickoffArmLength      = ProbePickoffArmTotalLength - PickoffMirrorSize/2.0
  val ProbeArmOffset             = 256.87

  // Width and length of tapered end of probe arm in arcsec.
  val ProbeArmTaperedWidth       = 15.0
  val ProbeArmTaperedLength      = 180.0

  /**
   * For a given context, guide star coordinates, and offset, calculate the arm adjustment that will be used for the
   * guide star at those coordinates.
   * @param ctx       context representing the configuration
   * @param guideStar guide star for which to calculate the adjustment
   * @param offset    offset for which to calculate the adjustment
   * @return          probe arm adjustments for this data
   */
  override def armAdjustment(ctx: ObsContext, guideStar: Coordinates, offset: Offset): Option[ArmAdjustment] = {
    import ProbeArmGeometry._

    val gemsFlag = ctx.getAOComponent.asScalaOpt.fold(false){ ao =>
      val aoNarrowType = ao.getNarrowType
      aoNarrowType.equals(Gems.SP_TYPE.narrowType)
    }

    val flamingos2  = ctx.getInstrument.asInstanceOf[Flamingos2]
    val flip        = flamingos2.getFlipConfig(gemsFlag)
    val posAngle    = ctx.getPositionAngle
    val fovRotation = flamingos2.getRotationConfig(gemsFlag).toNewModel
    guideStarOffset(ctx, guideStar).map { gsOffset =>
      val angle = armAngle(posAngle, fovRotation, gsOffset, offset, flip, flamingos2.getLyotWheel.getPlateScale)
      ArmAdjustment(angle, gsOffset)
    }
  }


  private def armAngle(posAngle:    Angle,
                       fovRotation: Angle,
                       gsOffset:    Offset,
                       offset:      Offset,
                       flip:        Boolean,
                       plateScale:  Double): Angle = {
    val Q = {
      val P = {
        val scaledFlippedPAO = {
          val scaledPAO = ProbeArmOffset * plateScale
          if (flip) -scaledPAO else scaledPAO
        }
        val angle = (posAngle + fovRotation) * -1
        new Point2D.Double(scaledFlippedPAO * math.cos(angle.toRadians), scaledFlippedPAO * math.sin(angle.toRadians))
      }

      val guideStar = (gsOffset - offset.rotate(posAngle)).toPoint
      val D = new Point2D.Double(guideStar.getX - P.getX, guideStar.getY - P.getY)

      val scaledPBAL = ProbeBaseArmLength * plateScale
      val scaledPPAL = ProbePickoffArmLength * plateScale
      val distance   = math.min(math.sqrt(D.getX * D.getX + D.getY * D.getY), scaledPBAL + scaledPPAL)
      val a = (scaledPBAL * scaledPBAL - scaledPPAL * scaledPPAL + distance * distance) / (2 * distance)
      val h = (if (flip) -1 else 1) * math.sqrt(scaledPBAL * scaledPBAL - a * a)
      new Point2D.Double(-guideStar.getX + P.getX + (a * D.getX + h * D.getY) / distance,
                         -guideStar.getY + P.getY + (a * D.getY - h * D.getX) / distance)
    }
    Angle.fromRadians(math.atan2(Q.getY, Q.getX))
  }
}