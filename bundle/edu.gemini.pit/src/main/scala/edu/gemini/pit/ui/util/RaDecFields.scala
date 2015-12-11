package edu.gemini.pit.ui.util

import MultiFormatTextField.Formatter
import java.text.{ParseException, DecimalFormat}

import edu.gemini.spModel.core.{Declination, RightAscension, Angle}

private trait RaDecFields // for ant

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

object DegreeFormatter {
  def ra(c: String)  = new DegreeFormatter(c, Some(_.abs < 360))
  def dec(c: String) = new DegreeFormatter(c, Some(_.abs <= 90))
}

class HMSFormatter(caption: String) extends Formatter[Double](caption) {
  def toString(d: Double) = Angle.fromDegrees(d).formatHMS
  def fromString(s: String) = Angle.parseHMS(s).toOption.map(_.toDegrees)
}

class DMSFormatter(caption: String) extends Formatter[Double](caption) {
  def toString(d: Double) = Angle.fromDegrees(d).formatDMS
  def fromString(s: String) = Angle.parseDMS(s).toOption.map(_.toDegrees)
}

class DecFormatter(caption: String) extends Formatter[Double](caption) {
  val dms = new DMSFormatter(caption)
  def toString(d: Double)   = dms.toString(d)
  def fromString(s: String) = dms.fromString(s) filter { _.abs <= 90 }
}

class RATextField(initialValue: Double) extends MultiFormatTextField(
  initialValue, new HMSFormatter("HMS"), DegreeFormatter.ra("DEG")) {
  def toRightAscension: RightAscension = RightAscension.fromAngle(Angle.fromDegrees(value))
  DegreePreference.BOX.get match {
    case DegreePreference.DEGREES => selectFormat("DEG")
    case DegreePreference.HMSDMS  => selectFormat("HMS")
  }
}

class DecTextField(initialValue: Double) extends MultiFormatTextField(
  initialValue, new DecFormatter("DMS"), DegreeFormatter.dec("DEG")) {
  def toDeclination: Declination = Declination.fromAngle(Angle.fromDegrees(value)).get
  DegreePreference.BOX.get match {
    case DegreePreference.DEGREES => selectFormat("DEG")
    case DegreePreference.HMSDMS  => selectFormat("DMS")
  }
}

