package edu.gemini.spModel.gemini.gmos

import java.awt.Shape
import java.awt.geom.{Point2D, AffineTransform}

import edu.gemini.shared.util.immutable.{DefaultImList, ImList, ImPolygon}
import edu.gemini.skycalc.{Angle, CoordinateDiff, Offset}
import edu.gemini.spModel.inst.ProbeArmGeometry
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.system.CoordinateParam
import edu.gemini.spModel.telescope.IssPort


import scala.collection.JavaConverters._

object GmosOiwfsProbeArm extends ProbeArmGeometry {
  import edu.gemini.spModel.inst.FeatureGeometry.transformPoint
  import GmosOiwfsProbeArm._

  val instance = this

  def geometry: List[Shape] =
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

  override def armAdjustment(ctx: ObsContext, offset: Offset): Option[(Double, Point2D)] = {
    if (ctx == null || offset == null) None
    else {
      for {
        gts <- ctx.getTargets.getPrimaryGuideProbeTargets(GmosOiwfsGuideProbe.instance).asScalaOpt
        gt  <- gts.getPrimary.asScalaOpt
      } yield {
        val flip = if (ctx.getIssPort == IssPort.SIDE_LOOKING) -1.0 else 1.0

        val pa        = ctx.getPositionAngle.toRadians.getMagnitude
        val offsetPt  = new Point2D.Double(-offset.p.toArcsecs.getMagnitude, -offset.q.toArcsecs.getMagnitude * flip)
        val guideStar = {
          val o     = new CoordinateDiff(ctx.getBaseCoordinates, gt.getTarget.getSkycalcCoordinates).getOffset
          val (x,y) = (o.p.toArcsecs.getMagnitude, o.q.toArcsecs.getMagnitude)
          new Point2D.Double(-x, -y * flip)
        }

        ctx.getInstrument match {
          case gmosn: InstGmosNorth => (armAngle(gmosn.getFPUnit.getWFSOffset, pa, guideStar, offsetPt), guideStar)
          case gmoss: InstGmosSouth => (armAngle(gmoss.getFPUnit.getWFSOffset, pa, guideStar, offsetPt), guideStar)
          case _                    => null
        }
      }
    }
  }

  /**
   * Return the arm adjustment parameters in a format easily usable by Java.
   * @see          armAdjustment
   * @param ctx    the observation context
   * @param offset the offset to use
   * @return       a pair comprising the angle in radians of the probe arm and the position of the guide star in arcsec
   */
  def armAdjustmentForJava(ctx: ObsContext, offset: Offset): edu.gemini.shared.util.immutable.Option[edu.gemini.shared.util.immutable.Pair[java.lang.Double, Point2D]] =
    armAdjustment(ctx, offset: Offset).map{ case (angle, guideStar) => new edu.gemini.shared.util.immutable.Pair(new java.lang.Double(angle.toDouble), guideStar) }.asGeminiOpt

  /**
   * Calculate the probe arm angle at the position angle (radians) for the given guide star location
   * and offset, specified in arcsec.
   * @param posAngle  the position angle in radians
   * @param guideStar the guide star position in arcsec
   * @param offset    the offset in arcsec
   * @return          the angle of the probe arm in radians
   */
  private def armAngle(wfsOffset: Double,
                       posAngle:    Double,
                       guideStar:   Point2D,
                       offset:      Point2D): Double = {
    val offsetAdj = {
      val posAngleRot = AffineTransform.getRotateInstance(posAngle)
      val ifuOffset = transformPoint(new Point2D.Double(wfsOffset, 0.0), posAngleRot)
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