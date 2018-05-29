package edu.gemini.spModel.target

import edu.gemini.spModel.core.{Coordinates, Declination, RightAscension}
import edu.gemini.spModel.pio.{ParamSet, PioFactory}

/** We need a mutable wrapper for Coordinates so that they can be managed by
  * an editor and the TPE.
  */
final class SPCoordinates(private var coordinates: Coordinates) extends WatchablePos {
  import SPCoordinates._

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
      if (tps != null) {
        val c = TargetParamSetCodecs.CoordinatesParamSetCodec.decode(tps).toOption.get
        setCoordinates(c)
      }
    }
  }

  /** Clone this SPCoordinates. */
  override def clone: SPCoordinates =
    new SPCoordinates(coordinates)

  def getCoordinates: Coordinates =
    coordinates

  def setCoordinates(coordinates: Coordinates): Unit = {
    this.coordinates = coordinates
    _notifyOfUpdate()
  }

  def setRaDegrees(value: Double): Unit =
    setCoordinates(Coordinates.ra.set(coordinates, RightAscension.fromHours(value)))

  def setDecDegrees(value: Double): Unit =
    Declination.fromDegrees(value)
      .foreach(dec => setCoordinates(Coordinates.dec.set(coordinates, dec)))

}

object SPCoordinates {
  val ParamSetName = "spCoordinates"
  val CoordinatesName   = "coordinates"
}