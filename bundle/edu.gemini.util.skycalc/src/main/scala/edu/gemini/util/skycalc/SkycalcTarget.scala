package edu.gemini.util.skycalc

import jsky.coords.WorldCoords

/**
 * Base trait for anything that has a position in the sky.
 */
trait SkycalcTarget {
  /** Gets the position of the target at time t. */
  def positionAt(t: Long): WorldCoords
}



// === SIDEREAL TARGETS ===

/**
 * Targets with a fixed position.
 */
case class SiderealTarget(position: WorldCoords) extends SkycalcTarget {
  def positionAt(t: Long) = position
}

object SiderealTarget {
  /** Creates a sidereal target for the given position. */
  def apply(ra: Double, dec: Double): SiderealTarget =
    SiderealTarget(new WorldCoords(ra, dec))
}



// === NON-SIDEREAL TARGETS ===

/** Ephemerides are positions that are valid at a given time. */
case class Ephemeris(t: Long, position: WorldCoords)

/**
 * Targets with positions that change over time.
 * @param positions
 */
case class NonSiderealTarget(positions: Seq[Ephemeris]) extends SkycalcTarget {
  require(positions.length > 0)

  /** Gets the closest known position for the given time. */
  def positionAt(t: Long): WorldCoords = {
    positions.
      reduce({
      (p0, p1) => if(Math.abs(t - p0.t) <= Math.abs(t - p1.t)) p0 else p1
    }).position
  }

}

object NonSiderealTarget {
  /** Creates a non-sidereal target for which only one position is known.
    * Useful for dealing with non-sidereal targets for which positions can not be looked up using the Horizons
    * service, e.g. because they are missing a Horizons ID. */
  def apply(coords: WorldCoords): NonSiderealTarget = NonSiderealTarget(ephemeridesFor(coords))

  /** Creates a non-sidereal target for which only one position is known.
    * Useful for dealing with non-sidereal targets for which positions can not be looked up using the Horizons
    * service, e.g. because they are missing a Horizons ID. */
  def apply(ra: Double, dec: Double): NonSiderealTarget = NonSiderealTarget(ephemeridesFor(ra, dec))

  // -- helpers

  private def ephemeridesFor(ra: Double, dec: Double): Seq[Ephemeris] = ephemeridesFor(new WorldCoords(ra, dec))

  private def ephemeridesFor(coords: WorldCoords): Seq[Ephemeris] = Seq(Ephemeris(System.currentTimeMillis(), coords))

}

