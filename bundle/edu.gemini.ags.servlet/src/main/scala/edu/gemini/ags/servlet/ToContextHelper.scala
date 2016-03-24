package edu.gemini.ags.servlet

import edu.gemini.spModel.core.{Ephemeris, Coordinates, NonSiderealTarget}

import scalaz.IMap

class ToContextHelper {

  def nonSiderealWithSingleEphemerisElement(ra: Double, dec: Double, when: Long): NonSiderealTarget = {
    val e: Ephemeris = Coordinates.fromDegrees(ra, dec).fold[Ephemeris](IMap.empty)(IMap.singleton(when, _))
    NonSiderealTarget.empty.copy(ephemeris = e)
  }

}
