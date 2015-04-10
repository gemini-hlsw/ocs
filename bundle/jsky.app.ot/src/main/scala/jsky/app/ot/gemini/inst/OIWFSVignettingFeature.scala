package jsky.app.ot.gemini.inst

import java.awt.geom.{AffineTransform, Point2D}
import java.awt.{AlphaComposite, Color}
import java.text.DecimalFormat

import edu.gemini.skycalc.{Coordinates, Angle, Offset}
import edu.gemini.spModel.gemini.flamingos2.{Flamingos2, F2OiwfsProbeArm$, F2OiwfsProbeArm, Flamingos2OiwfsGuideProbe}
import edu.gemini.spModel.gemini.gmos.{GmosSouthOiwfsProbeArm, GmosNorthOiwfsProbeArm, GmosOiwfsGuideProbe, GmosOiwfsProbeArm}
import edu.gemini.spModel.guide.{OffsetValidatingGuideProbe, PatrolField, GuideProbe}
import edu.gemini.spModel.inst.{FeatureGeometry, ProbeArmGeometry}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.GuideProbeTargets
import jsky.app.ot.tpe.TpeImageFeature.Figure
import jsky.app.ot.tpe.{TpeImageInfo, TpeMessage}

import scala.collection.JavaConverters._


sealed class OIWFSVignettingFeature[I <: SPInstObsComp](probe: OffsetValidatingGuideProbe,
                                                        probeArmGeometry: ProbeArmGeometry[I],
                                                        probeName: String)
  extends OIWFS_FeatureBase(probeName, s"Show the $probeName patrol field and arm.") {

  import OIWFSVignettingFeature._

  /**
   * Add the OIWFS patrol field to the list of figures to display.
   *
   * @param basePosX the X screen coordinate for the base position to use
   * @param basePosY the Y screen coordinate for the base position to use
   * @param adder    the function to use to add the PatrolField: this allows us to
   *                 generalize and add regular and constrained patrol fields
   */
  protected def addPatrolFieldAtBase(basePosX: Double, basePosY: Double, adder: PatrolField => Unit): Unit = {
    lazy val offsets = getContext.offsets.scienceOffsets
    for {
      ctx <- _iw.getMinimalObsContext.asScalaOpt
      patrolField <- probe.getCorrectedPatrolField(ctx).asScalaOpt
    } {
      val rotation    = new Angle(-_posAngle, Angle.Unit.RADIANS)
      val translation = new Point2D.Double(basePosX, basePosY)
      setTransformationToScreen(rotation, _pixelsPerArcsec, translation)
      adder(patrolField)
      addPatrolField(patrolField)
    }
  }

  /**
   * Add the OIWFS patrol field to the list of figures to display.
   *
   * @param basePosX the X screen coordinate for the base position to use
   * @param basePosY the Y screen coordinate for the base position to use
   */
  protected def addPatrolFieldAtBase(basePosX: Double, basePosY: Double): Unit =
    addPatrolFieldAtBase(basePosX, basePosY, addPatrolField)

  /**
   * Add the patrol field constrained by the offsets to the list of figures to display.
   *
   * @param basePosX the X screen coordinate for the base position to use
   * @param basePosY the Y screen coordinate for the base position to use
   */
  protected def addOffsetConstrainedPatrolFieldAtBase(basePosX: Double, basePosY: Double): Unit = {
    lazy val offsets = getContext.offsets.scienceOffsetsJava
    addPatrolFieldAtBase(basePosX, basePosY, addOffsetConstrainedPatrolField(_, offsets))
  }

  /**
   * Get the offset that should be used as the "base" for drawing the probe arm. This evaluates as follows:
   * 1. The selected offset position, if any; or
   * 2. The default offset position (i.e. the first in the list of offsets); or
   * 3. A zero offset, corresponding to the base.
   * @return the offset to use
   */
  protected def probeArmOffset: Offset = {
    (for {
      ctx            <- Option(getContext)
      selectedOffset <- ctx.offsets.selectedPos
    } yield selectedOffset.toSkycalcOffset).getOrElse(Offset.ZERO_OFFSET)
  }

  /**
   * Add the probe arm to the list of figures.
   *
   * @param offsetPosX the X screen coordinate for the selected offset
   * @param offsetPosY the X screen coordinate for the selected offset
   * @param translateX translate resulting figure by this amount of pixels in X
   * @param translateY translate resulting figure by this amount of pixels in Y
   * @param basePosX   the X screen coordinate for the base position
   * @param basePosY   the Y screen coordinate for the base position
   */
  protected def addProbeArm(offsetPosX: Double, offsetPosY: Double,
                            translateX: Double, translateY: Double,
                            basePosX:   Double, basePosY:   Double): Unit = {
    for {
      ctx <- _iw.getMinimalObsContext.asScalaOpt
      if probe.inRange(ctx, probeArmOffset)

      // An independent offset corresponding to the TPE offset.
      offset = {
        val ox = (offsetPosX - basePosX) / _pixelsPerArcsec
        val oy = (offsetPosY - basePosY) / _pixelsPerArcsec
        new Offset(Angle.arcsecs(ox), Angle.arcsecs(oy))
      }

      // Get the arm adjustment and apply it to all the shapes making up the probe arm.
      adj <- probeArmGeometry.armAdjustment(ctx, offset)

      // The point to move the probe arm to the required position on the screen.
      screenPos = new Point2D.Double(basePosX + translateX, basePosY + translateY)

      // Iterate over the shapes.
      s   <- probeArmGeometry.geometry(ctx.getInstrument.asInstanceOf[I])
    } {
      val sCtx = FeatureGeometry.transformProbeArmForContext(s, adj)
      val sScr = FeatureGeometry.transformProbeArmForScreen(sCtx, _pixelsPerArcsec, _flipRA, screenPos)
      _figureList.add(new Figure(sScr, ProbeArmColor, Blocked, OIWFS_FeatureBase.OIWFS_STROKE))
    }
  }

  /**
   * Update the figure list with the figures comprising the guide probe.
   *
   * @param guidePosX    the X screen coordinate position for the OIWFS guide star
   * @param guidePosY    the Y screen coordinate position for the OIWFS guide star
   * @param offsetPosX   the X screen coordinate for the selected offset
   * @param offsetPosY   the X screen coordinate for the selected offset
   * @param translateX   translate resulting figure by this amount of pixels in X
   * @param translateY   translate resulting figure by this amount of pixels in Y
   * @param basePosX     the X screen coordinate for the base position
   * @param basePosY     the Y screen coordinate for the base position
   * @param oiwfsDefined set to true if an OIWFS position is defined (otherwise
   */
  override def _updateFigureList(guidePosX:  Double, guidePosY:  Double,
                                 offsetPosX: Double, offsetPosY: Double,
                                 translateX: Double, translateY: Double,
                                 basePosX:   Double, basePosY:   Double,
                                 oiwfsDefined: Boolean): Unit = {
    _figureList.clear()
    addOffsetConstrainedPatrolFieldAtBase(basePosX, basePosY)
    addPatrolFieldAtBase(offsetPosX + translateX, offsetPosY + translateY)
    if (oiwfsDefined)
      addProbeArm(offsetPosX, offsetPosY, translateX, translateY, basePosX, basePosY)
  }

  /**
   * Check to see if the offset intersection is empty, and if so, issue a warning.
   *
   * @return Some(singleton list with a warning) if the offset intersection is empty, and
   *         None otherwise.
   */
  private def getMessagesAsScala: Option[java.util.Collection[TpeMessage]] = {
    lazy val offsets = getContext.offsets.scienceOffsetsJava
    for {
      ctx         <- _iw.getMinimalObsContext.asScalaOpt
      patrolField <- probe.getCorrectedPatrolField(ctx).asScalaOpt
      if patrolField.outerLimitOffsetIntersection(offsets).isEmpty
    } yield List(NoValidRegionWarning).asJavaCollection
  }

  override def getMessages =
    getMessagesAsScala.asGeminiOpt

  /**
   * We always force an update due to buggy code or the effort not worth doing to maintain
   * old offset lists.
   */
  override protected def _needsUpdate(inst: SPInstObsComp, tii: TpeImageInfo): Boolean = true
}

case object OIWFSVignettingFeature {
  // Drawing specifications.
  lazy val ProbeArmColor        = Color.RED
  lazy val Blocked              = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5F)

  // A TPE warning if there is no valid region.
  lazy val NoValidRegionWarning = TpeMessage.warningMessage("No valid OIWFS region. Check offset positions.")
}



// Concrete instances.
object Flamingos2OIWFSFeature extends OIWFSVignettingFeature(Flamingos2OiwfsGuideProbe.instance, F2OiwfsProbeArm, "Flamingos2 OIWFS") {
  val f: DecimalFormat = new DecimalFormat("0.###")

  private def printCoords(name: String, x: Double, y: Double, reduce: Boolean) {
    System.out.print("* " + name + " = (" + f.format(x) + "," + f.format(y) + ")")
    if (reduce) System.out.print(" = (" + f.format(x / _pixelsPerArcsec) + "," + f.format(y / _pixelsPerArcsec) + ")")
    System.out.println
  }

  private def printCoords(name: String, P: Point2D, reduce: Boolean) {
    printCoords(name, P.getX, P.getY, reduce)
  }

  private def printValue(name: String, value: Double, reduce: Boolean) {
    System.out.print("* " + name + " = " + f.format(value))
    if (reduce) System.out.print(" = " + f.format(value / _pixelsPerArcsec))
    System.out.println
  }

  val MAXARCSECS: Double = 360 * 60 * 60

  private def normalize(value: Double): Double = {
    return if ((value > MAXARCSECS / 2.0)) value - MAXARCSECS else value
  }
  private def _addProbeArm2 (xc: Double, yc: Double, xg: Double, yg: Double, xt: Double, yt: Double) {
    val inst: Flamingos2 = _iw.getInstObsComp.asInstanceOf[Flamingos2]
    val plateScale = inst.getLyotWheel.getPlateScale
    val sign: Int = if (inst.getFlipConfig(false)) -1 else 1
    val f: DecimalFormat = new DecimalFormat("0.###")
    System.out.println
    System.out.println("sign=" + sign + ", flipRA=" + _flipRA)
    printCoords("C", xc, yc, true)
    printCoords("G", xg, yg, true)
    printCoords("T", xt, yt, true)
    val tp: Point2D.Double = new Point2D.Double(xg, yg)
    val tpOff: Point2D.Double = new Point2D.Double(xg + xt, yg + yt)

    val scale: Double = _pixelsPerArcsec * plateScale
    val length_base: Double = F2OiwfsProbeArm.ProbeBaseArmLength * scale
    val length_pickoff: Double = F2OiwfsProbeArm.ProbePickoffArmLength * scale
    val base_arm_axis: Double = F2OiwfsProbeArm.ProbeArmOffset * scale
    val pa: Double = -_posAngle - inst.getRotationConfig(false).toRadians.getMagnitude
    val x0: Double = xc + base_arm_axis * _flipRA * sign * Math.cos(pa)
    val y0: Double = yc + base_arm_axis * _flipRA * sign * Math.sin(pa)
    val x1: Double = xg + xt
    val y1: Double = yg + yt
    val distance: Double = Math.min(Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0)), length_base + length_pickoff)
    val a: Double = (length_base * length_base - length_pickoff * length_pickoff + distance * distance) / (2 * distance)
    val h: Double = sign * Math.sqrt(length_base * length_base - a * a)
    val x2: Double = x0 + a * (x1 - x0) / distance
    val y2: Double = y0 + a * (y1 - y0) / distance
    var y3: Double = y2 - h * (x1 - x0) / distance
    var x3: Double = x2 + h * (y1 - y0) / distance
    if (_flipRA < 0) {
      if ((x3 * xc * _flipRA + y3 * yc) < 0 || (x3 * xc + y3 * yc * _flipRA) < 0) {
        y3 = y2 + h * (x1 - x0) / distance
        x3 = x2 - h * (y1 - y0) / distance
      }
    }
    val angle: Double = Math.atan2(y3 - y1, x3 - x1)

    val ctx: ObsContext = _iw.getMinimalObsContext.getOrNull
    val cb: Coordinates = ctx.getBaseCoordinates
    val cbx: Double = normalize(cb.getRa.convertTo(Angle.Unit.ARCSECS).getMagnitude)
    val cby: Double = normalize(cb.getDec.convertTo(Angle.Unit.ARCSECS).getMagnitude)
    val ox: Double = (xc - _baseScreenPos.getX) / _pixelsPerArcsec
    val oy: Double = (yc - _baseScreenPos.getY) / _pixelsPerArcsec
    System.out.println
    val scaledPAO: Double = F2OiwfsProbeArm.ProbeArmOffset * plateScale
    val P: Point2D = new Point2D.Double(scaledPAO * sign * Math.cos(pa), scaledPAO * sign * Math.sin(pa))
    val gpt: GuideProbeTargets = ctx.getTargets.getPrimaryGuideProbeTargets(Flamingos2OiwfsGuideProbe.instance).getOrNull
    if (gpt != null) {
      val guideStar: SPTarget = gpt.getPrimary.getOrNull
      if (guideStar != null) {
        val gx: Double = normalize(guideStar.getTarget.getSkycalcCoordinates.getRa.toArcsecs.getMagnitude)
        val gy: Double = normalize(guideStar.getTarget.getSkycalcCoordinates.getDec.toArcsecs.getMagnitude)
        val GS: Point2D = new Point2D.Double(gx, gy)
        printCoords("GS", gx, gy, false)
        printCoords("gs", (xc - xg), (yc - yg), true)
        printCoords("MX", P, false)
        printCoords("mx", x0 - xc, y0 - yc, true)
        val T: Point2D = new Point2D.Double(xt * _flipRA / _pixelsPerArcsec, yt * _flipRA / _pixelsPerArcsec)
        val D: Point2D = new Point2D.Double(-GS.getX + T.getX - P.getX, -GS.getY + T.getY - P.getY)
        val scaledPBAL: Double = F2OiwfsProbeArm.ProbeBaseArmLength * plateScale
        val scaledPPAL: Double = F2OiwfsProbeArm.ProbePickoffArmLength * plateScale
        val mdistance: Double = Math.min(Math.sqrt(D.getX * D.getX + D.getY * D.getY), scaledPBAL + scaledPPAL)
        printValue("mdistance", mdistance, false)
        printValue(" distance", distance, true)
        val ma: Double = (scaledPBAL * scaledPBAL - scaledPPAL * scaledPPAL + mdistance * mdistance) / (2 * mdistance)
        printValue("ma", ma, false)
        printValue(" a", a, true)
        val mh: Double = sign * Math.sqrt(scaledPBAL * scaledPBAL - ma * ma)
        printValue("mh", mh, false)
        printValue(" h", h, true)
        val Q: Point2D = new Point2D.Double(GS.getX - T.getX + P.getX + (ma * D.getX + mh * D.getY) / mdistance, GS.getY - T.getY + P.getY + (ma * D.getY - mh * D.getX) / mdistance)
        printCoords("Q", Q, false)
        printCoords("q", (x3 - x1) * _flipRA, (y3 - y1) * _flipRA, true)
        val mangle: Double = Math.atan2(Q.getY, Q.getX)
        printValue("mangle", mangle, false)
        printValue(" angle", angle, false)
      }
    }
  }

  override def _updateFigureList(guidePosX:  Double, guidePosY:  Double,
                                 offsetPosX: Double, offsetPosY: Double,
                                 translateX: Double, translateY: Double,
                                 basePosX:   Double, basePosY:   Double,
                                 oiwfsDefined: Boolean): Unit = {
    _addProbeArm2(offsetPosX, offsetPosY, guidePosX, guidePosY, translateX, translateY)
    super._updateFigureList(guidePosX, guidePosY, offsetPosX, offsetPosY, translateX, translateY, basePosX, basePosY, oiwfsDefined)
  }
}

object GmosNorthOIWFSFeature  extends OIWFSVignettingFeature(GmosOiwfsGuideProbe.instance, GmosNorthOiwfsProbeArm, "GMOS OIWFS")
object GmosSouthOIWFSFeature  extends OIWFSVignettingFeature(GmosOiwfsGuideProbe.instance, GmosSouthOiwfsProbeArm, "GMOS OIWFS")