package jsky.app.ot.gemini.inst

import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.gemini.gmos.GmosCommonType.{FPUnitMode, FPUnit}
import edu.gemini.spModel.gemini.gmos.GmosCommonType.DetectorManufacturer.HAMAMATSU
import edu.gemini.spModel.gemini.gmos.GmosScienceAreaGeometry.{ImagingFov, imagingFov, mosFov}
import edu.gemini.spModel.gemini.gmos.{InstGmosSouth, InstGmosNorth, GmosScienceAreaGeometry}
import edu.gemini.spModel.inst.FeatureGeometry
import edu.gemini.spModel.obscomp.SPInstObsComp
import jsky.app.ot.tpe.TpeImageInfo

import java.awt.{Graphics2D, Graphics}
import java.awt.geom.Point2D

import scalaz._
import Scalaz._

/** Plot feature for GMOS, which adds CCD labels to the basic science area plot. */
object GmosSciAreaPlotFeature extends SciAreaPlotFeature(GmosScienceAreaGeometry) {
  override def draw(g: Graphics, tii: TpeImageInfo): Unit = {
    super.draw(g, tii)
    g match {
      case g2d: Graphics2D => drawCcdLabels(g2d)
      case _               => // do nothing
    }
  }

  def fov(obsComp: SPInstObsComp): Option[ImagingFov] = {
    def go(m: FPUnitMode, f: FPUnit): Option[ImagingFov] =
      (((m == FPUnitMode.BUILTIN) && f.isImaging) option imagingFov) orElse
        ((m == FPUnitMode.CUSTOM_MASK) option mosFov)

    obsComp match {
      case gn: InstGmosNorth => go(gn.getFPUnitMode, gn.getFPUnit)
      case gs: InstGmosSouth => go(gs.getFPUnitMode, gs.getFPUnit)
      case _                 => none
    }
  }

  case class CcdLabels(left: String, center: String, right: String) {
    def toList: List[String] = List(left, center, right)
  }

  val hamamatsuLabels = CcdLabels("CCDr", "CCDg", "CCDb")
  val e2vLabels       = CcdLabels("CCD1", "CCD2", "CCD3")

  def labels(obsComp: SPInstObsComp): Option[CcdLabels] = {
    val man = obsComp match {
      case gn: InstGmosNorth => some(gn.getDetectorManufacturer)
      case gs: InstGmosSouth => some(gs.getDetectorManufacturer)
      case _                 => none
    }
    man.map(m => if (m == HAMAMATSU) hamamatsuLabels else e2vLabels)
  }

  def drawCcdLabels(g: Graphics2D): Unit = {
    val inst = _iw.getContext.instrument.get
    for {
      f <- fov(inst)
      l <- labels(inst)
    } drawCcdLabels(g, f, l)
  }

  def drawCcdLabels(g2d: Graphics2D, fov: ImagingFov, labels: CcdLabels): Unit = {
    g2d.setFont(SciAreaPlotFeature.PosAngleFont)
    val fm = g2d.getFontMetrics

    val posAngle      = Angle.fromDegrees(_iw.getContext.instrument.posAngleOrZero)
    val posAngleXform = FeatureGeometry.posAngleTransform(posAngle)
    val xform         = _tii.toScreen <| (_.concatenate(posAngleXform))

    fov.toList.zip(labels.toList).foreach { case (shape, label) =>
      val w  = fm.stringWidth(label)
      val b  = shape.getBounds2D
      val x  = b.getCenterX
      val y  = b.getY + b.getHeight / 4
      val p0 = new Point2D.Double(x, y)
      val p1 = new Point2D.Double()
      xform.transform(p0, p1)

      g2d.drawString(label, (p1.getX - w/2).toInt, p1.getY.toInt)
    }
  }
}