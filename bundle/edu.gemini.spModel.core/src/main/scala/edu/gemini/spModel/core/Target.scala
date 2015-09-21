package edu.gemini.spModel.core

import scalaz._, Scalaz._

/** Algebraic type for targets of observation. */
sealed trait Target {

  /** A human-readable name for this `Target`. */
  def name: String

  /** Coordinates (if known) for this target at the specified UNIX time. */
  def coords(time: Long): Option[Coordinates]

  /** Horizons information for this target, if known. */
  def horizonsInfo: Option[Target.HorizonsInfo]

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
      PLens.nil.run
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
  /// HORIZONS INFO
  ///

  final case class HorizonsInfo(objectTypeOrdinal: Int, objectId: Long)

  object HorizonsInfo {

    val objectTypeOrdinal: HorizonsInfo @> Int  = Lens(i => Store(s => i.copy(objectTypeOrdinal = s), i.objectTypeOrdinal))
    val objectId:          HorizonsInfo @> Long = Lens(i => Store(s => i.copy(objectId = s), i.objectId))

    object PioKey {
      val ObjectId = "horizons-object-id"
      val ObjectTypeOrdinal = "horizons-object-type"
    }

  }

  ///
  /// TARGET OF OPPORTUNITY
  ///

  /** Target of opportunity, with no coordinates. */
  case class TooTarget(name: String) extends Target {

    def coords(time: Long) = 
      None
    
    def horizonsInfo = 
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
  case class SiderealTarget (
    name: String,
    coordinates: Coordinates,
    properMotion: Option[ProperMotion],
    magnitudes: List[Magnitude],
    horizonsInfo: Option[Target.HorizonsInfo]) extends Target {

    def coords(date: Long) = 
      Some(coordinates)
  
    def fold[A](too: Target.TooTarget => A,
                sid: Target.SiderealTarget => A,
                non: Target.NonSiderealTarget => A): A = 
      sid(this)

    def magnitudeIn(band: MagnitudeBand): Option[Magnitude] = magnitudes.find(_.band === band)

  }

  object SiderealTarget {
  
    val empty = SiderealTarget("Untitled", Coordinates.zero, None, Nil, None)
  
    val name:        SiderealTarget @> String          = Lens(t => Store(s => t.copy(name = s), t.name))
    val coordinates: SiderealTarget @> Coordinates     = Lens(t => Store(c => t.copy(coordinates = c), t.coordinates))
    val pm:          SiderealTarget @?> ProperMotion   = PLens(t => t.properMotion.map(p => Store(q => t.copy(properMotion = p.some), p)))
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
    ephemeris: List[EphemerisElement],
    horizonsInfo: Option[Target.HorizonsInfo]) extends Target {

    def fold[A](too: Target.TooTarget => A,
                sid: Target.SiderealTarget => A,
                non: Target.NonSiderealTarget => A): A = 
      non(this)

    def coords(date: Long): Option[Coordinates] = 
      for {
        (a, b, f) <- find(date, ephemeris)
        (cA, cB)   = (a.coords, b.coords)
        ra         = RightAscension fromAngle Angle.fromDegrees(f(cA.ra. toAngle.toDegrees, cB.ra. toAngle.toDegrees))
        dec       <- Declination    fromAngle Angle.fromDegrees(f(cA.dec.toAngle.toDegrees, cB.dec.toAngle.toDegrees))
      } yield Coordinates(ra, dec)

    /** Magnitude for this target at the specified time, if known. */
    def magnitude(date: Long): Option[Double] = 
      for {
        (a, b, f) <- find(date, ephemeris)
        mA        <- a.magnitude
        mB        <- b.magnitude
      } yield f(mA, mB)

    private def find(date: Long, es: List[EphemerisElement]): Option[(EphemerisElement, EphemerisElement, (Double, Double) => Double)] = 
      es match {
        case Nil       => None
        case _ :: tail => 
          es.zip(tail).collectFirst { case (a, b) if a.validAt <= date && date <= b.validAt =>
            val factor = (date.doubleValue - a.validAt) / (b.validAt - a.validAt) // between 0 and 1
            def interp(a: Double, b: Double) = a + (b - a) * factor
            (a, b, interp)
          }
      }

  }

  object NonSiderealTarget {
    val name: NonSiderealTarget @> String = Lens(t => Store(s => t.copy(name = s), t.name))
  }
  
}



