package edu.gemini.horizons

import edu.gemini.spModel.core.Coordinates

import java.time.Instant

package object api {

  implicit class EphemerisEntryOps(ee: EphemerisEntry) {
    def timestamp: Long =
      ee.getDate.getTime

    def instant: Instant =
      Instant.ofEpochMilli(timestamp)

    def coords: Option[Coordinates] = {
      val cs = ee.getCoordinates
      Coordinates.fromDegrees(cs.getRaDeg, cs.getDecDeg)
    }
  }

}
