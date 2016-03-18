package edu.gemini.spModel.target

import edu.gemini.spModel.core.SiderealTarget
import edu.gemini.spModel.core.Target
import edu.gemini.spModel.core.TooTarget
import edu.gemini.spModel.core._
import edu.gemini.spModel.pio.ParamSet
import edu.gemini.spModel.pio.PioFactory
import edu.gemini.spModel.target.system.{ TransitionalSPTarget}

import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.skycalc.{ Coordinates => SCoordinates }

import scalaz.@?>

object SPTarget {

  /** Construct a new SPTarget from the given paramset */
  def fromParamSet(pset: ParamSet): SPTarget = {
    return SPTargetPio.fromParamSet(pset)
  }

}
final class SPTarget(private var target: Target) extends TransitionalSPTarget {

  def this() =
    this(SiderealTarget.empty)

  /** SPTarget with the given RA/Dec in degrees. */
  @deprecated("Use safe constructors; this one throws if declination is invalid.", "16B")
  def this(raDeg: Double, degDec: Double) =
    this(Coordinates.fromDegrees(raDeg, degDec)
      .map(cs => SiderealTarget.empty.copy(coordinates = cs))
      .getOrElse(sys.error("invalid coords")))

  /** Return a paramset describing this SPTarget. */
  def getParamSet(factory: PioFactory): ParamSet =
    SPTargetPio.getParamSet(this, factory)

  /** Re-initialize this SPTarget from the given paramset */
  def setParamSet(paramSet: ParamSet): Unit =
    SPTargetPio.setParamSet(paramSet, this)

  /** Clone this SPTarget. */
  override def clone: SPTarget =
    new SPTarget(target)

  def getTarget: Target =
    target

  def setNewTarget(target: Target): Unit = {
    this.target = target
    _notifyOfUpdate()
  }

  // Accessors and mutators below are provided for convenience but all can be implemented locally
  // by inspecting and/or replacing the Target.

  def setSidereal(): Unit =
    target match {
      case _: SiderealTarget => ()
      case _ => setNewTarget(SiderealTarget.empty)
    }

  def setNonSidereal(): Unit =
    target match {
      case _: NonSiderealTarget => ()
      case _ => setNewTarget(NonSiderealTarget.empty)
    }

  def setTOO(): Unit =
    target match {
      case _: TooTarget => ()
      case _ => setNewTarget(TooTarget.empty)
    }

  // Some convenience lenses
  private val ra:  Target @?> RightAscension = Target.coords >=> Coordinates.ra.partial
  private val dec: Target @?> Declination    = Target.coords >=> Coordinates.dec.partial

  def getName: String =
    target.name

  def setName(s: String): Unit =
    setNewTarget(Target.name.set(target, s))

  def setRaDegrees(value: Double): Unit =
    ra.set(target, RightAscension.fromDegrees(value)).foreach(setNewTarget)

  def setRaHours(value: Double): Unit =
    ra.set(target, RightAscension.fromHours(value)).foreach(setNewTarget)

  def setRaString(hms: String): Unit =
    for {
      a <- Angle.parseHMS(hms).toOption
      t <- ra.set(target, RightAscension.fromAngle(a))
    } setNewTarget(t)

  def setDecDegrees(value: Double): Unit =
    for {
      d <- Declination.fromDegrees(value)
      t <- dec.set(target, d)
    } setNewTarget(t)

  def setDecString(dms: String): Unit =
    for {
      a <- Angle.parseDMS(dms).toOption
      d <- Declination.fromAngle(a)
      t <- dec.set(target, d)
    } setNewTarget(t)

  def setRaDecDegrees(ra: Double, dec: Double): Unit =
    for {
      cs <- Coordinates.fromDegrees(ra, dec)
      t  <- Target.coords.set(target, cs)
    } setNewTarget(t)

  def setSpectralDistribution(sd: Option[SpectralDistribution]): Unit =
    Target.spectralDistribution.set(target, sd).foreach(setNewTarget)

  def setSpatialProfile(sp: Option[SpatialProfile]): Unit =
    Target.spatialProfile.set(target, sp).foreach(setNewTarget)

  def getSpectralDistribution: Option[SpectralDistribution] =
    Target.spectralDistribution.get(target).flatten

  def getSpatialProfile: Option[SpatialProfile] =
    Target.spatialProfile.get(target).flatten

  def isTooTarget: Boolean =
    target.fold(_ => true,  _ => false, _ => false)

  def isSidereal: Boolean =
    target.fold(_ => false, _ => true,  _ => false)

  def isNonSidereal: Boolean =
    target.fold(_ => false, _ => false, _ => true)

  def getCoordinates(when: Option[Long]): Option[Coordinates] =
    target.fold(
      _ => None,
      s => Some(s.coordinates),
      n => when.flatMap(n.coords)
    )

  // Accessors for Java that use Gemini's Option type and boxed primitives

  type GOLong   = GOption[java.lang.Long]
  type GODouble = GOption[java.lang.Double]

  private def gcoords[A](when: GOLong)(f: Coordinates => A): GOption[A] =
    getCoordinates(when.asScalaOpt.map(_.longValue())).map(f).asGeminiOpt

  def getRaHours(time: GOLong): GODouble =
    gcoords(time)(_.ra.toHours)

  def getRaDegrees(time: GOLong):GODouble =
    gcoords(time)(_.ra.toDegrees)

  def getRaString(time: GOLong): GOption[String] =
    gcoords(time)(_.ra.toAngle.formatHMS)

  def getDecDegrees(time: GOLong): GODouble =
    gcoords(time)(_.dec.toDegrees)

  def getDecString(time: GOLong): GOption[String] =
    gcoords(time)(_.dec.toAngle.formatDMS)

  def getSkycalcCoordinates(time: GOLong): GOption[SCoordinates] =
    gcoords(time)(cs => new SCoordinates(cs.ra.toDegrees, cs.dec.toDegrees))

  def getSiderealTarget: Option[SiderealTarget] =
    target.fold(_ => None, Some(_), _ => None)

}
