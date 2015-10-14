package edu.gemini.spModel.core

import scalaz._, Scalaz._

/** Algebraic type for targets of observation. */
sealed trait Target {

  /** A human-readable name for this `Target`. */
  def name: String

  /** Coordinates (if known) for this target at the specified UNIX time. */
  def coords(time: Long): Option[Coordinates]

  /** Alternative to pattern-matching. */
  def fold[A](too: Target.TooTarget         => A,
              sid: Target.SiderealTarget    => A,
              non: Target.NonSiderealTarget => A): A

}

/** Module of constructors for `Target`. */
object Target {

  /** Target of opportunity, with no coordinates. */
  case class TooTarget(name: String) extends Target {

    def coords(time: Long) = 
      None
    
    def fold[A](too: Target.TooTarget => A,
                sid: Target.SiderealTarget => A,
                non: Target.NonSiderealTarget => A): A = 
      too(this)
  
  }

  object TooTarget extends (String => TooTarget) {
    val empty = TooTarget("Untitled")
    val name: TooTarget @> String = Lens(t => Store(s => t.copy(name = s), t.name))
  }

  ///
  /// SIDEREAL TARGET
  ///

  /** Sidereal target with optional proper motion. */
  case class SiderealTarget(
    name: String,
    coordinates: Coordinates,
    properMotion: Option[ProperMotion],
    redshift: Option[Redshift],
    parallax: Option[Parallax],
    magnitudes: List[Magnitude]) extends Target {

    def coords(date: Long) = 
      Some(coordinates)
  
    def fold[A](too: Target.TooTarget => A,
                sid: Target.SiderealTarget => A,
                non: Target.NonSiderealTarget => A): A = 
      sid(this)

    def magnitudeIn(band: MagnitudeBand): Option[Magnitude] = magnitudes.find(_.band === band)

  }

  object SiderealTarget {

    val empty = SiderealTarget("Untitled", Coordinates.zero, None, None, None, Nil)
  
    val name:           SiderealTarget @> String          = Lens(t => Store(s => t.copy(name = s), t.name))
    val coordinates:    SiderealTarget @> Coordinates     = Lens(t => Store(c => t.copy(coordinates = c), t.coordinates))
    val properMotion:   SiderealTarget @> Option[ProperMotion]   = Lens(t => Store(c => t.copy(properMotion = c), t.properMotion))
    val radialVelocity: SiderealTarget @> Option[RadialVelocity] = Lens(t => Store(s => t.copy(radialVelocity = s), t.radialVelocity))
    val redshift:       SiderealTarget @> Option[Redshift] = Lens(t => Store(s => t.copy(redshift = s), t.redshift))
    val parallax:       SiderealTarget @> Option[Parallax] = Lens(t => Store(s => t.copy(parallax = s), t.parallax))
    val magnitudes:     SiderealTarget @> List[Magnitude] = Lens(t => Store(c => t.copy(magnitudes = c), t.magnitudes))

    val ra:          SiderealTarget @> RightAscension  = coordinates >=> Coordinates.ra
    val dec:         SiderealTarget @> Declination     = coordinates >=> Coordinates.dec

  }

  ///
  /// NONSIDEREAL TARGET
  ///

  /** Nonsidereal target with an ephemeris. */
  case class NonSiderealTarget(
    name: String,
    ephemeris: Ephemeris,
    horizonsDesignation: Option[HorizonsDesignation],
    magnitudes: List[Magnitude]) extends Target {

    def fold[A](too: Target.TooTarget => A,
                sid: Target.SiderealTarget => A,
                non: Target.NonSiderealTarget => A): A = 
      non(this)

    def coords(date: Long): Option[Coordinates] =
      ephemeris.iLookup(date)

  }

  object NonSiderealTarget {

    val empty = NonSiderealTarget("Untitled", ==>>.empty, None, Nil)

    val ephemeris:           NonSiderealTarget @> Ephemeris            = Lens(t => Store(s => t.copy(ephemeris = s), t.ephemeris))
    val name:                NonSiderealTarget @> String               = Lens(t => Store(s => t.copy(name = s), t.name))
    val horizonsDesignation: NonSiderealTarget @> Option[HorizonsDesignation] = Lens(t => Store(s => t.copy(horizonsDesignation = s), t.horizonsDesignation))
    val magnitudes:          NonSiderealTarget @> List[Magnitude]      = Lens(t => Store(c => t.copy(magnitudes = c), t.magnitudes))

    val ephemerisElements: NonSiderealTarget @> List[(Long, Coordinates)] = ephemeris.xmapB(_.toList)(==>>.fromList(_))

  }
  
}
