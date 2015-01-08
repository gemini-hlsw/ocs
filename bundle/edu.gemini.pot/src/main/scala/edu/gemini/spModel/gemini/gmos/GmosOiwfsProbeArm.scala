package edu.gemini.spModel.gemini.gmos

import java.awt.Shape
import java.awt.geom.{Point2D, AffineTransform}

import edu.gemini.shared.util.immutable.ImPolygon
import edu.gemini.spModel.inst.{FeatureGeometry, GuideProbeGeometry}
import edu.gemini.spModel.util.Angle

class GmosOiwfsProbeArm[I  <: InstGmosCommon[D,F,P,SM],
                        D  <: Enum[D]  with GmosCommonType.Disperser,
                        F  <: Enum[F]  with GmosCommonType.Filter,
                        P  <: Enum[P]  with GmosCommonType.FPUnit,
                        SM <: Enum[SM] with GmosCommonType.StageMode](inst: I) extends GuideProbeGeometry {
  import FeatureGeometry.transformPoint
  import GmosOiwfsProbeArm._

  override def probeArm: Shape = {
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

  override def pickoffMirror: Shape = {
    val (x0, y0) = (-PickoffMirrorSize / 2.0, -PickoffMirrorSize / 2.0)
    val (x1, y1) = (x0 + PickoffMirrorSize,   y0)
    val (x2, y2) = (x1,                       y1 + PickoffMirrorSize)
    val (x3, y3) = (x0,                       y2)

    val points = List((x0,y0), (x1,y1), (x2,y2), (x3,y3))
    ImPolygon(points)
  }

  def armAngle(posAngle:    Double,
               guideStar:   Point2D,
               offset:      Point2D,
               xFlipFactor: Double,
               raFactor:    Double): Double = {
    // TODO: Can we get rid of raFactor or xFlipFactor somehow?
    // TODO: We can collapse TX and TZ into a single variable
    val posAngleRot = AffineTransform.getRotateInstance(posAngle)

    val offsetAdj = {
      val ifuTrans  = AffineTransform.getScaleInstance(raFactor, 1.0)
      ifuTrans.concatenate(posAngleRot)
      val ifuOffset = transformPoint(new Point2D.Double(inst.getFPUnit.getWFSOffset, 0.0), ifuTrans)
      transformPoint(offset, AffineTransform.getTranslateInstance(ifuOffset.getX, ifuOffset.getY))
    }

    val t  = transformPoint(new Point2D.Double(TX, TZ), AffineTransform.getScaleInstance(raFactor, xFlipFactor))
    val p  = transformPoint(guideStar, AffineTransform.getTranslateInstance(t.getX - offsetAdj.getX, t.getY - offsetAdj.getY))
    val r  = math.sqrt(p.getX * p.getX + p.getY * p.getY)
    val mx = MX * raFactor
    val bx = BX


    val alpha = math.atan2(p.getX, p.getY)
    val phi = {
      val acosArg    = (r*r - (bx*bx + mx*mx)) / (2 * bx * mx)
      val acosArgAdj = if (acosArg > 1.0) 1.0 else if (acosArg < -1.0) -1.0 else acosArg
      xFlipFactor * math.acos(acosArgAdj)
    }
    val theta = {
      val thetaP = math.asin((mx / r) * math.sin(phi))
      if (mx * mx > (r*r + bx*bx)) math.Pi - thetaP else thetaP
    }
    phi - theta - alpha - math.Pi / 2.0
  }

  override def geometryForParams(posAngle:        Double,
                                 guideStar:       Point2D,
                                 offset:          Point2D,
                                 xFlipArm:        Boolean,
                                 raFactor:        Double): List[Shape] = {
    val xFlipFactor = if (xFlipArm) -1.0 else 1.0
    val angle = armAngle(posAngle, guideStar, offset, xFlipFactor, raFactor)

    // Create the transformation for the geometry.
    val armTrans = AffineTransform.getRotateInstance(angle, guideStar.getX, guideStar.getY)
    armTrans.concatenate(AffineTransform.getTranslateInstance(guideStar.getX, guideStar.getY))
    armTrans.concatenate(AffineTransform.getScaleInstance(raFactor, 1.0))
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

  // Location of base stage in arcsec
  private val TX = -427.52
  private val TZ = -101.84

  // Length of stage arm in arcsec
  private val BX =  124.89

  // Length of pick-off arm in arcsec
  private val MX =  358.46
}