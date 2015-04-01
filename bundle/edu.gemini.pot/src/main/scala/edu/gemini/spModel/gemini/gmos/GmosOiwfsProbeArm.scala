package edu.gemini.spModel.gemini.gmos

import java.awt.Shape
import java.awt.geom.{Rectangle2D, Point2D, AffineTransform}

import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.util.immutable.ImPolygon
import edu.gemini.skycalc.Offset
import edu.gemini.spModel.core.{Angle, Coordinates}
import edu.gemini.spModel.inst.{ArmAdjustment, ProbeArmGeometry}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.telescope.IssPort

object GmosOiwfsProbeArm extends ProbeArmGeometry {
  import edu.gemini.spModel.inst.FeatureGeometry.transformPoint

  // For simplified access from Java.
  val instance = this

  override protected val guideProbeInstance = GmosOiwfsGuideProbe.instance

  override def geometry: List[Shape] =
    List(probeArm, pickoffMirror)

  private lazy val probeArm: Shape = {
    val hm  = PickoffMirrorSize / 2.0
    val htw = ProbeArmTaperedWidth / 2.0

    val (x0, y0) = (hm,                         -htw)
    val (x1, y1) = (x0 + ProbeArmTaperedLength, -hm)
    val (x2, y2) = (x0 + ProbeArmLength,         y1)
    val (x3, y3) = (x2,                          hm)
    val (x4, y4) = (x1,                          y3)
    val (x5, y5) = (x0,                          htw)

    val points = List((x0,y0), (x1,y1), (x2,y2), (x3,y3), (x4,y4), (x5,y5))
    ImPolygon(points)
  }

  private lazy val pickoffMirror: Shape = {
    val xy = -PickoffMirrorSize / 2.0
    new Rectangle2D.Double(xy, xy, PickoffMirrorSize, PickoffMirrorSize)
  }

  override def armAdjustment(ctx0: ObsContext,
                             guideStarCoords: Coordinates,
                             offset0: Offset,
                             T: Point2D): Option[ArmAdjustment] = {
    import ProbeArmGeometry._

    for {
      ctx       <- Option(ctx0)
      offset    <- Option(offset0)
      wfsOffset <- ctx.getInstrument match {
        case gmosn: InstGmosNorth => Some(gmosn.getFPUnit.getWFSOffset)
        case gmoss: InstGmosSouth => Some(gmoss.getFPUnit.getWFSOffset)
        case _                    => None
      }
    } yield {
      val flip        = if (ctx.getIssPort == IssPort.SIDE_LOOKING) -1 else 1
      val posAngle    = ctx.getPositionAngle.toRadians.getMagnitude
      val offsetPt    = new Point2D.Double(-offset.p.toArcsecs.getMagnitude, -offset.q.toArcsecs.getMagnitude)
      val guideStarPt = guideStarPoint(ctx, guideStarCoords)
      val angle       = armAngle(wfsOffset, posAngle, guideStarPt, offsetPt, flip)
      ArmAdjustment(angle, guideStarPt)
    }
  }

  /**
   * Calculate the probe arm angle at the position angle (radians) for the given guide star location
   * and offset, specified in arcsec.
   * @param wfsOffset the value of any WFS offset adjustment that may need to be made for the instrument configuration
   * @param posAngle  the position angle in radians
   * @param guideStar the guide star position in arcsec
   * @param offset    the offset in arcsec
   * @param flip      whether or not things should be flipped in the x-axis, i.e. if the instrument ISS port is
   *                  side looking
   * @return          the angle of the probe arm in radians
   */
  private def armAngle(wfsOffset: Double,
                       posAngle:  Double,
                       guideStar: Point2D,
                       offset:    Point2D,
                       flip:      Int): Angle = {
    val p  = {
      val posAngleRot = AffineTransform.getRotateInstance(-posAngle)

      // The final adjusted offset as modified by the offset adjustment required by the IFU / WFS.
      val offsetAdj = {
        val ifuOffset = transformPoint(new Point2D.Double(wfsOffset, 0.0), posAngleRot)
        new Point2D.Double(offset.getX - ifuOffset.getX, offset.getY - ifuOffset.getY)
      }

      // Flip T if necessary and rotate by the position angle.
      val Tp = {
        val Ttrans = transformPoint(T, AffineTransform.getScaleInstance(1, flip))
        transformPoint(Ttrans, posAngleRot)
      }

      transformPoint(guideStar, AffineTransform.getTranslateInstance(Tp.getX + offsetAdj.getX, Tp.getY + offsetAdj.getY))
    }

    val r  = math.sqrt(p.getX * p.getX + p.getY * p.getY)
    val r2 = r*r

    // Here we may need to flip y based on ISSPort?
    val alpha = math.atan2(p.getX, p.getY)
    val phi = {
      val acosArg    = (r2 - (BX2 + MX2)) / (2 * BX * MX)
      val acosArgAdj = if (acosArg > 1.0) 1.0 else if (acosArg < -1.0) -1.0 else acosArg
      math.acos(acosArgAdj) * flip
    }
    val theta = {
      val thetaP = math.asin((MX / r) * math.sin(phi))
      if (MX2 > (r2 + BX2)) math.Pi - thetaP else thetaP
    }

    Angle.fromRadians(phi - theta - alpha - math.Pi / 2.0)
  }

  // Various measurements in arcsec.
  private val PickoffArmLength      = 358.46
  private val PickoffMirrorSize     =  20.0
  private val ProbeArmLength        = PickoffArmLength - PickoffMirrorSize / 2.0
  private val ProbeArmTaperedWidth  =  15.0
  private val ProbeArmTaperedLength = 180.0

  // The following values (in arcsec) are used to calculate the position of the OIWFS arm
  // and are described in the paper "Opto-Mechanical Design of the Gemini Multi-Object
  // Spectrograph On-Instrument Wavefront Sensor".
  // Location of base stage in arcsec
  private val T = new Point2D.Double(-427.52, -101.84)

  // Length of stage arm in arcsec
  private val BX  = 124.89
  private val BX2 = BX * BX

  // Length of pick-off arm in arcsec
  private val MX  = 358.46
  private val MX2 = MX * MX
}
