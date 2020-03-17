package jsky.app.ot.tpe.feat

import java.awt._
import java.awt.geom.{AffineTransform, Rectangle2D}
import java.awt.image.BufferedImage

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.gemini.ghost.Ghost
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.env.AsterismType._

import jsky.app.ot.tpe._
import jsky.app.ot.util.{BasicPropertyList, OtColor, PropertyWatcher}

import scala.swing.{Color, Graphics2D}

final class TpeGhostIFUFeature extends TpeImageFeature("GHOST", "Show the patrol fields of the GHOST IFUs") with PropertyWatcher with TpeDragSensitive {
  private var transformOpt: Option[AffineTransform] = None

  /**
   * Draw the feature: two overlapping symmetric arcs.
   */
  override def draw(g: Graphics, tii: TpeImageInfo): Unit = {
    if (!isEnabled(_iw.getContext))
      return

    final class IFURanges(val range1: Boolean, val range2: Boolean)
    for {
      ctx <- _iw.getObsContext.asScalaOpt
    } {
      // Store the old color.
      val g2d: Graphics2D = g.asInstanceOf[Graphics2D]
      val originalColor = g2d.getColor()

      // The science FOv for GHOST is handled by GHOST itself, so we draw the IFU1 ranges and IFU2 ranges depending
      // on what asterism types are selected.
      val ranges: IFURanges = ctx.getTargets.getAsterism.asterismType match {
        case Single => IFURanges(false, false)
        case GhostSingleTarget => IFURanges(true, false)
        case GhostDualTarget => IFURanges(true, true)
        case GhostTargetPlusSky => IFURanges(true, false)
        case GhostSkyPlusTarget => IFURanges(true, true)
        case GhostHighResolutionTarget => IFURanges(true, false)
        case GhostHighResolutionTargetPlusSky => IFURanges(true, true)
        case _ => sys.error("Illegal asterism type")
      }


      // Reset the color.
      g2d.setColor(originalColor)
    }
  }

  /**
   * Gets this feature's category, which is used for separating the categories
   * in the tool button display.
   */
  override def getCategory: TpeImageFeatureCategory
    = TpeImageFeatureCategory.fieldOfView

  /**
   * Called when an item is dragged in the TPE.
   *
   * @param dragObject object being dragged
   * @param context    observation context
   */
  override def handleDragStarted(dragObject: Any, context: ObsContext): Unit
    = ???

  /**
   * Called when the user stops dragging an item in them TPE.
   *
   * @param context observation context
   */
  override def handleDragStopped(context: ObsContext): Unit
    = ???

    override def isEnabled(ctx: TpeContext): Boolean
      = super.isEnabled(ctx) && ctx.instrument.is(SPComponentType.INSTRUMENT_GHOST)

    override def propertyChange(propName: String): Unit
      = _iw.repaint()

    override def getProperties: BasicPropertyList
      = TpeGhostIFUFeature.properties
  }

  object TpeGhostIFUFeature {
    // The magic numbers for the robe prange and the robe prange key paint.
    type Skip = Int
    type AlphaBg = Double
    properties.registerBooleanProperty(PropShowRanges, true)
    type AlphaLine = Double
    type ProbeKeyPaint = (Int, Double, Double)
    private lazy val probeRangePaint: ProbeKeyPaint = (16, 0.16, 0.4)
    private lazy val probeRangeKeyPaint: ProbeKeyPaint = (8, 0.32, 0.8)
    private val PropShowRanges: String = "Show IFUs and ranges"
    private val properties: BasicPropertyList = new BasicPropertyList(Ghost.getClass.getName)
    // The values used to render the probe ranges
    private val ProbeRangeColor: Color = OtColor.SALMON
    private val Blocked: Composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)

    // Creates the Paint that is used for filling the IFU1 and IFU2 patrol fields.
    private def probeRagePaint(g2d: Graphics2D, orientation: Orientation): Paint =
      createPatrolFieldPaint(g2d, orientation: Orientation, probeRangePaint)

    private def createPatrolFieldPaint(g2d: Graphics2D, orientation: Orientation, probeRangePaint: ProbeKeyPaint): Paint = {
      val (skip, alphaBg, alphaLine) = probeRangePaint

      val size = 2 * skip
      val rectangle: Rectangle2D = new Rectangle2D.Double(0, 0, size, size)

      // Get a buffered image capable of being transparent.
      val bufferedImage: BufferedImage = g2d.getDeviceConfiguration.createCompatibleImage(size, size, Transparency.TRANSLUCENT)
      val bufferedImageG2D: Graphics2D = bufferedImage.createGraphics()

      // Shade it with a light red color that is almost completely transparent.
      bufferedImageG2D.setColor(OtColor.makeTransparent(ProbeRangeColor, alphaBg))
      bufferedImageG2D.setComposite(AlphaComposite.Src)
      bufferedImageG2D.fill(rectangle)

      // Draw the slanting lines, which are slightly less transparent than the background.
      bufferedImageG2D.setClip(0, 0, size, size)
      bufferedImageG2D.setColor(OtColor.makeTransparent(ProbeRangeColor, alphaLine))

      (0 until size by skip).foreach { v =>
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

    private def probeRangeKeyPain(g2d: Graphics2D, orientation: Orientation): Paint =
      createPatrolFieldPaint(g2d, orientation, probeRangeKeyPaint)

    // The slant of the lines drawn for the IFU1 and IFU2 patrol fields.
    private sealed trait Orientation
    private case object NorthEast extends Orientation
    private case object SouthEast extends Orientation
  }