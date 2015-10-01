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

  ///
  /// TARGET LENSES
  ///

  val name: Target @?> String =
    PLens(_.fold(
      TooTarget.name.partial.run,
      SiderealTarget.name.partial.run,
      NonSiderealTarget.name.partial.run
    ))

  val coords: Target @?> Coordinates =
    PLens(_.fold(
      PLens.nil.run,
      SiderealTarget.coordinates.partial.run,
      PLens.nil.run
    ))

  val pm: Target @?> ProperMotion =
    PLens(_.fold(
      PLens.nil.run,
      SiderealTarget.pm.run,
      PLens.nil.run
    ))

  val magnitudes: Target @?> List[Magnitude] =
    PLens(_.fold(
      PLens.nil.run,
      SiderealTarget.magnitudes.partial.run,
      NonSiderealTarget.magnitudes.partial.run
    ))

  val ephemeris: Target @?> Ephemeris =
    PLens(_.fold(
      PLens.nil.run,
      PLens.nil.run,
      NonSiderealTarget.ephemeris.partial.run
    ))

  val horizonsDesignation: Target @?> HorizonsDesignation =
    PLens(_.fold(
      PLens.nil.run,
      PLens.nil.run,
      NonSiderealTarget.horizonsDesignation.run
    ))

  val raDec: Target @?> (RightAscension, Declination) =
    coords.xmapB(cs => (cs.ra, cs.dec))(Coordinates.tupled)

  val raDecAngles: Target @?> (Angle, Angle) =
    raDec.xmapB(_.bimap(_.toAngle, _.toAngle))(
                _.bimap(RightAscension.fromAngle,
                        Declination.fromAngle(_).getOrElse(throw new IllegalArgumentException("Declination out of range.")))) // hmm

  val raDecDegrees: Target @?> (Double, Double) =
    raDecAngles.xmapB(_.bimap(_.toDegrees, _.toDegrees))(_.bimap(Angle.fromDegrees, Angle.fromDegrees))

  ///
  /// TARGET OF OPPORTUNITY
  ///

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
    val name: TooTarget @> String = Lens(t => Store(s => t.copy(name = s), t.name))
  }

  ///
  /// SIDEREAL TARGET
  ///

  /** Sidereal target with optional proper motion. */
  case class SiderealTarget(name: String, coordinates: Coordinates, properMotion: Option[ProperMotion], radialVelocity: Option[RadialVelocity], magnitudes: List[Magnitude]) extends Target {

    def coords(date: Long) = 
      Some(coordinates)
  
    def fold[A](too: Target.TooTarget => A,
                sid: Target.SiderealTarget => A,
                non: Target.NonSiderealTarget => A): A = 
      sid(this)

    def magnitudeIn(band: MagnitudeBand): Option[Magnitude] = magnitudes.find(_.band === band)

  }

  object SiderealTarget {
  
    val empty = SiderealTarget("Untitled", Coordinates.zero, None, None, Nil)
  
    val name:        SiderealTarget @> String          = Lens(t => Store(s => t.copy(name = s), t.name))
    val coordinates: SiderealTarget @> Coordinates     = Lens(t => Store(c => t.copy(coordinates = c), t.coordinates))
    val pm:          SiderealTarget @?> ProperMotion   = PLens(t => t.properMotion.map(p => Store(q => t.copy(properMotion = p.some), p)))
    val rv:          SiderealTarget @?> RadialVelocity = PLens(t => t.radialVelocity.map(p => Store(q => t.copy(radialVelocity = p.some), p)))
    val ra:          SiderealTarget @> RightAscension  = coordinates >=> Coordinates.ra
    val dec:         SiderealTarget @> Declination     = coordinates >=> Coordinates.dec
    val magnitudes:  SiderealTarget @> List[Magnitude] = Lens(t => Store(c => t.copy(magnitudes = c), t.magnitudes))

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
    val ephemeris:           NonSiderealTarget @> Ephemeris            = Lens(t => Store(s => t.copy(ephemeris = s), t.ephemeris))
    val name:                NonSiderealTarget @> String               = Lens(t => Store(s => t.copy(name = s), t.name))
    val horizonsDesignation: NonSiderealTarget @?> HorizonsDesignation = PLens(t => t.horizonsDesignation.map(p => Store(q => t.copy(horizonsDesignation = p.some), p)))
    val magnitudes:          NonSiderealTarget @> List[Magnitude]      = Lens(t => Store(c => t.copy(magnitudes = c), t.magnitudes))
  }
  
}
