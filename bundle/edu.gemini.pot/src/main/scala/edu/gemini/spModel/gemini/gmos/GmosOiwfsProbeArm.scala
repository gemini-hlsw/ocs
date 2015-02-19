package edu.gemini.spModel.gemini.gmos

import java.awt.Shape
import java.awt.geom.{Point2D, AffineTransform}

import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.util.immutable.ImPolygon
import edu.gemini.skycalc.Offset
import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.inst.{ArmAdjustment, ProbeArmGeometry}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.telescope.IssPort

object GmosOiwfsProbeArm extends ProbeArmGeometry {
  import edu.gemini.spModel.inst.FeatureGeometry.transformPoint

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
    val (x0, y0) = (-PickoffMirrorSize / 2.0, -PickoffMirrorSize / 2.0)
    val (x1, y1) = (x0 + PickoffMirrorSize,   y0)
    val (x2, y2) = (x1,                       y1 + PickoffMirrorSize)
    val (x3, y3) = (x0,                       y2)

    val points = List((x0,y0), (x1,y1), (x2,y2), (x3,y3))
    ImPolygon(points)
  }

  override def armAdjustment(ctx: ObsContext, guideStarCoords: Coordinates, offset: Offset): Option[ArmAdjustment] = {
    if (ctx == null || offset == null) None
    else {
      import ProbeArmGeometry.CanonicalPoint
      val flip        = if (ctx.getIssPort == IssPort.SIDE_LOOKING) -1.0 else 1.0
      val posAngle    = ctx.getPositionAngle.toRadians.getMagnitude
      val offsetPt    = new Point2D.Double(-offset.p.toArcsecs.getMagnitude, -offset.q.toArcsecs.getMagnitude * flip)
      val guideStarPt = {
        val baseCoords = ctx.getBaseCoordinates.toNewModel
        val o = Coordinates.difference(baseCoords, guideStarCoords).offset
        val (x,y) = (o.p.toArcsecs, o.q.toArcsecs)
        new Point2D.Double(-x, -y * flip).toCanonicalForm
      }
      val wfsOffset   = {
        ctx.getInstrument match {
          case gmosn: InstGmosNorth => Some(gmosn.getFPUnit.getWFSOffset)
          case gmoss: InstGmosSouth => Some(gmoss.getFPUnit.getWFSOffset)
          case _                    => None
        }
      }
      wfsOffset.map { w =>
        val angle = armAngle(w, posAngle, guideStarPt, offsetPt)
        ArmAdjustment(angle, guideStarPt)
      }
    }
  }

  /**
   * Calculate the probe arm angle at the position angle (radians) for the given guide star location
   * and offset, specified in arcsec.
   * @param posAngle  the position angle in radians
   * @param guideStar the guide star position in arcsec
   * @param offset    the offset in arcsec
   * @return          the angle of the probe arm in radians
   */
  private def armAngle(wfsOffset: Double,
                       posAngle:  Double,
                       guideStar: Point2D,
                       offset:    Point2D): Double = {
    import ProbeArmGeometry._

    val offsetAdj = {
      val posAngleRot = AffineTransform.getRotateInstance(posAngle)
      val ifuOffset = transformPoint(new Point2D.Double(wfsOffset, 0.0), posAngleRot)
      transformPoint(offset, AffineTransform.getTranslateInstance(ifuOffset.getX, ifuOffset.getY))
    }

    val p  = transformPoint(guideStar, AffineTransform.getTranslateInstance(T.getX - offsetAdj.getX, T.getY - offsetAdj.getY)).toCanonicalForm
//    {
//      val tp = transformPoint(guideStar, AffineTransform.getTranslateInstance(T.getX - offsetAdj.getX, T.getY - offsetAdj.getY))
//
//      // Normalize the point if necessary, as arcsecs should range between 0 and 1296000 = 360 * 60 * 60.
//      val maxArcsec = 360 * 60 * 60
//      val nx = tp.getX % maxArcsec
//      val ny = tp.getY % maxArcsec
//      new Point2D.Double(nx, ny)
//    }
    val r  = math.sqrt(p.getX * p.getX + p.getY * p.getY)
    val r2 = r*r

    val alpha = math.atan2(p.getX, p.getY)
    val phi = {
      val acosArg    = (r2 - (BX2 + MX2)) / (2 * BX * MX)
      val acosArgAdj = if (acosArg > 1.0) 1.0 else if (acosArg < -1.0) -1.0 else acosArg
      math.acos(acosArgAdj)
    }
    val theta = {
      val thetaP = math.asin((MX / r) * math.sin(phi))
      if (MX2 > (r2 + BX2)) math.Pi - thetaP else thetaP
    }

    phi - theta - alpha - math.Pi / 2.0
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