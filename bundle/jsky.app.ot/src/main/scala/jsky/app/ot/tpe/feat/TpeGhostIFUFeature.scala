package jsky.app.ot.tpe.feat

import java.awt.{AlphaComposite, Color, Component, Graphics, GridBagConstraints, GridBagLayout, Insets, Paint, TexturePaint, Transparency}
import java.awt.geom.{AffineTransform, Area, Path2D, Point2D, Rectangle2D}
import java.awt.image.BufferedImage
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.util.Collections

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.shared.util.immutable.{None => JNone, Option => JOption, Some => JSome}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.skycalc.Offset
import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.gemini.ghost.{Ghost, GhostAsterism, GhostScienceAreaGeometry}
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GhostTarget
import edu.gemini.spModel.telescope.IssPort
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.target.{SPSkyObject, WatchablePos}
import edu.gemini.spModel.target.env.{Asterism, AsterismType, TargetEnvironment, TargetEnvironmentDiff}
import javax.swing.{Icon, JLabel, JPanel, SwingConstants}
import jsky.app.ot.tpe._
import jsky.app.ot.util.{BasicPropertyList, OtColor, PropertyWatcher}

import scala.collection.JavaConverters._
import scala.swing.{Color, Graphics2D}
import scalaz._
import Scalaz._


/**
 * Draws the GHOST IFU patrol fields and IFUs.
 * GHOST only supports one offset in standard mode with a single target, but we implement the code anyway and
 * issue P2 errors in GhostRule if this is violated.
 */
final class TpeGhostIfuFeature extends TpeImageFeature("GHOST", "Show the patrol fields of the GHOST IFUs") with PropertyWatcher with TpeDragSensitive {
  // Transformations for the GHOST IFU patrol fields.
  private var ifuTransform = new AffineTransform()
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
        val ifu1PatrolField: Area = new Area(flipArea(TpeGhostIfuFeature.ifu1Arc(ctx))).createTransformedArea(ifuTransform)
        val ifu2PatrolField: Area = new Area(flipArea(TpeGhostIfuFeature.ifu2Arc(ctx))).createTransformedArea(ifuTransform)

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
            case a: GhostAsterism.StandardResolution =>
              val pos1 = a.srifu1 match {
                case Left(spCoordinates) => spCoordinates
                case Right(GhostTarget(spTarget, guideFiberState)) => spTarget
              }

              // Now draw the hexagon at the appropriate place.
              val psrIfu1: Point2D.Double = pm.getLocationFromTag(pos1)

              // Draw first IFU.
              drawIFU(g2d, psrIfu1)

              // Draw the sky fibers. These are barely visible.
              drawSRSkyFibers(g2d, psrIfu1)

              // If SRIFU2 is in use, draw it as well.
              a.srifu2.map {
                case Left(sPCoordinates) => sPCoordinates
                case Right(GhostTarget(spTarget, guideFiberState)) => spTarget
              }.map(_iw.taggedPosToScreenCoords).foreach(p => drawIFU(g2d, p))

            case a: GhostAsterism.HighResolution =>
              val (pos1, guideFiberState) = (a.hrifu1.spTarget, a.hrifu1.guideFiberState)

              // Now draw the hexagon at the appropriate place.
              val p: Point2D.Double = _iw.taggedPosToScreenCoords(pos1)
              drawIFU(g2d, p)
          }
        }

        // Restore the paint.
        g2d.setPaint(originalPaint)
      }

      // Reset the color.
      g2d.setColor(originalColor)
    }
  }

  /**
   * Draw an IFU.
   */
  private def drawIFU(g2d: Graphics2D, p: Point2D): Unit = {
    // Transform the hexagon at the appropriate place.
    val trans: AffineTransform = {
      val t = new AffineTransform
      t.translate(p.getX, p.getY)
      t.scale(TpeGhostIfuFeature.HexPlateScale.toArcsecs, TpeGhostIfuFeature.HexPlateScale.toArcsecs)
      t.concatenate(scaleTransform)
      t
    }

    g2d.setColor(TpeGhostIfuFeature.IfuColor)
    val hex: Area = TpeGhostIfuFeature.regularHexagon().createTransformedArea(trans)
    g2d.fill(hex)
  }

  /**
   * Draw the SR sky fibers relative to the center of SRIFU1. Thery are extremely small.
   */
  private def drawSRSkyFibers(g2d: Graphics2D, p: Point2D): Unit = {
    val trans: AffineTransform = {
      val t = new AffineTransform
      t.translate(p.getX, p.getY + 0.12)
      // No HexPlateScale here because everything given in arcsecs.
      t.concatenate(scaleTransform)
      t.translate(3.41 + 0.12, 0.2)
      t.scale(0.456, 0.456)
      t
    }
    g2d.setColor(TpeGhostIfuFeature.SkyFiberColor)
    val hex: Area = TpeGhostIfuFeature.regularHexagon().createTransformedArea(trans)
    g2d.fill(hex)
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
  override def handleDragStarted(dragObject: Any, context: ObsContext): Unit = dragObject match {
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
    case AsterismType.GhostSingleTarget | AsterismType.GhostHighResolutionTarget => false
    case AsterismType.Single => sys.error("Invalid asterism type for GHOST")
  }


  /**
   * Called when dragging is stopped in the TPE: display both IFUs.
   */
  override def handleDragStopped(context: ObsContext): Unit = {
      displayMode = TpeGhostIfuFeature.IFUDisplayModeBoth
  }


  override def isEnabled(ctx: TpeContext): Boolean = super.isEnabled(ctx) && ctx.instrument.is(SPComponentType.INSTRUMENT_GHOST)


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


  // Implements the TelescopePosWatcher interface.
  def telescopePosLocationUpdate(tp: WatchablePos): Unit =
    redraw()


  def telescopePosGenericUpdate(tp: WatchablePos): Unit =
    redraw()


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
   * Create a regular hexagon of side 1 at (0,0).
   */
  private[feat] def regularHexagon(): Area = {
    // The rotation to create the edges of the hexagon.
    val path = new Path2D.Double(Path2D.WIND_EVEN_ODD)
    val rot: AffineTransform = AffineTransform.getRotateInstance(Math.PI / 3.0)
    val x = 1
    val y = 0
    path.moveTo(x, y)
    path.lineTo(x, y)
    val p = new Point2D.Double(x, 0)
    for (i <- 0 until 6) {
      rot.transform(p, p)
      path.lineTo(p.getX, p.getY)
    }
    path.closePath()
    new Area(path)
  }

  /**
   * Calculates the current patrol field for an IFU as specified by its default patrol field.
   */
  private def ifuPatrolField(ctx: ObsContext, patrol: Rectangle2D.Double): Area = {
    val rot = if (ctx.getIssPort == IssPort.SIDE_LOOKING) Angle.fromDegrees(90) else Angle.fromDegrees(0)
    val t: Double = ctx.getPositionAngle.toRadians + rot.toRadians

    val rects = ctx.getSciencePositions.asScala.toSet.map { pos: Offset =>
      val fov = new Area(GhostScienceAreaGeometry.Ellipse)
      val offsetPatrol = new Area(patrol)

      val p = pos.p.toArcsecs.getMagnitude
      val q = pos.q.toArcsecs.getMagnitude
      val xform = new AffineTransform
      if (t != 0.0) xform.rotate(-t)
      xform.translate(-p, -q)

      fov.transform(xform)
      offsetPatrol.transform(xform)
      offsetPatrol.intersect(fov)
      offsetPatrol
    }

    rects.reduceOption { (a, b) =>
      val c = new Area(a)
      c.intersect(b)
      c
    }.getOrElse(new Area)
  }

  // The patrol fields for IFU1 and IFU2 relative to the base position.
  private val ifu1PatrolField = new Rectangle2D.Double(-222, -222, 222 + 3.28, 444)
  private val ifu2PatrolField = new Rectangle2D.Double(-3.28, -222, 222 + 3.28, 444)
  private def ifu1Arc(ctx: ObsContext): Area = ifuPatrolField(ctx, ifu1PatrolField)
  private def ifu2Arc(ctx: ObsContext): Area = ifuPatrolField(ctx, ifu2PatrolField)


  // Color for IFU limts.
  private val IfuFovColor: Color = Color.RED
  private val PatrolRangeColor: Color = OtColor.SALMON

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
    case a: GhostAsterism.HighResolution => skyObject == a.hrifu1.spTarget
    case _ => false
  }

  private[feat] def objectInIFU2(env: TargetEnvironment, skyObject: SPSkyObject): Boolean = env.getAsterism match {
    case a: GhostAsterism.StandardResolution => a.srifu2.exists(_.fold(skyObject == _, skyObject == _.spTarget))
    case a: GhostAsterism.HighResolution => a.hrifu2.contains(skyObject)
    case _ => false
  }

  val Warning: TpeMessage = TpeMessage.warningMessage("No valid region for IFU positions.")

  // 610 microns per arcsec. All dimensions in mm.
  // 0.240 face width per hexagon: three hexagons.
  val IfuColor: Color = OtColor.makeSlightlyDarker(Color.green)
  val HexPlateScale: Angle = Angle.fromArcsecs(0.720 / 0.61)

  // The colors of the sky fibers.
  val SkyFiberColor: Color = OtColor.makeSlightlyDarker(Color.cyan)
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
