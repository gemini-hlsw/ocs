package edu.gemini.ags.servlet

import edu.gemini.spModel.core.{Ephemeris, Site, Coordinates, NonSiderealTarget}

class ToContextHelper {

  def nonSiderealWithSingleEphemerisElement(ra: Double, dec: Double, when: Long): NonSiderealTarget = {
    val e: Ephemeris = Coordinates.fromDegrees(ra, dec).fold[Ephemeris](Ephemeris.empty)(Ephemeris.singleton(Site.GN, when, _)) // N.B. site is irrelevant here
    NonSiderealTarget.empty.copy(ephemeris = e)
  }

}
