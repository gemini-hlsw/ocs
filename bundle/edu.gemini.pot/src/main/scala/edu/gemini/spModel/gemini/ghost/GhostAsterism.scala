package edu.gemini.spModel.gemini.ghost

import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.CloudCover
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.{Asterism, AsterismType}

import java.time.Instant

import scalaz._
import Scalaz._

// TODO: Unsure if we should return sky positions in allSpTargets as dummy targets.

/** Base trait for the three GHOST asterism types: two target, beam switching,
  * and high resolution.
  */
sealed trait GhostAsterism extends Asterism {

  def base: Option[Coordinates]

  /** All Targets that comprise the asterism. */
  override def allTargets: NonEmptyList[Target] =
    allSpTargets.map(_.getTarget)
}

/** GHOST asterism model. */
object GhostAsterism {

  /** The GHOST guide fibers can be enabled or disabled for each science target.
    * Typically they are enabled for bright targets (< B mag 18 for standard
    * resolution, < B mag 17 for high resolution) but disabled for faint targets.
    * Guide state can also be explicitly disabled, say, in a crowded field where
    * guide fibers are less effective.
    */
  sealed abstract class GuideFiberState private (val name: String) extends Product with Serializable

  object GuideFiberState {
    case object Enabled  extends GuideFiberState("enabled")
    case object Disabled extends GuideFiberState("disabled")

    val enabled:  GuideFiberState = Enabled
    val disabled: GuideFiberState = Disabled

    val All = NonEmptyList(enabled, disabled)

    implicit val EqualGuideFiberState: Equal[GuideFiberState] =
      Equal.equalA[GuideFiberState]

    def fromString(s: String): Option[GuideFiberState] =
      All.findLeft(_.name == s)

    def unsafeFromString(s: String): GuideFiberState =
      fromString(s).getOrElse(sys.error(s"Unknown guide fiber state: $s"))
  }


  /** GHOST targets are associated with a guiding state (enabled or disabled), referring to
    * whether the dedicated guide fibers surrounding the science target should be used.
    *
    * There is a default guiding state based on magnitude, but this can be
    * explicitly overridden.
    */
  final case class GhostTarget(spTarget: SPTarget,
                               explicitGuideFiberState: Option[GuideFiberState]) {

    def coordinates(when: Option[Instant]): Option[Coordinates] =
      spTarget.getCoordinates(when.map(_.toEpochMilli))

    def copyWithClonedTarget: GhostTarget =
      copy(spTarget = spTarget.clone)
  }

  object GhostTarget {
    val empty: GhostTarget = GhostTarget(new SPTarget, None)

    val target: GhostTarget @> SPTarget =
      Lens.lensu((a, b) => a.copy(spTarget = b), _.spTarget)
    val explicitGuideFiberState: GhostTarget @> Option[GuideFiberState] =
      Lens.lensu((a,b) => a.copy(explicitGuideFiberState = b), _.explicitGuideFiberState)

    /** The magnitude at which guide state is disabled by default at standard
      * resolution.
      */
    val StandardResCutoff: Magnitude =
      Magnitude(18.0, MagnitudeBand.B, None, MagnitudeSystem.Vega)

    /** The magnitude at which guide state is disabled by default at high
      * resolution.
      */
    val HighResCutoff: Magnitude =
      Magnitude(17.0, MagnitudeBand.B, None, MagnitudeSystem.Vega)

    private def defaultGuideFiberState(t: GhostTarget, cutoff: Magnitude): GuideFiberState =
      t.spTarget.getMagnitude(MagnitudeBand.B).forall(_.value < cutoff.value) ? GuideFiberState.enabled | GuideFiberState.disabled

    private def guideFiberState(t: GhostTarget, cutoff: Magnitude): GuideFiberState =
      t.explicitGuideFiberState | defaultGuideFiberState(t, cutoff)

    /** Computes the GuideFiberState for the given target and cloud cover in
      * standard resolution mode.
      */
    def standardResGuideFiberState(t: GhostTarget, cc: CloudCover): GuideFiberState =
      guideFiberState(t, cc.adjustMagnitude(StandardResCutoff))

    /** Computes the GuideFiberState for the given target and cloud cover in
      * high resolution mode.
      */
    def highResGuideFiberState(t: GhostTarget, cc: CloudCover): GuideFiberState =
      guideFiberState(t, cc.adjustMagnitude(HighResCutoff))
  }

  /** GHOST standard resolution asterism type.  In this mode, one or two targets (one of which may be
    * a sky position) are observed simultaneously with both IFUs at standard resolution.
    */
  sealed trait StandardResolution extends GhostAsterism {
    import StandardResolution._

    override def allSpTargets: NonEmptyList[SPTarget] = this match {
      case SingleTarget(t,_)    => NonEmptyList(t.spTarget)
      case DualTarget(t1,t2,_)  => NonEmptyList(t1.spTarget, t2.spTarget)
      case TargetPlusSky(t,_,_) => NonEmptyList(t.spTarget)
      case SkyPlusTarget(_,t,_) => NonEmptyList(t.spTarget)
    }

    /** Obtains the base position, which defaults to the half-way point between
      * the two targets but may be explicitly specified instead.
      */
    override def basePosition(when: Option[Instant]): Option[Coordinates] =
      base orElse defaultBasePosition(when)

    override def basePositionProperMotion: Option[ProperMotion] =
      allTargets.map(Target.pm.get).fold

    def ifu1GuideFiberState(cc: CloudCover): GuideFiberState =
      StandardResolution.guideFiberState(ifu1, cc)

    def ifu2GuideFiberState(cc: CloudCover): GuideFiberState = {
      ifu2.map(t => StandardResolution.guideFiberState(t, cc)).getOrElse(GuideFiberState.Disabled)
    }

    def ifu1: Either[Coordinates, GhostTarget] = this match {
      case SingleTarget(t,_)    => Right(t)
      case DualTarget(t,_,_)    => Right(t)
      case TargetPlusSky(t,_,_) => Right(t)
      case SkyPlusTarget(s,_,_) => Left(s)
    }
    def ifu2: Option[Either[Coordinates, GhostTarget]] = this match {
      case SingleTarget(_,_)    => None
      case DualTarget(_,t,_)    => Some(Right(t))
      case TargetPlusSky(_,s,_) => Some(Left(s))
      case SkyPlusTarget(_,t,_) => Some(Right(t))
    }

    // Sky coords are immutable, so we don't need to copy them.
    override def copyWithClonedTargets: Asterism = this match {
      case SingleTarget(t,b)    => SingleTarget(t.copyWithClonedTarget,b)
      case DualTarget(t1,t2,b)  => DualTarget(t1.copyWithClonedTarget, t2.copyWithClonedTarget, b)
      case TargetPlusSky(t,s,b) => TargetPlusSky(t.copyWithClonedTarget, s, b)
      case SkyPlusTarget(s,t,b) => SkyPlusTarget(s, t.copyWithClonedTarget, b)
    }

    /** In any single target object mode, the default base position is the same as the
      * target position. In dual target mode, we interpolate between the two.
      */
    private def defaultBasePosition(when: Option[Instant]): Option[Coordinates] = this match {
      case SingleTarget(t,_)    => t.coordinates(when)
      case DualTarget(t1,t2,_)  => interpolateCoords(t1.coordinates(when), t2.coordinates(when))
      case TargetPlusSky(t,_,_) => t.coordinates(when)
      case SkyPlusTarget(_,t,_) => t.coordinates(when)
    }

    override def asterismType: AsterismType = this match {
      case SingleTarget(_,_)    => AsterismType.GhostSingleTarget
      case DualTarget(_,_,_)    => AsterismType.GhostDualTarget
      case TargetPlusSky(_,_,_) => AsterismType.GhostTargetPlusSky
      case SkyPlusTarget(_,_,_) => AsterismType.GhostSkyPlusTarget
    }
  }

  case class SingleTarget(target: GhostTarget, override val base: Option[Coordinates]) extends StandardResolution
  case class DualTarget(target1: GhostTarget, target2: GhostTarget, override val base: Option[Coordinates]) extends StandardResolution
  case class TargetPlusSky(target: GhostTarget, sky: Coordinates, override val base: Option[Coordinates]) extends StandardResolution
  case class SkyPlusTarget(sky: Coordinates, target: GhostTarget, override val base: Option[Coordinates]) extends StandardResolution

  object StandardResolution {
    def guideFiberState(e: Either[Coordinates, GhostTarget], cc: CloudCover): GuideFiberState =
      e.rightMap(t => GhostTarget.standardResGuideFiberState(t, cc)).right.getOrElse(GuideFiberState.Disabled)

    private[ghost] def interpolateCoords(c1Opt: Option[Coordinates], c2Opt: Option[Coordinates]): Option[Coordinates] = for {
      c1 <- c1Opt
      c2 <- c2Opt
    } yield c1.interpolate(c2, 0.5)

    val emptySingleTarget:  SingleTarget       = SingleTarget(GhostTarget.empty, None)
    val emptyDualTarget:    DualTarget         = DualTarget(GhostTarget.empty, GhostTarget.empty, None)
    val emptyTargetPlusSky: TargetPlusSky      = TargetPlusSky(GhostTarget.empty, Coordinates.zero, None)
    val emptySkyPlusTarget: SkyPlusTarget      = SkyPlusTarget(Coordinates.zero, GhostTarget.empty, None)
    val empty:              StandardResolution = emptySingleTarget

    val SingleTargetIFU1: SingleTarget @> GhostTarget =
      Lens.lensu((a,b) => a.copy(target = b), _.target)
    val SingleTargetBase: SingleTarget @> Option[Coordinates] =
      Lens.lensu((a,b) => a.copy(base = b), _.base)

    val DualTargetIFU1: DualTarget @> GhostTarget =
      Lens.lensu((a,b) => a.copy(target1 = b), _.target1)
    val DualTargetIFU2: DualTarget @> GhostTarget =
      Lens.lensu((a,b) => a.copy(target2 = b), _.target2)
    val DualTargetBase: DualTarget @> Option[Coordinates] =
      Lens.lensu((a,b) => a.copy(base = b), _.base)

    val TargetPlusSkyIFU1: TargetPlusSky @> GhostTarget =
      Lens.lensu((a,b) => a.copy(target = b), _.target)
    val TargetPlusSkyIFU2: TargetPlusSky @> Coordinates =
      Lens.lensu((a,b) => a.copy(sky = b), _.sky)
    val TargetPlusSkyBase: TargetPlusSky @> Option[Coordinates] =
      Lens.lensu((a,b) => a.copy(base = b), _.base)

    val SkyPlusTargetIFU1: SkyPlusTarget @> Coordinates =
      Lens.lensu((a,b) => a.copy(sky = b), _.sky)
    val SkyPlusTargetIFU2: SkyPlusTarget @> GhostTarget =
      Lens.lensu((a,b) => a.copy(target = b), _.target)
    val SkyPlusTargetBase: SkyPlusTarget @> Option[Coordinates] =
      Lens.lensu((a,b) => a.copy(base = b), _.base)
  }

  /** High resolution mode.
    *
    * It has a calibration duration (of type Duration), which should be determined
    * by exposure time.
    *
    * The target is always observed using the high resolution IFU1.  The sky
    * coordinates are observed using the sky fibers of IFU2, not SRIFU2. The
    * guide fibers will be used by default because the target must be bright,
    * but can be explicitly turned off.
    */
  final case class HighResolution(ghostTarget: GhostTarget,
                                  sky: Option[Coordinates],
                                  override val base: Option[Coordinates]) extends GhostAsterism {

    override def allSpTargets: NonEmptyList[SPTarget] =
      NonEmptyList(ghostTarget.spTarget)

    /** Defines the default base position to be the same as the target position. */
    override def basePosition(when: Option[Instant]): Option[Coordinates] =
      base orElse ghostTarget.coordinates(when)

    override def basePositionProperMotion: Option[ProperMotion] =
      Target.pm.get(ghostTarget.spTarget.getTarget)

    /** Deterimines the guide fiber state for the HRIFU1.  Typically this will
      * be enabled since the target is bright but may be explicitly turned off.
      */
    def guideFiberState(cc: CloudCover): GuideFiberState =
      GhostTarget.highResGuideFiberState(ghostTarget, cc)

    override def copyWithClonedTargets: Asterism =
      copy(ghostTarget = ghostTarget.copyWithClonedTarget)

    override def asterismType: AsterismType = AsterismType.GhostHighResolution
  }

  object HighResolution {
    val empty: HighResolution = HighResolution(GhostTarget.empty, None, None)

    val IFU1: HighResolution @> GhostTarget =
      Lens.lensu((a,b) => a.copy(ghostTarget = b), _.ghostTarget)
    val IFU2: HighResolution @> Option[Coordinates] =
      Lens.lensu((a,b) => a.copy(sky = b), _.sky)
    val Base: HighResolution @> Option[Coordinates] =
      Lens.lensu((a,b) => a.copy(base = b), _.base)
  }

  // Convenience create methods for Java since trying to access nested objects and case
  // classes results cannot be resolved.
  def createEmptySingleTargetAsterism: Asterism = {
    StandardResolution.emptySingleTarget.copyWithClonedTargets
  }

  def createEmptyDualTargetAsterism: Asterism = {
    StandardResolution.emptyDualTarget.copyWithClonedTargets
  }

  def createEmptyTargetPlusSkyAsterism: Asterism = {
    StandardResolution.emptyTargetPlusSky.copyWithClonedTargets
  }

  def createEmptySkyPlusTargetAsterism: Asterism = {
    StandardResolution.emptySkyPlusTarget.copyWithClonedTargets
  }

  def createEmptyHighResolutionAsterism: Asterism = {
    HighResolution.empty.copyWithClonedTargets
  }
}