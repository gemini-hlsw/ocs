package jsky.app.ot.gemini.inst

import java.awt.geom.{Point2D}
import java.awt.{AlphaComposite, Color}

import edu.gemini.skycalc.{Angle, Offset}
import edu.gemini.spModel.gemini.flamingos2.{F2OiwfsProbeArm, Flamingos2OiwfsGuideProbe}
import edu.gemini.spModel.gemini.gmos.{GmosSouthOiwfsProbeArm, GmosNorthOiwfsProbeArm, GmosOiwfsGuideProbe}
import edu.gemini.spModel.guide.{OffsetValidatingGuideProbe, PatrolField}
import edu.gemini.spModel.inst.{FeatureGeometry, ProbeArmGeometry}
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.rich.shared.immutable._
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
object Flamingos2OIWFSFeature extends OIWFSVignettingFeature(Flamingos2OiwfsGuideProbe.instance, F2OiwfsProbeArm, "Flamingos2 OIWFS")
object GmosNorthOIWFSFeature  extends OIWFSVignettingFeature(GmosOiwfsGuideProbe.instance, GmosNorthOiwfsProbeArm, "GMOS OIWFS")
object GmosSouthOIWFSFeature  extends OIWFSVignettingFeature(GmosOiwfsGuideProbe.instance, GmosSouthOiwfsProbeArm, "GMOS OIWFS")