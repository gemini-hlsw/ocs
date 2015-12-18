package edu.gemini.pit.ui.util

import MultiFormatTextField.Formatter
import java.text.{ParseException, DecimalFormat}

import edu.gemini.spModel.core.{Declination, RightAscension, Angle}

import scalaz._
import Scalaz._

class DegreeFormatter(caption: String, inRange: Option[Double => Boolean] = None) extends Formatter[Double](caption) {
  val raDecFormat = new DecimalFormat("##0.000###")
  def toString(d: Double) = raDecFormat.format(d)

  def fromString(s: String) = try {
    s.toDouble match {
      case d if inRange.forall(f => f(d)) => Some(d)
      case _ => None
    }
  } catch {
    case e:ParseException => None
    case e:NumberFormatException => None
    case e:Exception => e.printStackTrace(); None
  }
}

class DecDegreeFormatter(caption: String, inRange: Option[Double => Boolean] = None) extends Formatter[Declination](caption) {
  val raDecFormat = new DecimalFormat("##0.000###")
  def toString(d: Declination) = raDecFormat.format(d.toDegrees)

  def fromString(s: String):Option[Declination] = try {
    s.toDouble match {
      case d if inRange.forall(f => f(d)) => Declination.fromAngle(Angle.fromDegrees(d))
      case _                              => None
    }
  } catch {
    case e:Exception             => None
  }
}

object DegreeFormatter {
  def ra(c: String)  = new DegreeFormatter(c, Some(_.abs < 360))
  def dec(c: String) = new DecDegreeFormatter(c, Some(_.abs <= 90))
}

class HMSFormatter(caption: String) extends Formatter[Double](caption) {
  def toString(d: Double) = Angle.fromDegrees(d).formatHMS
  def fromString(s: String) = Angle.parseHMS(s).toOption.map(_.toDegrees)
}

class DMSFormatter(caption: String) extends Formatter[Double](caption) {
  def toString(d: Double) = Angle.fromDegrees(d).formatDMS
  def fromString(s: String) = Angle.parseDMS(s).toOption.map(_.toDegrees)
}

class DecFormatter(caption: String) extends Formatter[Declination](caption) {
  val dms = new DMSFormatter(caption)
  def toString(d: Declination) = ~Option(d).map(_.formatDMS)
  def fromString(s: String) = Angle.parseDMS(s).toOption.flatMap(Declination.fromAngle)
}

class RATextField(initialValue: Double) extends MultiFormatTextField(
  initialValue, new HMSFormatter("HMS"), DegreeFormatter.ra("DEG")) {
  def toRightAscension: RightAscension = RightAscension.fromAngle(Angle.fromDegrees(value))
  DegreePreference.BOX.get match {
    case DegreePreference.DEGREES => selectFormat("DEG")
    case DegreePreference.HMSDMS  => selectFormat("HMS")
  }
}

class DecTextField(initialValue: Declination) extends MultiFormatTextField(
  initialValue, new DecFormatter("DMS"), DegreeFormatter.dec("DEG")) {
  DegreePreference.BOX.get match {
    case DegreePreference.DEGREES => selectFormat("DEG")
    case DegreePreference.HMSDMS  => selectFormat("DMS")
  }
}

