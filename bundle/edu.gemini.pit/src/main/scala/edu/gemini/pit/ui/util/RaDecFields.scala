package edu.gemini.pit.ui.util

import MultiFormatTextField.Formatter
import java.text.DecimalFormat

import edu.gemini.spModel.core.{Declination, RightAscension, Angle}

import scala.util.Try
import scalaz._
import Scalaz._

class DegreeFormatter(caption: String, inRange: Option[Double => Boolean] = None) extends Formatter[RightAscension](caption) {
  val raDecFormat = new DecimalFormat("##0.000###")
  def toString(d: RightAscension) = raDecFormat.format(d.toAngle.toDegrees)

  def fromString(s: String) = Try { s.toDouble }.filter(d => inRange.forall(f => f(d))).toOption.map(a => RightAscension.fromAngle(Angle.fromDegrees(a)))
}

class DecDegreeFormatter(caption: String, inRange: Option[Double => Boolean] = None) extends Formatter[Declination](caption) {
  val raDecFormat = new DecimalFormat("##0.000###")
  def toString(d: Declination) = raDecFormat.format(d.toDegrees)

  def fromString(s: String):Option[Declination] = Try { s.toDouble }.filter(d => inRange.forall(f => f(d))).toOption.flatMap(a => Declination.fromAngle(Angle.fromDegrees(a)))
}

object DegreeFormatter {
  def ra(c: String)  = new DegreeFormatter(c, Some(_.abs < 360))
  def dec(c: String) = new DecDegreeFormatter(c, Some(_.abs <= 90))
}

class HMSFormatter(caption: String) extends Formatter[RightAscension](caption) {
  def toString(d: RightAscension) = ~Option(d).map(_.toAngle.formatHMS)
  def fromString(s: String) = Angle.parseHMS(s).toOption.map(RightAscension.fromAngle)
}

class DecFormatter(caption: String) extends Formatter[Declination](caption) {
  def toString(d: Declination) = ~Option(d).map(_.formatDMS)
  def fromString(s: String) = Angle.parseDMS(s).toOption.flatMap(Declination.fromAngle)
}

class RATextField(initialValue: RightAscension) extends MultiFormatTextField(
  initialValue, new HMSFormatter("HMS"), DegreeFormatter.ra("DEG")) {
  def toRightAscension: RightAscension = value
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

