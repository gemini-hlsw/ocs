package edu.gemini.ags.servlet.estimation

import edu.gemini.spModel.core.{Coordinates, Ephemeris, NonSiderealTarget, Site}

class ToContextHelper {

  def nonSiderealWithSingleEphemerisElement(ra: Double, dec: Double, when: Long): NonSiderealTarget = {
    val e: Ephemeris = Coordinates.fromDegrees(ra, dec).fold[Ephemeris](Ephemeris.empty)(Ephemeris.singleton(Site.GN, when, _)) // N.B. site is irrelevant here
    NonSiderealTarget.empty.copy(ephemeris = e)
  }

}
