package edu.gemini.spModel.core

import scalaz._, Scalaz._

/** Algebraic type for targets of observation. */
sealed trait Target {

  /** A human-readable name for this `Target`. */
  def name: String

  /** Coordinates (if known) for this target at the specified UNIX time. */
  def coords(time: Long): Option[Coordinates]

  /** Coordinates (if known) for this target at the specified epoch. */
  def coordsAtEpoch(e: Epoch): Option[Coordinates] =
    coords(e.toUnixTime)

  /**
   * Coordinates (if known) for this target at the natural epoch for the specified Equinox,
   * precessed as required.
   */
  def coordsForEquinox(e: Equinox): Option[Coordinates] =
    ???

  /** Coordinates (if known) at equinox and epoch J2000. */
  def coordsForJ2000: Option[Coordinates] =
    coordsForEquinox(Equinox.J2000)

  /** Coordinates at equinox and epoch J2000, or (0, 0). */
  def coordsForJ2000orZero: Coordinates =
    coordsForJ2000.getOrElse(Coordinates.zero)

  /** Horizons information for this target, if known. */
  def horizonsInfo: Option[Target.HorizonsInfo]

  
  def isNamedTarget: Boolean =
    fold(too => false, sid => false, non => false, nam => true, con => false)

  // TODO: this is somewhat misleading; perhaps name .hasFixedCoordinates ?
  def isNonSiderealTarget: Boolean =
    fold(too => false, sid => false, non => true, nam => true, con => true)

  /** Alternative to pattern-matching. */
  def fold[A](too: Target.TooTarget         => A,
              sid: Target.SiderealTarget    => A,
              non: Target.NonSiderealTarget => A,
              nam: Target.NamedTarget       => A,
              con: Target.ConicTarget       => A): A

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
      NonSiderealTarget.name.partial.run,
      PLens.nil.run,
      ConicTarget.name.partial.run
    ))

  val coords: Target @?> Coordinates =
    PLens(_.fold(
      PLens.nil.run,
      SiderealTarget.coordinates.partial.run,
      PLens.nil.run,
      PLens.nil.run,
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
                non: Target.NonSiderealTarget => A,
                nam: Target.NamedTarget => A,
                con: Target.ConicTarget => A): A = 
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
    equinox: Equinox,
    properMotion: Option[ProperMotion],
    magnitudes: List[Magnitude],
    horizonsInfo: Option[Target.HorizonsInfo]) extends Target {

    def coords(date: Long) = 
      Some(coordinates)
  
    def fold[A](too: Target.TooTarget => A,
                sid: Target.SiderealTarget => A,
                non: Target.NonSiderealTarget => A,
                nam: Target.NamedTarget => A,
                con: Target.ConicTarget => A): A = 
      sid(this)
  
  }

  object SiderealTarget {
  
    val empty = SiderealTarget("Untitled", Coordinates.zero, Equinox.J2000, None, Nil, None)
  
    val name:        SiderealTarget @> String         = Lens(t => Store(s => t.copy(name = s), t.name))
    val coordinates: SiderealTarget @> Coordinates    = Lens(t => Store(c => t.copy(coordinates = c), t.coordinates))
    val equinox:     SiderealTarget @> Equinox        = Lens(t => Store(e => t.copy(equinox = e), t.equinox))
    val ra:          SiderealTarget @> RightAscension = coordinates >=> Coordinates.ra
    val dec:         SiderealTarget @> Declination    = coordinates >=> Coordinates.dec
  
  }

  ///
  /// NONSIDEREAL TARGET
  ///

  /** Nonsidereal target with an ephemeris. */
  case class NonSiderealTarget(
    name: String,
    ephemeris: List[EphemerisElement],
    equinox: Equinox,
    horizonsInfo: Option[Target.HorizonsInfo]) extends Target {

    def fold[A](too: Target.TooTarget => A,
                sid: Target.SiderealTarget => A,
                non: Target.NonSiderealTarget => A,
                nam: Target.NamedTarget => A,
                con: Target.ConicTarget => A): A = 
      non(this)

    /** Treat this as a `SiderealTarget` by fixing the coordinates. */ // TODO: track down magnitude information
    def fix(date: Long): Option[SiderealTarget] =
      coords(date).map(SiderealTarget(name, _, equinox, None, Nil, horizonsInfo))

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

  ///
  /// NAMED TARGET (SOLAR OBJECT, KNOWN TO TCS)
  ///

  /** Enumerated type of named targets recognized by the TCC. */
  sealed abstract class NamedTarget(val name: String, val horizonsId: String) extends Target with Product with Serializable {

    def coords(time: Long) =
      None

    val horizonsInfo =
      None // TODO

    def fold[A](too: Target.TooTarget => A,
                sid: Target.SiderealTarget => A,
                non: Target.NonSiderealTarget => A,
                nam: Target.NamedTarget => A,
                con: Target.ConicTarget => A): A = nam(this)

  }

  object NamedTarget {

    case object Moon    extends NamedTarget("Moon",    "301")
    case object Mercury extends NamedTarget("Mercury", "199")
    case object Venus   extends NamedTarget("Venus",   "299")
    case object Mars    extends NamedTarget("Mars",    "499")
    case object Jupiter extends NamedTarget("Jupiter", "599")
    case object Saturn  extends NamedTarget("Saturn",  "699")
    case object Uranus  extends NamedTarget("Uranus",  "799")
    case object Neptune extends NamedTarget("Neptune", "899")
    case object Pluto   extends NamedTarget("Pluto",   "999")

    val values: List[NamedTarget] =
      List(Moon, Mercury, Venus, Mars, Jupiter, Saturn, Uranus, Neptune, Pluto)

    def fromString(name: String): Option[NamedTarget] =
      values.find(_.name == name)

  }

  ///
  /// CONIC TARGET (UNDERSTOOD BY TCS)
  ///

  /** The type of targets defined by orbital elements. */
  sealed trait ConicTarget extends Target {
    import ConicParameter._

    def name: String
    def epochOfElevation: EpochOfElevation
    def inclination: Inclination
    def longitudeOfAscendingNode: LongitudeOfAscendingNode
    def eccentricity: Double
    def perihelion: Perihelion
    def coords(time: Long) = None
    val horizonsInfo = None // TODO

    def fold[A](too: Target.TooTarget => A,
                sid: Target.SiderealTarget => A,
                non: Target.NonSiderealTarget => A,
                nam: Target.NamedTarget => A,
                con: Target.ConicTarget => A): A = 
      con(this)

    def foldConic[A](com: ConicTarget.JplComet => A,
                     min: ConicTarget.JplMinorPlanet => A): A

  }

  object ConicTarget {
    import ConicParameter._

    /// CONIC TARGET LENSES

    val name:                     ConicTarget @> String                   = Lens(_.foldConic(JplComet.name.run, JplMinorPlanet.name.run))
    val epochOfElevation:         ConicTarget @> EpochOfElevation         = Lens(_.foldConic(JplComet.epochOfElevation.run, JplMinorPlanet.epochOfElevation.run))
    val inclination:              ConicTarget @> Inclination              = Lens(_.foldConic(JplComet.inclination.run, JplMinorPlanet.inclination.run))
    val longitudeOfAscendingNode: ConicTarget @> LongitudeOfAscendingNode = Lens(_.foldConic(JplComet.longitudeOfAscendingNode.run, JplMinorPlanet.longitudeOfAscendingNode.run))
    val eccentricity:             ConicTarget @> Double                   = Lens(_.foldConic(JplComet.eccentricity.run, JplMinorPlanet.eccentricity.run))
    val perihelion:               ConicTarget @> Perihelion               = Lens(_.foldConic(JplComet.perihelion.run, JplMinorPlanet.perihelion.run))

    val periDistance:      ConicTarget @?> PerihelionDistance = PLens(_.foldConic(JplComet.periDistance.partial.run, PLens.nil.run))
    val epochOfPerihelion: ConicTarget @?> EpochOfPerihelion  = PLens(_.foldConic(JplComet.epochOfPerihelion.partial.run, PLens.nil.run))
    val meanDist:          ConicTarget @?> MeanDistance       = PLens(_.foldConic(PLens.nil.run, JplMinorPlanet.meanDist.partial.run))
    val meanAnomaly:       ConicTarget @?> MeanAnomaly        = PLens(_.foldConic(PLens.nil.run, JplMinorPlanet.meanAnomaly.partial.run))

    /// JPL COMET

    final case class JplComet(
      name: String,
      epochOfElevation: EpochOfElevation,
      inclination: Inclination,
      longitudeOfAscendingNode: LongitudeOfAscendingNode,
      eccentricity: Double,
      perihelion: Perihelion,
      periDistance: PerihelionDistance,
      epochOfPerihelion: EpochOfPerihelion) extends ConicTarget {

      def foldConic[A](com: JplComet => A, min: JplMinorPlanet => A): A =
        com(this)

    }

    object JplComet {
      val name:                     JplComet @> String                   = Lens(c => Store(s => c.copy(name = s), c.name))
      val epochOfElevation:         JplComet @> EpochOfElevation         = Lens(c => Store(s => c.copy(epochOfElevation = s), c.epochOfElevation))
      val inclination:              JplComet @> Inclination              = Lens(c => Store(s => c.copy(inclination = s), c.inclination))
      val longitudeOfAscendingNode: JplComet @> LongitudeOfAscendingNode = Lens(c => Store(s => c.copy(longitudeOfAscendingNode = s), c.longitudeOfAscendingNode))
      val eccentricity:             JplComet @> Double                   = Lens(c => Store(s => c.copy(eccentricity = s), c.eccentricity))
      val perihelion:               JplComet @> Perihelion               = Lens(c => Store(s => c.copy(perihelion = s), c.perihelion))
      val periDistance:             JplComet @> PerihelionDistance       = Lens(c => Store(s => c.copy(periDistance = s), c.periDistance))
      val epochOfPerihelion:        JplComet @> EpochOfPerihelion        = Lens(c => Store(s => c.copy(epochOfPerihelion = s), c.epochOfPerihelion))      
    }

    /// JPL MINOR PLANET

    final case class JplMinorPlanet(
      name: String,
      epochOfElevation: EpochOfElevation,
      inclination: Inclination,
      longitudeOfAscendingNode: LongitudeOfAscendingNode,
      eccentricity: Double,
      perihelion: Perihelion,
      meanDist: MeanDistance,
      meanAnomaly: MeanAnomaly) extends ConicTarget {

      def foldConic[A](com: JplComet => A, min: JplMinorPlanet => A): A =
        min(this)

    }

    object JplMinorPlanet {
      val name:                     JplMinorPlanet @> String                   = Lens(c => Store(s => c.copy(name = s), c.name))
      val epochOfElevation:         JplMinorPlanet @> EpochOfElevation         = Lens(c => Store(s => c.copy(epochOfElevation = s), c.epochOfElevation))
      val inclination:              JplMinorPlanet @> Inclination              = Lens(c => Store(s => c.copy(inclination = s), c.inclination))
      val longitudeOfAscendingNode: JplMinorPlanet @> LongitudeOfAscendingNode = Lens(c => Store(s => c.copy(longitudeOfAscendingNode = s), c.longitudeOfAscendingNode))
      val eccentricity:             JplMinorPlanet @> Double                   = Lens(c => Store(s => c.copy(eccentricity = s), c.eccentricity))
      val perihelion:               JplMinorPlanet @> Perihelion               = Lens(c => Store(s => c.copy(perihelion = s), c.perihelion))
      val meanDist:                 JplMinorPlanet @> MeanDistance             = Lens(c => Store(s => c.copy(meanDist = s), c.meanDist))
      val meanAnomaly:              JplMinorPlanet @> MeanAnomaly              = Lens(c => Store(s => c.copy(meanAnomaly = s), c.meanAnomaly))
    }


  }



}



