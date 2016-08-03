package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import java.util.GregorianCalendar

import edu.gemini.spModel.core.Coordinates

object EphemerisElement {
  import Target._

  private lazy val df = javax.xml.datatype.DatatypeFactory.newInstance

  private[immutable] def mkCoords(m: M.EphemerisElement): Coordinates = {
    val hd = Option(m.getHmsDms).flatMap(_.toCoordinates.toOption)
    val dd = Option(m.getDegDeg).flatMap(_.toCoordinates.toOption)
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

  val empty = EphemerisElement(Coordinates.zero, None, System.currentTimeMillis)

}

case class EphemerisElement(coords: Coordinates, magnitude: Option[Double], validAt: Long) {

  import EphemerisElement._

  def mutable = {
    val m = Factory.createEphemerisElement
    val degDeg = Factory.createDegDegCoordinates()
    degDeg.setRa(new java.math.BigDecimal(coords.ra.toAngle.toDegrees))
    degDeg.setDec(new java.math.BigDecimal(coords.dec.toDegrees))
    m.setDegDeg(degDeg)
    m.setMagnitude(magnitude.map(java.math.BigDecimal.valueOf).orNull)
    m.setValidAt(validAt)
    m
  }

}