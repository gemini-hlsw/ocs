package edu.gemini.spModel.target

import edu.gemini.spModel.core.Coordinates
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.skycalc.{ Coordinates => SCoordinates }

abstract class SPSkyObject extends WatchablePos {
  def getSkycalcCoordinates(time: GOption[java.lang.Long]): GOption[SCoordinates]

  def getCoordinates(when: Option[Long]): Option[Coordinates]

  def setRaDecDegrees(ra: Double, dec: Double): Unit
}
