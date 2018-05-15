package edu.gemini.spModel.core

import scalaz._, Scalaz._

/**
 * Algebraic type for targets of observation.
 */
sealed trait Target extends Product with Serializable {

  /** A human-readable name for this Target. */
  def name: String

  /** Coordinates (if known) for this target at the specified UNIX time. */
  def coords(time: Long): Option[Coordinates]

  /** Coordinates (if known) for this target at the specified UNIX time, if any. */
  def coords(time: Option[Long]): Option[Coordinates] =
    time.flatMap(coords) orElse fold(_ => None, s => Some(s.coordinates), _ => None)

  /** Alternative to pattern-matching. */
  def fold[A](too: TooTarget         => A,
              sid: SiderealTarget    => A,
              non: NonSiderealTarget => A): A

  // Some predicates, useful in crappy parts of the codebase
  def isToo:         Boolean = fold(_ => true,  _ => false, _ => false)
  def isSidereal:    Boolean = fold(_ => false, _ => true,  _ => false)
  def isNonSidereal: Boolean = fold(_ => false, _ => false, _ => true)

}

object Target extends TargetLenses

trait TargetLenses {

  private def runTarget[A <: Target, B](l: A @> B):  A => IndexedStore[B, B, Target] =
   (l.run _) andThen (_.map(x => x: Target))

  private def pRunTarget[A <: Target, B](l: A @?> B): A => Option[Store[B, Target]] =
   (l.run _) andThen (_.map(_.map(x => x: Target)))

  val name: Target @> String =
    Lens(_.fold(
      runTarget(TooTarget.name),
      runTarget(SiderealTarget.name),
      runTarget(NonSiderealTarget.name)
    ))

  val coords: Target @?> Coordinates =
    PLens(_.fold(
      PLens.nil.run,
      pRunTarget(SiderealTarget.coordinates.partial),
      PLens.nil.run
    ))

  val ra:  Target @?> RightAscension =
    coords >=> Coordinates.ra.partial

  val dec: Target @?> Declination    =
    coords >=> Coordinates.dec.partial

  val pm: Target @?> ProperMotion =
    PLens(_.fold(
      PLens.nil.run,
      pRunTarget(SiderealTarget.properMotion.partial >=> PLens.somePLens[ProperMotion]),
      PLens.nil.run
    ))

  val magnitudes: Target @?> List[Magnitude] =
    PLens(_.fold(
      PLens.nil.run,
      pRunTarget(SiderealTarget.magnitudes.partial),
      pRunTarget(NonSiderealTarget.magnitudes.partial)
    ))

  def const[A, B](b: B): A @> B =
    Lens.lensu((a, _) => a, _ => b)

  val spatialProfile: Target @?> Option[SpatialProfile] =
    PLens(_.fold(
      PLens.nil.run,
      pRunTarget(SiderealTarget.spatialProfile.partial),
      pRunTarget(NonSiderealTarget.spatialProfile.partial)
    ))

  val spectralDistribution: Target @?> Option[SpectralDistribution] =
    PLens(_.fold(
      PLens.nil.run,
      pRunTarget(SiderealTarget.spectralDistribution.partial),
      pRunTarget(NonSiderealTarget.spectralDistribution.partial)
    ))

  val horizonsDesignation: Target @?> Option[HorizonsDesignation] =
    PLens(_.fold(
      PLens.nil.run,
      PLens.nil.run,
      pRunTarget(NonSiderealTarget.horizonsDesignation.partial)
    ))

  val ephemeris: Target @?> Ephemeris =
    PLens(_.fold(
      PLens.nil.run,
      PLens.nil.run,
      pRunTarget(NonSiderealTarget.ephemeris.partial)
    ))

  val redshift: Target @?> Option[Redshift] =
    PLens(_.fold(
      PLens.nil.run,
      pRunTarget(SiderealTarget.redshift.partial),
      PLens.nil.run
    ))

}


/**
 * Target of opportunity, with no coordinates. Use '''TooTarget.empty''' if no name is given.
 * @param name a human-readable name
 */
case class TooTarget(name: String) extends Target {

  /** Always returns [[None]] */
  def coords(time: Long): Option[Coordinates] =
    None

  def fold[A](too: TooTarget => A, sid: SiderealTarget => A, non: NonSiderealTarget => A): A =
    too(this)

}

object TooTarget extends TooTargetLenses {
  val empty = TooTarget("Untitled")
}

trait TooTargetLenses {
  val name: TooTarget @> String =
    Lens.lensu((a, b) => a.copy(name = b), _.name)
}

/**
 * Properties and derived methods in common for defined (non-TOO) targets.
 */
sealed trait DefinedTarget extends Target {

  def magnitudes: List[Magnitude]

  def spectralDistribution: Option[SpectralDistribution]

  def spatialProfile: Option[SpatialProfile]

  def magnitudeIn(b: MagnitudeBand): Option[Magnitude] =
    magnitudes.find(_.band == b)

}


/**
 * Sidereal target with optional proper motion. Note that the preferred method of construction is
 * via '''SiderealTarget.empty.copy(...)''' with named arguments.
 * @param name a human-readable name
 * @param coordinates target coordinates
 * @param properMotion optional proper motion information
 * @param redshift optional target redshift
 * @param parallax optional parallax
 * @param magnitudes list of magnitudes
 * @param spectralDistribution optional spectral distribution, for ITC
 * @param spatialProfile optional spectral distribution, for ITC
 */
case class SiderealTarget(
  name:                 String,
  coordinates:          Coordinates,
  properMotion:         Option[ProperMotion],
  redshift:             Option[Redshift],
  parallax:             Option[Parallax],
  magnitudes:           List[Magnitude],
  spectralDistribution: Option[SpectralDistribution],
  spatialProfile:       Option[SpatialProfile]
) extends DefinedTarget {

  /** Returns the fixed coordinates; proper motion is not yet taken into account. */
  def coords(date: Long): Option[Coordinates] =
    Some(coordinates)

  def fold[A](too: TooTarget => A, sid: SiderealTarget => A, non: NonSiderealTarget => A): A =
    sid(this)

}

object SiderealTarget extends SiderealTargetLenses {
  val empty = SiderealTarget("Untitled", Coordinates.zero, None, None, None, Nil, None, None)
}

trait SiderealTargetLenses {

  val name: SiderealTarget @> String =
    Lens.lensu((a, b) => a.copy(name = b), _.name)

  val coordinates: SiderealTarget @> Coordinates =
    Lens.lensu((a, b) => a.copy(coordinates = b), _.coordinates)

  val properMotion: SiderealTarget @> Option[ProperMotion] =
    Lens.lensu((a, b) => a.copy(properMotion = b), _.properMotion)

  val redshift: SiderealTarget @> Option[Redshift] =
    Lens.lensu((a, b) => a.copy(redshift = b), _.redshift)

  val parallax: SiderealTarget @> Option[Parallax] =
    Lens.lensu((a, b) => a.copy(parallax = b), _.parallax)

  val magnitudes: SiderealTarget @> List[Magnitude] =
    Lens.lensu((a, b) => a.copy(magnitudes = b), _.magnitudes)

  val spectralDistribution: SiderealTarget @> Option[SpectralDistribution] =
    Lens.lensu((a, b) => a.copy(spectralDistribution = b), _.spectralDistribution)

  val spatialProfile: SiderealTarget @> Option[SpatialProfile] =
    Lens.lensu((a, b) => a.copy(spatialProfile = b), _.spatialProfile)

  val ra: SiderealTarget @> RightAscension =
    coordinates >=> Coordinates.ra

  val dec: SiderealTarget @> Declination =
    coordinates >=> Coordinates.dec

}


/**
 * Nonsidereal target with an ephemeris. Note that the preferred method of construction is
 * via '''NonSiderealTarget.empty.copy(...)''' with named arguments.
 * @param name a human-readable name
 * @param ephemeris a map from points in time to coordinates
 * @param horizonsDesignation optional unique Horizons identifier
 * @param magnitudes list of magnitudes
 * @param spectralDistribution optional spectral distribution, for ITC
 * @param spatialProfile optional spectral distribution, for ITC
 */
case class NonSiderealTarget(
  name:                 String,
  ephemeris:            Ephemeris,
  horizonsDesignation:  Option[HorizonsDesignation],
  magnitudes:           List[Magnitude],
  spectralDistribution: Option[SpectralDistribution],
  spatialProfile:       Option[SpatialProfile]
) extends DefinedTarget {

  def coords(date: Long): Option[Coordinates] =
    ephemeris.iLookup(date)

  def fold[A](too: TooTarget => A, sid: SiderealTarget => A,non: NonSiderealTarget => A): A =
    non(this)

  override def toString =
    s"NonSiderealTarget($name,«${ephemeris.size}»,$horizonsDesignation,$magnitudes,$spectralDistribution,$spatialProfile)"

}

object NonSiderealTarget extends NonSiderealTargetLenses {
  val empty = NonSiderealTarget("Untitled", Ephemeris.empty, None, Nil, None, None)
}

trait NonSiderealTargetLenses {

  val ephemeris: NonSiderealTarget @> Ephemeris =
    Lens.lensu((a, b) => a.copy(ephemeris = b), _.ephemeris)

  val name: NonSiderealTarget @> String =
    Lens.lensu((a, b) => a.copy(name = b), _.name)

  val horizonsDesignation: NonSiderealTarget @> Option[HorizonsDesignation] =
    Lens.lensu((a, b) => a.copy(horizonsDesignation = b), _.horizonsDesignation)

  val magnitudes: NonSiderealTarget @> List[Magnitude] =
    Lens.lensu((a, b) => a.copy(magnitudes = b), _.magnitudes)

  val spectralDistribution: NonSiderealTarget @> Option[SpectralDistribution] =
    Lens.lensu((a, b) => a.copy(spectralDistribution = b), _.spectralDistribution)

  val spatialProfile: NonSiderealTarget @> Option[SpatialProfile] =
    Lens.lensu((a, b) => a.copy(spatialProfile = b), _.spatialProfile)

}

