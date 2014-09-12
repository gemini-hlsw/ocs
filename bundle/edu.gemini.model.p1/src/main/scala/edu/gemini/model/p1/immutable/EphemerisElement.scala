package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }
import java.util.GregorianCalendar
import javax.xml.datatype.XMLGregorianCalendar

object EphemerisElement {

  private lazy val df = javax.xml.datatype.DatatypeFactory.newInstance

  private[immutable] def mkCoords(m: M.EphemerisElement): Coordinates = {
    val hd = Option(m.getHmsDms).map(hd => HmsDms(hd))
    val dd = Option(m.getDegDeg).map(dd => DegDeg(dd))
    hd.orElse(dd).get
  }

  private implicit val long2xgc = (ms: Long) => {
    val gc = new GregorianCalendar
    gc.setTimeInMillis(ms)
    df.newXMLGregorianCalendar(gc)
  }

  def apply(m: M.EphemerisElement): EphemerisElement = EphemerisElement(
    mkCoords(m),
    Option(m.getMagnitude).map(_.doubleValue),
    m.getValidAt.toGregorianCalendar.getTimeInMillis)

  val empty = EphemerisElement(Coordinates.empty, None, System.currentTimeMillis)

}

case class EphemerisElement(coords: Coordinates, magnitude: Option[Double], validAt: Long) {

  import EphemerisElement._

  def mutable = {
    val m = Factory.createEphemerisElement
    m.setDegDeg(coords.toDegDeg.mutable)
    m.setMagnitude(magnitude.map(java.math.BigDecimal.valueOf).orNull)
    m.setValidAt(validAt)
    m
  }

}