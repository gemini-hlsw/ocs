package jsky.app.ot.tpe.feat

import java.awt.{AlphaComposite, Color, Component, Graphics, GridBagConstraints, GridBagLayout, Insets, Paint, TexturePaint, Transparency}
import java.awt.geom.{AffineTransform, Area, Path2D, Point2D, Rectangle2D}
import java.awt.image.BufferedImage
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.util.Collections
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.shared.util.immutable.{None => JNone, Option => JOption, Some => JSome}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.gemini.ghost.GhostIfuPatrolField.{ScaleMmToArcsec, TransformMmToArcsec}
import edu.gemini.spModel.gemini.ghost.{Ghost, GhostAsterism, GhostIfuPatrolField}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.target.SPSkyObject
import edu.gemini.spModel.target.env.{Asterism, AsterismType, TargetEnvironment, TargetEnvironmentDiff}

import javax.swing.{Icon, JLabel, JPanel, SwingConstants}
import jsky.app.ot.tpe._
import jsky.app.ot.tpe.feat.TpeGhostIfuFeature.{HrIfuTargetColor, SrIfuSkyColor, SrIfuTargetColor, highResolutionIfuArcsec, highResolutionSkyArcsec, standardResolutionIfuArcsec, standardResolutionSkyArcsec}
import jsky.app.ot.util.{BasicPropertyList, OtColor, PropertyWatcher}

import scala.annotation.tailrec
import scala.swing.Graphics2D


/**
 * Draws the GHOST IFU patrol fields and IFUs.
 * GHOST only supports one offset in standard mode with a single target, but we implement the code anyway and
 * issue P2 errors in GhostRule if this is violated.
 */
final class TpeGhostIfuFeature extends TpeImageFeature("GHOST", "Show the patrol fields of the GHOST IFUs") with PropertyWatcher with TpeDragSensitive {
  // Transformations for the GHOST IFU patrol fields.
  private var ifuTransform   = new AffineTransform()
  private var scaleTransform = new AffineTransform()

  // Determine if there are no TPE messages.
  private var isEmpty: Boolean = false

  /**
   * Determine which IFUs are active, e.g. when targets / sky positions are being dragged.
   * This should be set by the handle methods. This does not determine if both the IFU patrol fields are drawn
   * in and of itself alone, as we only draw the IFU2 elements if it is in use.
   */
  private var displayMode: TpeGhostIfuFeature.IFUDisplayMode = TpeGhostIfuFeature.IFUDisplayModeBoth

  /**
   * If _flipRA is -1, flip the RA axis of the area.
   */
  private def flipArea(a: Area): Area =
    if (_flipRA == -1) a.createTransformedArea(AffineTransform.getScaleInstance(_flipRA, 1.0))
    else a

  /**
   * Draw the feature: two overlapping symmetric arcs.
   */
  override def draw(g: Graphics, tii: TpeImageInfo): Unit = {
    if (!isEnabled(_iw.getContext))
      return

    _iw.getObsContext.asScalaOpt.foreach { ctx =>
      val g2d: Graphics2D = g.asInstanceOf[Graphics2D]

      // Store the old color and paint to restore.
      val originalColor: Color = g2d.getColor
      val originalPaint: Paint = g2d.getPaint

      val env: TargetEnvironment = ctx.getTargets
      if (env == null)
        return

      if (drawPatrolFields) {
        val ifu1PatrolField: Area = new Area(flipArea(GhostIfuPatrolField.ifu1(ctx).area)).createTransformedArea(ifuTransform)
        val ifu2PatrolField: Area = new Area(flipArea(GhostIfuPatrolField.ifu2(ctx).area)).createTransformedArea(ifuTransform)

        g2d.setColor(TpeGhostIfuFeature.PatrolFieldBorderColor)

        if (displayMode.show1) {
          g2d.draw(ifu1PatrolField)
          g2d.setPaint(TpeGhostIfuFeature.createPatrolFieldPaint(g2d, TpeGhostIfuFeature.NorthEast))
          g2d.fill(ifu1PatrolField)
        }

        // We only show the IFU2 elements if the asterism type applies to them.
        if (displayMode.show2 && usingIFU2(env)) {
          g2d.draw(ifu2PatrolField)
          g2d.setPaint(TpeGhostIfuFeature.createPatrolFieldPaint(g2d, TpeGhostIfuFeature.SouthEast))
          g2d.fill(ifu2PatrolField)
        }

        // Draw the IFUs hexagons at the SRIFU positions if in standard mode, HRIFU position if in high res mode.
        val pm: TpePositionMap = TpePositionMap.getMap(_iw)
        val asterism: Asterism = env.getAsterism

        if (displayMode.show1) {
          asterism match {
            case GhostAsterism.SingleTarget(t, _) =>
              // IFU1
              val p = pm.getLocationFromTag(t.spTarget)
              drawStandardResolutionIfu(g2d, p, Ifu1, AsTarget)
              drawStandardResolutionSky(g2d, p)

            case GhostAsterism.DualTarget(t1, t2, _) =>
              // IFU1
              val p1 = pm.getLocationFromTag(t1.spTarget)
              drawStandardResolutionIfu(g2d, p1, Ifu1, AsTarget)
              drawStandardResolutionSky(g2d, p1)

              // IFU2
              val p2 = pm.getLocationFromTag(t2.spTarget)
              drawStandardResolutionIfu(g2d, p2, Ifu2, AsTarget)

            case GhostAsterism.TargetPlusSky(t, s, _) =>
              // IFU1
              val p1 = pm.getLocationFromTag(t.spTarget)
              drawStandardResolutionIfu(g2d, p1, Ifu1, AsTarget)
              drawStandardResolutionSky(g2d, p1)

              // IFU2
              val p2 = pm.getLocationFromTag(s)
              drawStandardResolutionIfu(g2d, p2, Ifu2, AsSky)

            case GhostAsterism.SkyPlusTarget(s, t, _) =>
              // IFU1
              val p1 = pm.getLocationFromTag(s)
              drawStandardResolutionIfu(g2d, p1, Ifu1, AsSky)
              drawStandardResolutionSky(g2d, p1)

              // IFU2
              val p2 = pm.getLocationFromTag(t.spTarget)
              drawStandardResolutionIfu(g2d, p2, Ifu2, AsTarget)

            case GhostAsterism.HighResolutionTargetPlusSky(t, s, _) =>
              // IFU1
              val p1 = pm.getLocationFromTag(t.spTarget)
              drawHighResolutionIfu(g2d, p1)

              // IFU2
              val p2 = pm.getLocationFromTag(s)
              // We draw the SRIFU2 as Sky
              drawStandardResolutionIfu(g2d, p2, Ifu2, AsSky)
              //drawHighResolutionSky(g2d, p2)
          }
        }

        // Restore the paint.
        g2d.setPaint(originalPaint)
      }

      // Reset the color.
      g2d.setColor(originalColor)
    }
  }

  private sealed trait IfuNumber extends Product with Serializable {
    def toInt: Int =
      this match {
        case Ifu1 => 1
        case Ifu2 => 2
      }
  }
  private case object Ifu1 extends IfuNumber
  private case object Ifu2 extends IfuNumber

  private sealed trait IfuDisposition extends Product with Serializable
  private case object AsTarget extends IfuDisposition
  private case object AsSky    extends IfuDisposition

  private def drawHighResolutionIfu(g2d: Graphics2D, p: Point2D): Unit =
    drawIfu(g2d, p, "HRIFU", highResolutionIfuArcsec, HrIfuTargetColor)

  private def drawStandardResolutionIfu(g2d: Graphics2D, p: Point2D, i: IfuNumber, d: IfuDisposition): Unit =
    drawIfu(
      g2d,
      p,
      if (d == AsTarget) s"SRIFU${i.toInt}" else "Sky",
      standardResolutionIfuArcsec,
      if (d == AsTarget) SrIfuTargetColor else SrIfuSkyColor
    )

  private def drawIfu(g2d: Graphics2D, p: Point2D, label: String, a: Area, c: Color): Unit = {

    // Transform the hexagon at the appropriate place.
    val trans: AffineTransform = {
      val t = AffineTransform.getTranslateInstance(p.getX, p.getY)
      t.concatenate(scaleTransform)
      t
    }

    val aʹ = a.createTransformedArea(trans)
    g2d.setColor(c)
    g2d.fill(aʹ)

    val ifuBounds = aʹ.getBounds2D
    val origFont  = g2d.getFont

    def drawLabel(): Unit = {
      val fontMet = g2d.getFontMetrics
      val strBounds = fontMet.getStringBounds(label, g2d)
      val strWidth = strBounds.getWidth
      val strHeight = strBounds.getHeight
      g2d.setColor(Color.yellow)
      val x = p.getX + math.max(ifuBounds.getWidth - strWidth / 2, TpeImageFeature.MARKER_SIZE + 2)
      val y = p.getY + math.min(strHeight, math.max(ifuBounds.getHeight - strHeight, TpeImageFeature.MARKER_SIZE * 2))
      g2d.drawString(label, x.toFloat, y.toFloat)
    }

    // Use same font as other targets
    g2d.setFont(TpeImageFeature.FONT)
    drawLabel()
    g2d.setFont(origFont)
  }

  /**
   * Draw the SR sky fibers relative to the center of SRIFU1. They are extremely small.
   */
  private def drawStandardResolutionSky(g2d: Graphics2D, p: Point2D): Unit = {

    // size (in mm) of one side of one individual SR sky fiber
    val fiberSide = 0.24 / Math.sqrt(3.0)

    // See diagram in https://docs.google.com/document/d/1aN2ZPgaRMD52Rf4YG7ahwLnvQ8bpTRudc7MTBn-Lt2Y
    // The sky fibers are slightly offset.

    drawSky(
      g2d,
      p,
      standardResolutionSkyArcsec,
      1.0392 + 1.0392 + fiberSide,
      fiberSide
    )

  }

  private def drawHighResolutionSky(g2d: Graphics2D, p: Point2D): Unit = {
    // fiber centered on the sky position?
    drawSky(
      g2d,
      p,
      highResolutionSkyArcsec,
      0.0,
      // IFU2 is positioned 2 mm below sky position (the demand coordinates sent
      // are corrected by 2 mm) but we display the actual coordinates.
      0.0
    )
  }

  private def drawSky(g2d: Graphics2D, p: Point2D, a: Area, xOffMm: Double, yOffMm: Double): Unit = {
    val xOff = xOffMm * ScaleMmToArcsec
    val yOff = yOffMm * ScaleMmToArcsec
    val θ    = _tii.getCorrectedPosAngleRadians

    val t = new AffineTransform()
    t.translate(p.getX, p.getY)
    t.rotate(-θ)
    t.translate(
      xOff * _tii.getPixelsPerArcsec,
      yOff * _tii.getPixelsPerArcsec
    )
    t.concatenate(scaleTransform)

    g2d.setColor(TpeGhostIfuFeature.SkyFiberColor)
    g2d.fill(a.createTransformedArea(t))
  }

  /**
   * Gets this feature's category, which is used for separating the categories
   * in the tool button display.
   */
  override def getCategory: TpeImageFeatureCategory = TpeImageFeatureCategory.fieldOfView


  /**
   * Called when an item is dragged in the TPE: select the IFU to display.
   * We should only be able to drag sky objects (targets and coordinates).
   */
  override def handleDragStarted(dragObject: Any, context: ObsContext): Unit =
    dragObject match {
      case o: SPSkyObject =>
        val env: TargetEnvironment = context.getTargets
        if (env != null) {
          if (TpeGhostIfuFeature.objectInIFU1(env, o)) {
            displayMode = TpeGhostIfuFeature.IFUDisplayMode1
          } else if (TpeGhostIfuFeature.objectInIFU2(env, o)) {
            displayMode = TpeGhostIfuFeature.IFUDisplayMode2
          } else {
            displayMode = TpeGhostIfuFeature.IFUDisplayModeBoth
          }
        } else
          displayMode = TpeGhostIfuFeature.IFUDisplayModeBoth
      case _ =>
    }

  /**
   * Determine if IFU2 is in use.
   */
  private def usingIFU2(env: TargetEnvironment): Boolean = env.getAsterism.asterismType match {
    case AsterismType.GhostDualTarget | AsterismType.GhostTargetPlusSky | AsterismType.GhostSkyPlusTarget | AsterismType.GhostHighResolutionTargetPlusSky => true
    case AsterismType.GhostSingleTarget => false
    case AsterismType.Single => sys.error("Invalid asterism type for GHOST")
  }


  /**
   * Called when dragging is stopped in the TPE: display both IFUs.
   */
  override def handleDragStopped(context: ObsContext): Unit = {
      displayMode = TpeGhostIfuFeature.IFUDisplayModeBoth
  }


  override def isEnabled(ctx: TpeContext): Boolean = super.isEnabled(ctx) && ctx.instrument.is(SPComponentType.INSTRUMENT_GHOST)

  override def isEnabledByDefault: Boolean = true

  // A property has changed.
  override def propertyChange(propName: String): Unit = _iw.repaint()


  // The properties supported by this feature.
  override def getProperties: BasicPropertyList = TpeGhostIfuFeature.properties


  /**
   * Turn on / off the drawing of the IFU patrol field.
   */
  def drawPatrolFields_=(draw: Boolean): Unit =
    TpeGhostIfuFeature.properties.setBoolean(TpeGhostIfuFeature.PropShowRanges, draw)

  def drawPatrolFields: Boolean =
    TpeGhostIfuFeature.properties.getBoolean(TpeGhostIfuFeature.PropShowRanges, true)


  /**
   * Reinitialize (recalculate the positions and redraw).
   */
  private val selListener: PropertyChangeListener = new PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit = redraw()
  }


  /**
   * Determine if we should be drawing the IFU patrol fields.
   */
  def drawProbeRanges(): Boolean =
    TpeGhostIfuFeature.properties.getBoolean(TpeGhostIfuFeature.PropShowRanges, true)


  /**
   * Reinitialize (recalculate the positions and redraw).
   */
  override def reinit(iw: TpeImageWidget, tii: TpeImageInfo): Unit = {
    _stopMonitorOffsetSelections(selListener)
    super.reinit(iw, tii)
    TpeGhostIfuFeature.properties.addWatcher(this)

    val inst: SPInstObsComp = _iw.getInstObsComp
    if (inst == null) return

    // Arrange to be notified if telescope positions are added, removed, or selected.
    _monitorPosList()

    // Monitor the selections of offset positions, since that affects the positions drawn
    _monitorOffsetSelections(selListener)

    val base: Point2D.Double = tii.getBaseScreenPos
    val ppa: Double = tii.getPixelsPerArcsec

    ifuTransform = {
      val t = new AffineTransform
      t.translate(base.x, base.y)
      t.rotate(-tii.getTheta)
      t.scale(ppa, ppa)
      t
    }

    scaleTransform = AffineTransform.getScaleInstance(ppa, ppa)
  }


  override def unloaded(): Unit = {
    TpeGhostIfuFeature.properties.deleteWatcher(this)
    super.unloaded()
  }

  override protected def handleTargetEnvironmentUpdate(diff: TargetEnvironmentDiff): Unit =
    redraw()

  // Schedule a redraw of the image feature.
  private def redraw(): Unit =
    if (_iw != null) _iw.repaint()


  override def getKey: JOption[Component] = {
    val pan = new JPanel(new GridBagLayout)

    val ifu1Label: JLabel = new JLabel("IFU1", new ProbeRangeIcon(Array(TpeGhostIfuFeature.NorthEast)), SwingConstants.LEFT)
    pan.setForeground(Color.black)
    val gbc1: GridBagConstraints = new GridBagConstraints(0, 0, 1, 1, 0,
      0, java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.HORIZONTAL,
      new Insets(0, 0, 0, 0), 0, 0)


    val ifu2Label: JLabel = new JLabel("IFU2", new ProbeRangeIcon(Array(TpeGhostIfuFeature.SouthEast)), SwingConstants.LEFT)
    ifu2Label.setForeground(Color.black)
    val gbc2: GridBagConstraints = new GridBagConstraints(1, 0, 1, 1, 0,
      0, java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.HORIZONTAL,
      new Insets(0, 0, 0, 0), 0, 0)

    val bothLabel: JLabel = new JLabel("Both", new ProbeRangeIcon(Array(TpeGhostIfuFeature.NorthEast, TpeGhostIfuFeature.SouthEast)), SwingConstants.LEFT)
    bothLabel.setForeground(Color.black)
    val gbcBoth: GridBagConstraints = new GridBagConstraints(2, 0, 1, 1, 0,
      0, java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.HORIZONTAL,
      new Insets(0, 0, 0, 0), 0, 0)


    pan.add(ifu1Label, gbc1)
    pan.add(ifu2Label, gbc2)
    pan.add(bothLabel, gbcBoth)

    new JSome[Component](pan)
  }

  override def getMessages: JOption[java.util.Collection[TpeMessage]] = {
    if (!isEmpty) JNone.instance()
    else new JSome(Collections.singletonList(TpeGhostIfuFeature.Warning))
  }
}


object TpeGhostIfuFeature {

  /**
   * Create a regular hexagon representing an array of hexagonal fibers, count
   * fibers across, each with the given `flatToFlat` size.
   */
  private[feat] def fiberArray(
    flatToFlat: Double,
    count: Int
  ): Area = {

    // The rotation to create the edges of the hexagon.
    val path = new Path2D.Double(Path2D.WIND_EVEN_ODD)
    val rot: AffineTransform = AffineTransform.getRotateInstance(Math.PI / 3.0)

    val side = (flatToFlat * count) / Math.sqrt(3.0)
    path.moveTo(side, 0)
    path.lineTo(side, 0)
    val p = new Point2D.Double(side, 0)
    (0 until 6).foreach { _ =>
      rot.transform(p, p)
      path.lineTo(p.getX, p.getY)
    }
    path.closePath()
    new Area(path)
  }

  //
  // See https://docs.google.com/document/d/1aN2ZPgaRMD52Rf4YG7ahwLnvQ8bpTRudc7MTBn-Lt2Y/edit#
  //

  // Actually SR and HR are the same size in this representation but it seems
  // useful to keep them separate and match the spec.
  private def standardResolutionIfuMm: Area = fiberArray(0.240, 3)
  private def highResolutionIfuMm: Area     = fiberArray(0.144, 5)

  private def standardResolutionSkyMm: Area = fiberArray(0.240, 2)
  private def highResolutionSkyMm: Area     = fiberArray(0.144, 3)

  def standardResolutionIfuArcsec: Area =
    standardResolutionIfuMm.createTransformedArea(TransformMmToArcsec)

  def highResolutionIfuArcsec: Area =
    highResolutionIfuMm.createTransformedArea(TransformMmToArcsec)

  def standardResolutionSkyArcsec: Area =
    standardResolutionSkyMm.createTransformedArea(TransformMmToArcsec)

  def highResolutionSkyArcsec: Area =
    highResolutionSkyMm.createTransformedArea(TransformMmToArcsec)


  // Color for IFU limts.
  val IfuFovColor: Color = Color.RED
  val PatrolRangeColor: Color = OtColor.SALMON

  // The color to draw the patrol fields.
  private[feat] val PatrolFieldBorderColor: Color = OtColor.makeTransparent(IfuFovColor, 0.3)

  // Property used to control drawing of the probes ranges.
  val properties: BasicPropertyList = new BasicPropertyList(Ghost.getClass.getName)
  val PropShowRanges: String = "Show GHOST IFUs and ranges"
  properties.registerBooleanProperty(PropShowRanges, true)


  /**
   * Determine which IFU display modes are to be drawn at a given time.
   * Note: They overlap slightly.
   */
  private[feat] sealed trait IFUDisplayMode {
    def show1: Boolean = false
    def show2: Boolean = false
  }
  private[feat] object IFUDisplayMode1 extends IFUDisplayMode {
    override def show1 = true
  }
  private[feat] object IFUDisplayMode2 extends IFUDisplayMode {
    override def show2 = true
  }
  private[feat] object IFUDisplayModeBoth extends IFUDisplayMode {
    override def show1: Boolean = true
    override def show2: Boolean = true
  }

  // Creates the Paint that is used for filling the IFU1 and IFU2 patrol fields.
  private case class PaintParameters(skip: Int, alphaBg: Double, alphaLine: Double)

  private object PatrolFieldPaintParameters extends PaintParameters(16, 0.16, 0.4)
  private def createPatrolFieldPaint(g2d: Graphics2D, orientation: Orientation): Paint =
    createPatrolFieldPaint(g2d, orientation, PatrolFieldPaintParameters)

  private def createPatrolFieldPaint(g2d: Graphics2D, orientation: Orientation, paintParameters: PaintParameters): Paint = {
    val size = 2 * paintParameters.skip
    val rectangle: Rectangle2D = new Rectangle2D.Double(0, 0, size, size)

    // Get a buffered image capable of being transparent.
    val bufferedImage: BufferedImage = g2d.getDeviceConfiguration.createCompatibleImage(size, size, Transparency.TRANSLUCENT)
    val bufferedImageG2D: Graphics2D = bufferedImage.createGraphics()

    bufferedImageG2D.setColor(OtColor.makeTransparent(PatrolRangeColor, paintParameters.alphaBg))
    bufferedImageG2D.setComposite(AlphaComposite.Src)
    bufferedImageG2D.fill(rectangle)

    // Draw the slanting lines, which are slightly less transparent than the background.
    bufferedImageG2D.setClip(0, 0, size, size)
    bufferedImageG2D.setColor(OtColor.makeTransparent(PatrolRangeColor, paintParameters.alphaLine))

    (0 until size by paintParameters.skip).foreach { v =>
      orientation match {
        case NorthEast =>
          bufferedImageG2D.drawLine(0, v, size - v, size)
          bufferedImageG2D.drawLine(v, 0, size, size - v)
        case SouthEast =>
          bufferedImageG2D.drawLine(v, size, size, v)
          bufferedImageG2D.drawLine(0, v, v, 0)
      }
    }
    bufferedImageG2D.dispose()
    new TexturePaint(bufferedImage, rectangle)
  }

  private object PatrolFieldKeyPaintParameters extends PaintParameters(skip = 8, alphaBg = 0.32, alphaLine = 0.8)
  private[feat] def createPatrolFieldKeyPaint(g2d: Graphics2D, orientation: Orientation): Paint =
    createPatrolFieldPaint(g2d, orientation, PatrolFieldKeyPaintParameters)

  // The slant of the lines drawn for the IFU1 and IFU2 patrol fields.
  sealed trait Orientation
  case object NorthEast extends Orientation
  case object SouthEast extends Orientation

  // Determine which IFU a sky object belongs to.
  private[feat] def objectInIFU1(env: TargetEnvironment, skyObject: SPSkyObject): Boolean = env.getAsterism match {
    case a: GhostAsterism.StandardResolution => a.srifu1.fold(skyObject == _, skyObject == _.spTarget)
    case a: GhostAsterism.HighResolution     => skyObject == a.hrifu.spTarget
    case _ => false
  }

  private[feat] def objectInIFU2(env: TargetEnvironment, skyObject: SPSkyObject): Boolean = env.getAsterism match {
    case a: GhostAsterism.StandardResolution => a.srifu2.exists(_.fold(skyObject == _, skyObject == _.spTarget))
    case a: GhostAsterism.HighResolution     => a.hrsky == skyObject
    case _ => false
  }

  val Warning: TpeMessage = TpeMessage.warningMessage("No valid region for IFU positions.")

  // The colors of the sky fibers.
  val SkyFiberColor: Color = OtColor.makeSlightlyDarker(Color.cyan)

  // 610 microns per arcsec. All dimensions in mm.
  // 0.240 face width per hexagon: three hexagons.
  val HrIfuTargetColor: Color = new Color(135, 255, 135)
  val SrIfuTargetColor: Color = HrIfuTargetColor.darker
  val SrIfuSkyColor:    Color = SkyFiberColor.darker

}

private[feat] class ProbeRangeIcon(val slants: Array[TpeGhostIfuFeature.Orientation]) extends Icon {
  override val getIconWidth = 18
  override val getIconHeight = 18

  override def paintIcon(c: Component, g: Graphics, x: Int, y: Int): Unit = {
    val g2d = g.asInstanceOf[Graphics2D]
    g2d.setColor(Color.black)
    g2d.fill(new Rectangle2D.Double(1, 1, 16, 16))
    val origPaint = g2d.getPaint
    for (slant <- slants) {
      val p = TpeGhostIfuFeature.createPatrolFieldKeyPaint(g2d, slant)
      g2d.setPaint(p)
      g2d.fill(new Rectangle2D.Double(1, 1, 16, 16))
    }
    g2d.setPaint(origPaint)
  }
}
