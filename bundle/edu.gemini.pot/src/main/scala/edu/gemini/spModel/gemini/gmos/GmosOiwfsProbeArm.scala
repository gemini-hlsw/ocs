package edu.gemini.spModel.gemini.gmos

import java.awt.Shape
import java.awt.geom.{Point2D, AffineTransform}

import edu.gemini.shared.util.immutable.ImPolygon
import edu.gemini.spModel.inst.{FeatureGeometry, GuideProbeGeometry}

class GmosOiwfsProbeArm[I  <: InstGmosCommon[D,F,P,SM],
                        D  <: Enum[D]  with GmosCommonType.Disperser,
                        F  <: Enum[F]  with GmosCommonType.Filter,
                        P  <: Enum[P]  with GmosCommonType.FPUnit,
                        SM <: Enum[SM] with GmosCommonType.StageMode](inst: I) extends GuideProbeGeometry {
  import FeatureGeometry.transformPoint
  import GmosOiwfsProbeArm._

  override protected lazy val probeArm: Shape = {
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

  override protected lazy val pickoffMirror: Shape = {
    val (x0, y0) = (-PickoffMirrorSize / 2.0, -PickoffMirrorSize / 2.0)
    val (x1, y1) = (x0 + PickoffMirrorSize,   y0)
    val (x2, y2) = (x1,                       y1 + PickoffMirrorSize)
    val (x3, y3) = (x0,                       y2)

    val points = List((x0,y0), (x1,y1), (x2,y2), (x3,y3))
    ImPolygon(points)
  }

  /**
   * Calculate the probe arm angle at the position angle (radians) for the given guide star location
   * and offset, specified in arcsec.
   * @param posAngle  the position angle in radians
   * @param guideStar the guide star position in arcsec
   * @param offset    the offset in arcsec
   * @return          the angle of the probe arm in radians
   */
  private def armAngle(posAngle:    Double,
                       guideStar:   Point2D,
                       offset:      Point2D): Double = {
    val offsetAdj = {
      val posAngleRot = AffineTransform.getRotateInstance(posAngle)
      val ifuOffset = transformPoint(new Point2D.Double(inst.getFPUnit.getWFSOffset, 0.0), posAngleRot)
      transformPoint(offset, AffineTransform.getTranslateInstance(ifuOffset.getX, ifuOffset.getY))
    }

    val p  = transformPoint(guideStar, AffineTransform.getTranslateInstance(T.getX - offsetAdj.getX, T.getY - offsetAdj.getY))
    val r  = math.sqrt(p.getX * p.getX + p.getY * p.getY)

    val alpha = math.atan2(p.getX, p.getY)
    val phi = {
      val acosArg    = (r*r - (BX2 + MX2)) / (2 * BX * MX)
      val acosArgAdj = if (acosArg > 1.0) 1.0 else if (acosArg < -1.0) -1.0 else acosArg
      math.acos(acosArgAdj)
    }
    val theta = {
      val thetaP = math.asin((MX / r) * math.sin(phi))
      if (MX2 > (r*r + BX2)) math.Pi - thetaP else thetaP
    }
    phi - theta - alpha - math.Pi / 2.0
  }

  override def geometryForParams(posAngle:        Double,
                                 guideStar:       Point2D,
                                 offset:          Point2D): List[Shape] = {
    val angle = armAngle(posAngle, guideStar, offset)
    val armTrans = AffineTransform.getRotateInstance(angle, guideStar.getX, guideStar.getY)
    armTrans.concatenate(AffineTransform.getTranslateInstance(guideStar.getX, guideStar.getY))
    transformedGeometry(armTrans)
  }
}

object GmosOiwfsProbeArm {
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