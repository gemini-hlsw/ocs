package edu.gemini.spModel.target

import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.skycalc.{Coordinates => SCoordinates}
import edu.gemini.spModel.core.{Angle, Coordinates, Declination, RightAscension}
import edu.gemini.spModel.pio.{ParamSet, PioFactory}


/** We need a mutable wrapper for Coordinates so that they can be managed by
  * an editor and the TPE.
  */
final class SPCoordinates(var coordinates: Coordinates) extends SPSkyObject {
  import SPCoordinates._

  type JDouble = java.lang.Double

  def this() =
    this(Coordinates.zero)

  /** Return a paramset describing these SPCoordinates. */
  def getParamSet(factory: PioFactory): ParamSet = {
    val ps = factory.createParamSet(ParamSetName)
    ps.addParamSet(TargetParamSetCodecs.CoordinatesParamSetCodec.encode(CoordinatesName, coordinates))
    ps
  }

  /** Re-initialize these SPCoordinates from the given paramset. */
  def setParamSet(ps: ParamSet): Unit = {
    if (ps != null) {
      val tps = ps.getParamSet(CoordinatesName)
      if (tps != null)
        TargetParamSetCodecs.CoordinatesParamSetCodec.decode(tps).toOption.foreach(setCoordinates)
    }
  }

  override def clone: SPCoordinates =
    new SPCoordinates(coordinates)

  override def getCoordinates(time: Option[Long]): Option[Coordinates] =
    Some(coordinates)

  def getCoordinates: Coordinates =
    coordinates

  def setCoordinates(coordinates: Coordinates): Unit = {
    this.coordinates = coordinates
    _notifyOfUpdate()
  }

  override def getSkycalcCoordinates(time: GOLong): GOption[SCoordinates] =
    Some(new SCoordinates(coordinates.ra.toDegrees, coordinates.dec.toDegrees)).asGeminiOpt

  override def setRaDegrees(value: Double): Unit =
    setCoordinates(Coordinates.ra.set(coordinates, RightAscension.fromDegrees(value)))

  override def setRaHours(value: Double): Unit = {
    setCoordinates(Coordinates.ra.set(coordinates, RightAscension.fromHours(value)))
  }

  override def setRaString(hms: String): Unit =
    for {
      a <- Angle.parseHMS(hms).toOption
      c = Coordinates.ra.set(coordinates, RightAscension.fromAngle(a))
    } setCoordinates(c)

  override def setDecDegrees(value: Double): Unit =
    Declination.fromDegrees(value)
      .foreach(dec => setCoordinates(Coordinates.dec.set(coordinates, dec)))

  override def setDecString(dms: String): Unit =
    for {
      a <- Angle.parseDMS(dms).toOption
      d <- Declination.fromAngle(a)
      c = Coordinates.dec.set(coordinates, d)
    } setCoordinates(c)

  override def setRaDecDegrees(ra: Double, dec: Double): Unit =
    Coordinates.fromDegrees(ra, dec).foreach(setCoordinates)

  override def getRaDegrees(time: GOLong): GODouble = {
    val d: JDouble = coordinates.ra.toDegrees
    Some(d).asGeminiOpt
  }

  override def getRaHours(time: GOLong): GODouble = {
    val d: JDouble = coordinates.ra.toDegrees
    Some(d).asGeminiOpt
  }

  override def getRaString(time: GOLong): GOption[String] =
    Some(coordinates.ra.toAngle.formatHMS).asGeminiOpt

  override def getDecDegrees(time: GOLong): GODouble = {
    val d: JDouble = coordinates.dec.toDegrees
    Some(d).asGeminiOpt
  }

  override def getDecString(time: GOLong): GOption[String] =
    Some(coordinates.dec.toAngle.formatHMS).asGeminiOpt
}

object SPCoordinates {
  val ParamSetName = "spCoordinates"
  val CoordinatesName   = "coordinates"
}