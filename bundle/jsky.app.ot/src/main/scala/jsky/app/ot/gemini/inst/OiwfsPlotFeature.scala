package jsky.app.ot.gemini.inst

import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.AngleSyntax._
import edu.gemini.spModel.gemini.flamingos2.{F2OiwfsProbeArm, Flamingos2OiwfsGuideProbe}
import edu.gemini.spModel.gemini.gmos.{GmosSouthOiwfsProbeArm, GmosNorthOiwfsProbeArm, GmosOiwfsGuideProbe}
import edu.gemini.spModel.guide.{PatrolField, OffsetValidatingGuideProbe}
import edu.gemini.spModel.inst.{FeatureGeometry, ProbeArmGeometry}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable.{asScalaOpt, asGeminiOpt}
import edu.gemini.spModel.target.env.TargetEnvironmentDiff
import edu.gemini.shared.util.immutable.{Option => JOption}
import jsky.app.ot.tpe.TpeImageFeature.Figure

import jsky.app.ot.tpe._
import jsky.app.ot.util.PropertyWatcher

import java.awt.{List => _, _}
import java.awt.BasicStroke.{CAP_BUTT, JOIN_BEVEL}
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.util.logging.Logger

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

object OiwfsPlotFeature {
  private val Log = Logger.getLogger(getClass.getName)

  // Drawing specifications.
  val ProbeArmColor        = Color.red
  val ProbeArmStroke       = new BasicStroke(2F)
  val Blocked              = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5F)

  // A TPE warning if there is no valid region.
  val NoValidRegionWarning = TpeMessage.warningMessage("No valid OIWFS region. Check offset positions.")

  // The color to use to draw the patrol field
  val ReachableColor       = Color.green
  val PatrolFieldColor     = Color.red

  // Used to draw dashed lines
  val DashedLineStroke  = new BasicStroke(0.5F, CAP_BUTT,  JOIN_BEVEL, 0F, Array[Float](12F, 12F), 0F)
  val ThickDashedStroke = new BasicStroke(2.0F, CAP_BUTT,  JOIN_BEVEL, 0F, Array[Float](12F, 12F), 0F)

  def fillObscuredArea: Boolean =
    OIWFS_Feature.getProps.getBoolean(OIWFS_Feature.PROP_FILL_OBSCURED, true)
}

sealed class OiwfsPlotFeature(probe: OffsetValidatingGuideProbe, probeArm: ProbeArmGeometry)
  extends TpeImageFeature(probe.getKey, s"Show the ${probe.getKey} patrol field and arm.") with PropertyWatcher {

  OIWFS_Feature.getProps.addWatcher(this)

  import OiwfsPlotFeature._
  import FeatureGeometry._

  def getFigures(tpeCtx: TpeContext): List[Figure] = {
    def go(obsCtx: ObsContext, patField: PatrolField): List[Figure] = {
      // Extract info from the TPE context, converted to new model.
      val posAngle = Angle.fromDegrees(tpeCtx.instrument.posAngleOrZero)
      val selPos   = tpeCtx.offsets.selectedPos
      val offset   = selPos.map { opb =>
        Offset(opb.getXaxis.arcsecs[OffsetP], opb.getYaxis.arcsecs[OffsetQ])
      } | Offset.zero

      // Create the TPE Figures

      // OIWFS patrol field at the given offset position.
      val patrolFieldFigs = offsetTransform(posAngle, offset) |> { xform =>
        List(new Figure(patField.getArea, PatrolFieldColor, null, ThickDashedStroke).transform(xform))
      }

      // Intersection of patrol fields at all offset positions.  This is the
      // area in which a guide star may be selected.
      val reachableFigs = {
        val guidedOffsets = tpeCtx.offsets.scienceOffsetsJava
        val safe  = patField.safeOffsetIntersection(guidedOffsets)
        val area  = patField.offsetIntersection(guidedOffsets)
        val outer = patField.outerLimitOffsetIntersection(guidedOffsets)
        outer.subtract(safe)

        posAngleTransform(posAngle) |> { xform =>
          List(
            new Figure(area,  ReachableColor, null, DashedLineStroke),
            new Figure(outer, ReachableColor, AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25F), new BasicStroke(0F))
          ).map(_.transform(xform))
        }
      }

      // The OIWFS guide probe arm itself, if there is a selected guide star.
      val probeArmFig = {
        // this is weird behavior but matches 2015A production OT
        val showProbeArm = selPos.forall(_.isActive(probe)) &&
          probe.inRange(obsCtx, selPos.map(_.toSkycalcOffset) | edu.gemini.skycalc.Offset.ZERO_OFFSET)

        val guideStar = for {
          gpt <- tpeCtx.targets.envOrDefault.getPrimaryGuideProbeTargets(probe).asScalaOpt
          gs  <- gpt.getPrimary.asScalaOpt
        } yield gs.toNewModel.coordinates // TODO: when?

        if (showProbeArm)
          guideStar.flatMap { gs =>
            probeArm.geometry(obsCtx, gs, offset).map {
              new Figure(_, ProbeArmColor, Blocked, ProbeArmStroke)
            }
          }
        else None
      }

       reachableFigs ++ patrolFieldFigs ++ probeArmFig
    }

    (for {
      obsCtx   <- tpeCtx.obsContext
      patField <- probe.getCorrectedPatrolField(obsCtx).asScalaOpt
    } yield go(obsCtx, patField)).toList.flatten
  }


  // --------------------------------------------------------------
  // Everything that follows is junk required to plug into the TPE.
  // --------------------------------------------------------------

  override def draw(g: Graphics, tii: TpeImageInfo): Unit =
    g match {
      case g2d: Graphics2D =>
        reinit()
        drawFigures(g2d, fillObscuredArea)

      case _               =>
        OiwfsPlotFeature.Log.warning("draw expecting Graphics2D: " + g.getClass.getName)
    }

  val selectionWatcher = new PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit =
      _iw.repaint()
  }

  override def reinit(iw: TpeImageWidget, tii: TpeImageInfo): Unit = {
    _stopMonitorOffsetSelections(selectionWatcher)

    super.reinit(iw, tii)

    OIWFS_Feature.getProps.deleteWatcher(this)
    OIWFS_Feature.getProps.addWatcher(this)

    _monitorPosList()
    _monitorOffsetSelections(selectionWatcher)

    _figureList.clear()
    val toScreen = tii.toScreen
    getFigures(iw.getContext).foreach { fig => addFigure(fig.transform(toScreen)) }
  }

  override def getCategory: TpeImageFeatureCategory =
    TpeImageFeatureCategory.fieldOfView

  override def handleTargetEnvironmentUpdate(diff: TargetEnvironmentDiff): Unit =
    _iw.repaint()

  override def isEnabled(ctx: TpeContext): Boolean =
    super.isEnabled(ctx) && ctx.targets.envOrDefault.isActive(probe)

  override def unloaded(): Unit = {
    OIWFS_Feature.getProps.deleteWatcher(this)
    super.unloaded()
  }

  override def propertyChange(propName: String): Unit =
    _iw.repaint()

  override def getMessages: JOption[java.util.Collection[TpeMessage]] = {
    lazy val offsets = getContext.offsets.scienceOffsetsJava
    (for {
      ctx         <- _iw.getMinimalObsContext.asScalaOpt
      patrolField <- probe.getCorrectedPatrolField(ctx).asScalaOpt
      if patrolField.outerLimitOffsetIntersection(offsets).isEmpty
    } yield List(NoValidRegionWarning).asJavaCollection).asGeminiOpt
  }
}


object Flamingos2OiwfsFeature extends OiwfsPlotFeature(Flamingos2OiwfsGuideProbe.instance, F2OiwfsProbeArm) {
  val instance = this
}

object GmosNorthOiwfsFeature  extends OiwfsPlotFeature(GmosOiwfsGuideProbe.instance, GmosNorthOiwfsProbeArm) {
  val instance = this
}

object GmosSouthOiwfsFeature  extends OiwfsPlotFeature(GmosOiwfsGuideProbe.instance, GmosSouthOiwfsProbeArm) {
  val instance = this
}