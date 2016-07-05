package edu.gemini.ags.impl

import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.skycalc.{DDMMSS, HHMMSS}
import edu.gemini.spModel.core.{Declination, RightAscension, Angle, Coordinates, MagnitudeBand, Magnitude, SiderealTarget}
import edu.gemini.spModel.target.SPTarget

/**
 *
 */
trait Helpers {
  val mt       = ProbeLimitsTable.loadOrThrow()
  val zeroBase = basePosition("00:00:00.000 00:00:00.00")

    // Convert a string and magnitude to a SiderealTarget.
  final def siderealTarget(name: String, raDecStr: String, rMag: Double): SiderealTarget =
    SiderealTarget.empty.copy(name = name, coordinates = parseCoordinates(raDecStr), magnitudes = List(new Magnitude(rMag, MagnitudeBand.R)))

  // Convert a string to a base.
  final def basePosition(raDecStr: String): SPTarget = {
    val c = parseCoordinates(raDecStr)
    new SPTarget(c.ra.toAngle.toDegrees, c.dec.toDegrees)
  }

  final def parseCoordinates(raDecStr: String): Coordinates = {
    val (raStr, decStr) = raDecStr.span(_ != ' ')
    val ra  = Angle.fromDegrees(HHMMSS.parse(raStr).toDegrees.getMagnitude)
    val dec = Angle.fromDegrees(DDMMSS.parse(decStr.trim).toDegrees.getMagnitude)
    Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
  }

}
