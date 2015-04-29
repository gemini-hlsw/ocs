package jsky.app.ot.gemini.inst

import edu.gemini.shared.util.immutable.{Option => JOption, ImPolygon}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.Offset
import edu.gemini.spModel.inst.ScienceAreaGeometry
import jsky.app.ot.gemini.tpe.EdIterOffsetFeature
import jsky.app.ot.tpe.TpeImageFeature.{Figure, MARKER_SIZE}
import jsky.app.ot.tpe._
import jsky.app.ot.util.PropertyWatcher

import java.awt.{AlphaComposite, Color, Graphics2D, Graphics, BasicStroke, Font}
import java.awt.geom.{AffineTransform, Point2D}
import java.util.logging.Logger

import jsky.util.gui.DrawUtil

import scalaz._
import Scalaz._


object SciAreaPlotFeature {
  val Log = Logger.getLogger(getClass.getName)

  val FovColor     = Color.cyan
  val FovStroke    = new BasicStroke(1F)
  val TickStroke   = new BasicStroke(0F)
  val PosAngleFont = new Font("dialog", Font.PLAIN, 12)

  def tickMark(offset: Point2D): ImPolygon = {
    val m = MARKER_SIZE
    val x = offset.getX
    val y = offset.getY

    ImPolygon(List(
      (x,     y - m * 2),
      (x - m, y - 2    ),
      (x + m, y - 2    )
    ))
  }
}

class SciAreaPlotFeature(sciArea: ScienceAreaGeometry)
  extends TpeImageFeature("Sci Area", s"Show the science area.") with PropertyWatcher with TpeDraggableFeature {

  import SciAreaPlotFeature._

  def getFigures(tpeCtx: TpeContext, offset: Offset, color: Color): List[Figure] = {
    val shapes = tpeCtx.obsContext.toList.flatMap { obsCtx =>
      sciArea.geometry(obsCtx, offset)
    }

    shapes.map { new Figure(_, color, null, FovStroke) }
  }

  // --------------------------------------------------------------
  // Everything that follows is junk required to plug into the TPE.
  // --------------------------------------------------------------

  override def draw(g: Graphics, tii: TpeImageInfo): Unit =
    g match {
      case g2d: Graphics2D =>
        reinit()
        drawFigures(g2d, Offset.zero, FovColor)
        tickMarkFigure(_iw.getContext, tii).foreach { _.draw(g2d, true) }
        drawPosAngleLabel(g2d)

      case _               =>
        Log.warning("draw expecting Graphics2D: " + g.getClass.getName)
    }

  def drawAtOffset(g: Graphics, tii: TpeImageInfo, o: Offset): Unit =
    g match {
      case g2d: Graphics2D =>
        drawFigures(g2d, o, EdIterOffsetFeature.OFFSET_SCI_AREA_COLOR)

      case _               =>
        Log.warning("draw expecting Graphics2D: " + g.getClass.getName)
    }

  def drawFigures(g2d: Graphics2D, o: Offset, c: Color): Unit = {
    val toScreen = _tii.toScreen
    getFigures(_iw.getContext, o, c).foreach { _.transform(toScreen).draw(g2d, false) }
  }

  // The tick mark indicates the position angle.  It should sit atop the
  // science area figure pointing away from the base.  It isn't scaled with
  // the image but rather should always be an absolute size.  For that reason
  // it is created and transformed here.
  def tickMarkFigure(tpeCtx: TpeContext, tii: TpeImageInfo): Option[Figure] =
    tpeCtx.obsContext.map { obsCtx =>
      val yRaw  = sciArea.unadjustedGeometry(obsCtx).fold(0.0) { _.getBounds2D.getMinY }
      val y     = (yRaw * tii.getPixelsPerArcsec) min -30.0
      val tick  = tickMark(new Point2D.Double(0, y))
      val fig   = new Figure(tick, FovColor, AlphaComposite.SrcOver, TickStroke)
      val base  = tii.getBaseScreenPos
      val xform = AffineTransform.getTranslateInstance(base.getX, base.getY)
      xform.rotate(-(tpeCtx.instrument.posAngleOrZero.toRadians + tii.getTheta) * tii.flipRA)
      fig.transform(xform)
    }

  var dragPos: Option[(Int, Int)] = None

  override def dragStart(tme: TpeMouseEvent, tii: TpeImageInfo): JOption[Object] = {
    dragPos = if (isMouseOver(tme)) Some((tme.xWidget, tme.yWidget)) else None
    // nearly useless return value
    dragPos.as(new Object).asGeminiOpt
  }

  override def drag(tme: TpeMouseEvent): Unit =
    dragPos.foreach { _ =>
      dragPos = Some((tme.xWidget, tme.yWidget))
      _iw.setPosAngle(Math.round(_tii.positionAngle(tme).toDegrees))
      _iw.repaint();
    }

  override def dragStop(tme: TpeMouseEvent): Unit =
    dragPos.foreach { _ =>
      drag(tme)
      dragPos = None
      _iw.getContext.instrument.commit()
    }

  override def isMouseOver(tme: TpeMouseEvent): Boolean =
    tickMarkFigure(_iw.getContext, _tii).exists {
      _.shape.getBounds2D.contains(tme.xWidget, tme.yWidget)
    }

  def drawPosAngleLabel(g2d: Graphics2D): Unit =
    dragPos.foreach { case (x,y) =>
      val pa = _iw.getContext.instrument.posAngleOrZero.round.toString
      val s  = s"position angle = $pa deg"
      DrawUtil.drawString(g2d, s, FovColor, Color.black, x, y)
    }

  override def getCategory: TpeImageFeatureCategory =
    TpeImageFeatureCategory.fieldOfView

  override def propertyChange(propName: String): Unit =
    _iw.repaint()
}