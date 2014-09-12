package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }
import scala.collection.JavaConverters._
import java.text.ParseException

sealed trait Coordinates {
  def toHmsDms: HmsDms
  def toDegDeg: DegDeg
}

object Coordinates {

  val empty = DegDeg.empty

  def apply(m: M.Coordinates): Coordinates = m match {
    case c: M.DegDegCoordinates => DegDeg(c)
    case c: M.HmsDmsCoordinates => HmsDms(c)
  }

}

case class Sign(sn: Int) {
  require(-1 <= sn && sn <= 1, "Invalid signum: %d".format(sn))
  def *(value: Double) = sn * value
  override def toString = if (sn == -1) "-" else ""
}

object Sign {
  def apply(s: String): Sign = s match {
    case null => Sign(1)
    case ""   => Sign(1)
    case "+"  => Sign(1)
    case "-"  => Sign(-1)
  }
  def apply[A](n: A)(implicit x: Numeric[A]): Sign = Sign(x.signum(n))
}

private object Sexigesimal {
  val Pat   = """([-+])?(\d\d?):(\d\d?):(\d\d?\.?\d*)""".r

  def parse[T](s: String, f: (Sign, Int, Int, Double) => T): Option[T] = s.trim match {
    case Pat(a, b, c, d) => Some(f(Sign(a), b.toInt, c.toInt, d.toFloat))
    case _               => None
  }
}

//
// STRUCTURED DMS
//

object DMS extends SchemaResource("Target.xsd") with App {

//  private lazy val dmsPattern = patternFor("DecDms")

  def apply(s: String): DMS = Sexigesimal.parse(s, DMS(_,_,_,_)).getOrElse(throw new ParseException(s, 0))

  def apply(deg: Double): DMS = {
    val sign = Sign(deg)
    val d = deg.abs % 360 // not normalized to positive; assumed to be in (-90, 90) if it's Dec
    val m = (deg.abs - deg.abs.intValue) * 60
    val s = (m - m.intValue) * 60
    DMS(sign, d.intValue, m.intValue, s)
  }

  // In some cases nn:mm:59.9999999999 becomes nn:mm:60.000 which isn't good. So we have to work around it.
  def format(sn: Sign, a: Int, b: Int, c: Double):String = (a, b, c) match {
    case (_, 60, _) => format(sn, a + 1, 59, c)
    case _ =>
      val s = "%s%d:%02d:%06.3f".format(sn, a, b, c)
      if (s.endsWith("60.000")) format(sn, a, b + 1, 0) else s
  }

//    for {
//      h <- 0 to 89
//      m <- 0 to 59
//    } {
//      val a = "-%d:%02d:%06.3f".format(h, m, 0.0)
//      val b = apply(a)
//      val c = apply(b.toDegrees)
//      if (b.toString != c.toString) println("%s => %s".format(b, c))
//    }

}


case class DMS(sn: Sign, d: Int, m: Int, s: Double) {
  // If this is a declination, d:m:s will be in [-90:00:00, 90:00:00] but in the general case
  // it's just an arbitrary int.
  //  require(d.abs < 90 || (d == 90 && m == 0 && s == 0), "Invalid DMS: %d:%d:%f".format(d, m, s))
  require(d >= 0,  "Invalid DMS: %s".format(DMS.format(sn, d, m, s)))
  require(m >= 0 && m < 60, "Invalid DMS: %s".format(DMS.format(sn, d, m, s)))
  require(s >= 0 && s < 60, "Invalid DMS: %s".format(DMS.format(sn, d, m, s)))

  override def toString = DMS.format(sn, d, m, s)
  def toDegrees = sn * (d + m / 60.0 + s / (60 * 60))
}

//
// STRUCTURED HMS
//

object HMS extends SchemaResource("Target.xsd") with App {

//  private lazy val hmsPattern = patternFor("RaHms")

  def apply(s: String): HMS = Sexigesimal.parse(s, HMS(_,_,_,_)).getOrElse(throw new ParseException(s, 0))

  def apply(sn: Sign, h: Int, m: Int, s: Double): HMS = sn match {
    case Sign(-1) => HMS(-1 * HMS(h, m, s).toDegrees)
    case _        => HMS(h, m, s)
  }

  def apply(deg: Double): HMS = {
    val d = ((deg % 360) + 360) % 360
    val h = d / 15
    val m = (h - h.intValue) * 60
    val s = (m - m.intValue) * 60
    HMS(h.intValue, m.intValue, s)
  }

  // In some cases nn:mm:59.9999999999 becomes nn:mm:60.000 which isn't good. So we have to work around it.
  def format(a: Int, b: Int, c: Double): String = (a, b, c) match {
    case (24, _, _) => format(0, b, c)
    case (_, 60, _) => format(a + 1, 59, c)
    case _ =>
      val s = "%d:%02d:%06.3f".format(a, b, c)
      if (s.endsWith("60.000")) format(a, b + 1, 0) else s
  }

//  for {
//    h <- 0 to 23
//    m <- 0 to 59
//    s0 <- 0 to 5900
//    s = s0 / 100.0
//  } {
//    val a = "%d:%02d:%06.3f".format(h, m, s)
//    val b = apply(a)
//    val c = apply(b.toDegrees)
//    if (b.toString != c.toString) println("%s => %s".format(b, c))
//  }
//
//  val a = apply("17:19:00")
//
//  println(a)
//  println(apply(a.toDegrees))

}

case class HMS(h: Int, m: Int, s: Double) {
  require(h >= 0 && h < 24, "Invalid HMS: %s".format(HMS.format(h, m, s)))
  require(m >= 0 && m < 60, "Invalid HMS: %s".format(HMS.format(h, m, s)))
  require(s >= 0 && s < 60, "Invalid HMS: %s".format(HMS.format(h, m, s)))
  override def toString = HMS.format(h, m, s)
  def toDegrees = h * 15.0 + m / 4.0 + s / (4.0 * 60)
}

/** DegDeg **/
object DegDeg {
  def apply(m: M.DegDegCoordinates) = new DegDeg(m)
  val empty = DegDeg(0, 0)
}

case class DegDeg(ra: BigDecimal, dec: BigDecimal) extends Coordinates {

  require(ra >= 0 && ra < 360.0, "RA out of bounds: " + ra)
  require(dec.abs <= 90.0, "Dec out of bounds: " + dec)

  def this(m: M.DegDegCoordinates) = this(m.getRa, m.getDec)

  def mutable = {
    val m = Factory.createDegDegCoordinates
    m.setRa(ra.bigDecimal)
    m.setDec(dec.bigDecimal)
    m
  }

  def toDegDeg = this
  def toHmsDms = HmsDms(HMS(ra.toDouble), DMS(dec.toDouble))

}

object HmsDms extends SchemaResource("Target.xsd") {

  private lazy val hmsPattern = patternFor("RaHms")
  private lazy val dmsPattern = patternFor("DecDms")

  def apply(m: M.HmsDmsCoordinates): HmsDms = apply(m.getRa, m.getDec)

  def apply(raHms: String, decDms: String): HmsDms = {
    require(hmsPattern.matcher(raHms).matches, "Invalid RA: " + raHms)
    require(dmsPattern.matcher(decDms).matches, "Invalid Dec: " + decDms)
    HmsDms(HMS(raHms), DMS(decDms))
  }

}

case class HmsDms(ra: HMS, dec: DMS) extends Coordinates {

  def mutable = {
    val m = Factory.createHmsDmsCoordinates()
    m.setRa(ra.toString)
    m.setDec(dec.toString)
    m
  }

  def toDegDeg = DegDeg(ra.toDegrees, dec.toDegrees)
  def toHmsDms = this

}
