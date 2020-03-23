package jsky.app.ot.tpe.feat

import java.awt.{AlphaComposite, Color, Component, Composite, Graphics, Graphics2D, GridBagConstraints, GridBagLayout, Insets, Paint, TexturePaint, Transparency}
import java.awt.geom.{AffineTransform, Area, Point2D, Rectangle2D}
import java.awt.image.BufferedImage
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.util.Collections

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.shared.util.immutable.{None => JNone, Option => JOption, Some => JSome}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.gemini.ghost.Ghost
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.target.WatchablePos
import edu.gemini.spModel.target.env.AsterismType._
import edu.gemini.spModel.target.env.TargetEnvironmentDiff
import javax.swing.{Icon, JLabel, JPanel, SwingConstants}
import jsky.app.ot.tpe._
import jsky.app.ot.util.{BasicPropertyList, OtColor, PropertyWatcher}

import scala.swing.{Color, Graphics2D}

/**
 * Draws the GHOST IFU patrol fiels and IFUS.
 */
final class TpeGhostIfuFeature extends TpeImageFeature("GHOST", "Show the patrol fields of the GHOST IFUs") with PropertyWatcher with TpeDragSensitive {
  private var transform: Option[AffineTransform] = None

  // Determine if there are no TPE messages.
  private var isEmpty: Boolean = false

  /**
   * If _flipRA is -1, flip the TA axis of the area.
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
    if (transform == null)
      return

    final case class IFURanges(srifu1range: Boolean, srifu2range: Boolean, hrifu1range: Boolean, hrifu2range: Boolean)
    for {
      ctx <- _iw.getObsContext.asScalaOpt
    } {
      // Store the old color.
      val g2d: Graphics2D = g.asInstanceOf[Graphics2D]
      val originalColor = g2d.getColor

      // The science FOV for GHOST is handled by GHOST itself, so we draw the IFU1 patrol fields depending
      // on what asterism types are selected.
      val ranges: IFURanges = ctx.getTargets.getAsterism.asterismType match {
        case Single => IFURanges(false, false, false, false)
        case GhostSingleTarget => IFURanges(true, false, false, false)
        case GhostDualTarget | GhostTargetPlusSky | GhostSkyPlusTarget => IFURanges(true, true, false, false)
        case GhostTargetPlusSky => IFURanges(true, false)
        case GhostSkyPlusTarget => IFURanges(true, true)
        case GhostHighResolutionTarget => IFURanges(true, false)
        case GhostHighResolutionTargetPlusSky => IFURanges(true, true)
        case _ => sys.error("Illegal asterism type")
      }

      // TODO: Draw the IFU1 patrol field and IFU2 patrol field ranges.

      // TODO: These should be open Arc2D.
      if (ranges.srifu1range || ranges.hrifu1range) g2d.draw(ifu1)
      if (ranges.srifu2range || ranges.hrifu2range) g2d.draw(ifu2)

      val paint: Paint = gr

      // Reset the color.
      g2d.setColor(originalColor)
    }
  }

  /**
   * Gets this feature's category, which is used for separating the categories
   * in the tool button display.
   */
  override def getCategory: TpeImageFeatureCategory = TpeImageFeatureCategory.fieldOfView

  /**
   * Called when an item is dragged in the TPE.
   *
   * @param dragObject object being dragged
   * @param context    observation context
   */
  override def handleDragStarted(dragObject: Any, context: ObsContext): Unit = ???

  /**
   * Called when the user stops dragging an item in them TPE.
   *
   * @param context observation context
   */
  override def handleDragStopped(context: ObsContext): Unit = ???

  override def isEnabled(ctx: TpeContext): Boolean = super.isEnabled(ctx) && ctx.instrument.is(SPComponentType.INSTRUMENT_GHOST)

  // A property has changed.
  override def propertyChange(propName: String): Unit = _iw.repaint()


  // The properties supported by this feature.
  override def getProperties: BasicPropertyList = TpeGhostIfuFeature.properties

  // TODO: Use set or _=
  // Turn on / off the drawing of the IFU patrol field.
  def drawPatrolFields_=(draw: Boolean): Unit =
    TpeGhostIfuFeature.properties.setBoolean(TpeGhostIfuFeature.PropShowRanges, draw)

  def drawPatrolFields_=(): Boolean =
    TpeGhostIfuFeature.properties.getBoolean(TpeGhostIfuFeature.PropShowRanges, true)

  /**
   * Reinitialize (recalculate the positions and redraw).
   */
  private val selListener: PropertyChangeListener = new PropertyChangeListener {
    override def propertyChange(evt: PropertyChangeEvent): Unit = _redraw()
  }

  // Get the drawing of probe ranges.
  def drawProbeRanges(): Boolean =
    TpeGhostIfuFeature.properties.getBoolean(TpeGhostIfuFeature.PropShowRanges, true)

  // Reinitialize (recalculate the positions and redraw).
  override def reinit(iw: TpeImageWidget, tii: TpeImageInfo): Unit = {
    _stopMonitorOffsetSelections(selListener)
    super.reinit(iw, tii)
    TpeGhostIfuFeature.properties.addWatcher(this)

    val inst: SPInstObsComp = _iw.getInstObsComp
    if (inst == null) return

    // TODO: We might want to remove this since we are not adding or removing telescope positions.
    // Arrange to be notified if telescope positions are added, removed, or selected.
    _monitorPosList()

    // TODO: Not sure what exactly this does.
    // Monitor the selections of offset positions, since that affects the positions drawn
    _monitorOffsetSelections(selListener)

    val base: Point2D.Double = tii.getBaseScreenPos
    val ppa: Double = tii.getPixelsPerArcsec

    transform = Some {
      val t = new AffineTransform
      t.translate(base.x, base.y)
      t.rotate(-tii.getTheta)
      t.scale(ppa, ppa)
      t
    }
  }


  override def unloaded(): Unit = {
    TpeGhostIfuFeature.properties.deleteWatcher(this)
    super.unloaded()
  }


  // TODO: What about telescope coords? Do we need to do something separate for this?
  // Implements the TelescopePosWatcher interface.
  def telescopePosLocationUpdate(tp: WatchablePos): Unit =
    _redraw()


  def telescopePosGenericUpdate(tp: WatchablePos): Unit =
    _redraw()


  override protected def handleTargetEnvironmentUpdate(diff: TargetEnvironmentDiff): Unit =
    _redraw()


  // Schedule a redraw of the image feature.
  private def _redraw(): Unit =
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
  // Color for IFU limts.
  private val IfuFovColor: Color = Color.RED
  private val PatrolRangeColor: Color = OtColor.SALMON

  // Alpha Composite used for drawing items that block the view.
  private val Blocked: Composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)


  // Property used to control drawing of the probes ranges.
  val properties: BasicPropertyList = new BasicPropertyList(Ghost.getClass.getName)
  val PropShowRanges: String = "Show GHOST IFUs and ranges"
  properties.registerBooleanProperty(PropShowRanges, true)


  /**
   * Determine the ranges of the IFU display modes: there are two, and they overlap.
   */
  // TODO: Do for we need SRIFU and HRIFU since they have different IFU?
  private sealed trait IFUDisplayMode {
    def show1: Boolean = false
    def show2: Boolean = false
  }
  private case object IFUDisplayMode1 extends IFUDisplayMode {
    override def show1 = true
  }
  private case object IFUDisplayMode2 extends IFUDisplayMode {
    override def show2 = true
  }

  // Creates the Paint that is used for filling the IFU1 and IFU2 patrol fields.
  private case class PaintParameters(skip: Int, alphaBg: Double, alphaLine: Double)

  private case object PatrolFieldPaintParameters extends PaintParameters(16, 0.16, 0.4)
  private def createPatrolFieldPaint(g2d: Graphics2D, orientation: Orientation): Paint =
    createPatrolFieldPaint(g2d, orientation: Orientation, PatrolFieldPaintParameters)

  private case object PatrolFieldKeyPaintParameters extends PaintParameters(skip = 8, alphaBg = 0.32, alphaLine = 0.8)
  private[feat] def createPatrolFieldKeyPaint(g2d: Graphics2D, orientation: Orientation): Paint =
    createPatrolFieldPaint(g2d, orientation, PatrolFieldKeyPaintParameters)

  private def createPatrolFieldPaint(g2d: Graphics2D, orientation: Orientation, paintParameters: PaintParameters): Paint = {
    val size = 2 * paintParameters.skip
    val rectangle: Rectangle2D = new Rectangle2D.Double(0, 0, size, size)

    // Get a buffered image capable of being transparent.
    val bufferedImage: BufferedImage = g2d.getDeviceConfiguration.createCompatibleImage(size, size, Transparency.TRANSLUCENT)
    val bufferedImageG2D: Graphics2D = bufferedImage.createGraphics()

    // TODO: Replace this with an Arc2D.Double
    // Shade it with a light red color that is almost completely transparent.
    bufferedImageG2D.setColor(OtColor.makeTransparent(PatrolRangeColor, paintParameters.alphaBg))
    bufferedImageG2D.setComposite(AlphaComposite.Src)
    bufferedImageG2D.fill(rectangle)

    // Draw the slanting lines, which are slightly less transparent than the background.
    bufferedImageG2D.setClip(0, 0, size, size)
    bufferedImageG2D.setColor(OtColor.makeTransparent(PatrolRangeColor, paintParameters.alphaLine))

    (0 until size by paintParameters.skip).foreach { v =>
      orientation match {
        case NorthEast =>
          bufferedImageG2D.drawLine(0, v, size, v)
        case SouthEast =>
          bufferedImageG2D.drawLine(v, 0, size, v)
      }
    }
    bufferedImageG2D.dispose()
    new TexturePaint(bufferedImage, rectangle)
  }

  // The slant of the lines drawn for the IFU1 and IFU2 patrol fields.
  sealed trait Orientation
  case object NorthEast extends Orientation
  case object SouthEast extends Orientation

  val Warning: TpeMessage = TpeMessage.warningMessage("No valid region for IFU positions.")
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
