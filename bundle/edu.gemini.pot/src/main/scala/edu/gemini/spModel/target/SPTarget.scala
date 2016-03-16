package edu.gemini.spModel.target

import edu.gemini.spModel.core.SiderealTarget
import edu.gemini.spModel.core.Target
import edu.gemini.spModel.core.TooTarget
import edu.gemini.spModel.core._
import edu.gemini.spModel.pio.ParamSet
import edu.gemini.spModel.pio.PioFactory
import edu.gemini.spModel.target.system.CoordinateParam.Units
import edu.gemini.spModel.target.system.{HmsDegTarget, ITarget, TransitionalSPTarget}

import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.skycalc.{ Coordinates => SCoordinates }

import scalaz.@?>

object SPTarget {

  /** Construct a new SPTarget from the given paramset */
  def fromParamSet(pset: ParamSet): SPTarget = {
    return SPTargetPio.fromParamSet(pset)
  }

}

final class SPTarget(private var _target: ITarget) extends TransitionalSPTarget {

  def this() =
    this(new HmsDegTarget)

  /** SPTarget with the given RA/Dec in degrees. */
  def this(raDeg: Double, degDec: Double) =
    this {
      val hms = new HmsDegTarget
      hms.getRa.setAs(raDeg, Units.DEGREES)
      hms.getDec.setAs(degDec, Units.DEGREES)
      hms
    }

  /** Return the contained target. */
  protected def getTarget: ITarget =
    _target

  /** Replace the contained target and notify listeners. */
  protected def setTarget(target: ITarget): Unit = {
    _target = target
    _notifyOfUpdate()
  }

  /** Return a paramset describing this SPTarget. */
  def getParamSet(factory: PioFactory): ParamSet =
    SPTargetPio.getParamSet(this, factory)

  /** Re-initialize this SPTarget from the given paramset */
  def setParamSet(paramSet: ParamSet): Unit = {
    SPTargetPio.setParamSet(paramSet, this)
  }

  /** Clone this SPTarget. */
  override def clone: SPTarget = {
    val t: SPTarget = new SPTarget(_target.clone)
    t.setNewTarget(_newTarget)
    t
  }

  private var _newTarget: Target = SiderealTarget.empty

  def getNewTarget: Target =
    _newTarget

  def setNewTarget(target: Target): Unit = {
    _newTarget = target
    _notifyOfUpdate()
  }

  // Accessors and mutators below are provided for convenience but all can be implemented locally
  // by inspecting and/or replacing the Target.

  def setSidereal(): Unit =
    _newTarget match {
      case _: SiderealTarget => ()
      case _ => setNewTarget(SiderealTarget.empty)
    }

  def setNonSidereal(): Unit =
    _newTarget match {
      case _: NonSiderealTarget => ()
      case _ => setNewTarget(NonSiderealTarget.empty)
    }

  def setTOO(): Unit =
    _newTarget match {
      case _: TooTarget => ()
      case _ => setNewTarget(TooTarget.empty)
    }

  // Some convenience lenses
  private val ra:  Target @?> RightAscension = Target.coords >=> Coordinates.ra.partial
  private val dec: Target @?> Declination    = Target.coords >=> Coordinates.dec.partial

  def getName(): String =
    _newTarget.name

  def setName(s: String): Unit =
    setNewTarget(Target.name.set(_newTarget, s))

  def setRaDegrees(value: Double): Unit =
    ra.set(_newTarget, RightAscension.fromDegrees(value)).foreach(setNewTarget)

  def setRaHours(value: Double): Unit =
    ra.set(_newTarget, RightAscension.fromHours(value)).foreach(setNewTarget)

  def setDecDegrees(value: Double): Unit =
    Declination.fromDegrees(value).flatMap(dec.set(_newTarget, _)).foreach(setNewTarget)

  def setRaDecDegrees(ra: Double, dec: Double): Unit =
    Coordinates.fromDegrees(ra, dec).flatMap(Target.coords.set(_newTarget, _)).foreach(setNewTarget)

  def setSpectralDistribution(sd: Option[SpectralDistribution]): Unit =
    Target.spectralDistribution.set(_newTarget, sd).foreach(setNewTarget)

  def setSpatialProfile(sp: Option[SpatialProfile]): Unit =
    Target.spatialProfile.set(_newTarget, sp).foreach(setNewTarget)

  def getSpectralDistribution: Option[SpectralDistribution] =
    Target.spectralDistribution.get(_newTarget).flatten

  def getSpatialProfile: Option[SpatialProfile] =
    Target.spatialProfile.get(_newTarget).flatten

  def isSidereal(): Boolean =
    _newTarget.fold(_ => true, _ => true, _ => false)

  def isNonSidereal(): Boolean =
    !isSidereal()

}
