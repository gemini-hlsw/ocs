package edu.gemini.pit.ui.util

import MultiFormatTextField.Formatter
import edu.gemini.model.p1.immutable.{ HMS, DMS }
import java.text.{ParseException, DecimalFormat}

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
  def toString(d: Double) = HMS(d).toString
  def fromString(s: String) = try {
    Some(HMS(s).toDegrees)
  } catch {
    case e:ParseException => None
    case e:Exception => e.printStackTrace(); None
  }
}

class DMSFormatter(caption: String) extends Formatter[Double](caption) {
  def toString(d: Double) = DMS(d).toString
  def fromString(s: String) = try {
    Some(DMS(s).toDegrees)
  } catch {
    case e:ParseException => None
    case e:Exception => e.printStackTrace(); None
  }
}

class DecFormatter(caption: String) extends Formatter[Double](caption) {
  val dms = new DMSFormatter(caption)
  def toString(d: Double)   = dms.toString(d)
  def fromString(s: String) = dms.fromString(s) filter { _.abs <= 90 }
}

class RATextField(initialValue: Double) extends MultiFormatTextField(
  initialValue, new HMSFormatter("HMS"), DegreeFormatter.ra("DEG")) {
  DegreePreference.BOX.get match {
    case DegreePreference.DEGREES => selectFormat("DEG")
    case DegreePreference.HMSDMS  => selectFormat("HMS")
  }
}

class DecTextField(initialValue: Double) extends MultiFormatTextField(
  initialValue, new DecFormatter("DMS"), DegreeFormatter.dec("DEG")) {
  DegreePreference.BOX.get match {
    case DegreePreference.DEGREES => selectFormat("DEG")
    case DegreePreference.HMSDMS  => selectFormat("DMS")
  }
}

