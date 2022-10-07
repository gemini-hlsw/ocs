package edu.gemini.spModel.target

import edu.gemini.spModel.core.Coordinates
import edu.gemini.shared.util.immutable.ImOption
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.skycalc.{ Coordinates => SCoordinates }

abstract class SPSkyObject extends WatchablePos {
  // Java accessors using Gemini's Option type and boxed primitives.
  type GOLong   = GOption[java.lang.Long]
  type GODouble = GOption[java.lang.Double]

  def getSkycalcCoordinates(time: GOLong): GOption[SCoordinates]

  def getCoordinates(time: Option[Long]): Option[Coordinates]

  def getCoordinatesAsJava(time: GOLong): GOption[Coordinates] =
    ImOption.fromScalaOpt(getCoordinates(time.toScalaOpt.map(_.longValue)))

  def setRaDegrees(ra: Double): Unit

  def setRaHours(value: Double): Unit

  def setRaString(hms: String): Unit

  def setDecDegrees(dec: Double): Unit

  def setDecString(dec: String): Unit

  def setRaDecDegrees(ra: Double, dec: Double): Unit

  def getRaDegrees(time: GOLong): GODouble

  def getRaHours(time: GOLong): GODouble

  def getRaString(time: GOLong): GOption[String]

  def getDecDegrees(time: GOLong): GODouble

  def getDecString(time: GOLong): GOption[String]

  def getName: String
}
