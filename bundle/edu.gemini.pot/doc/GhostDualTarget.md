# GHOST Dual Target Mode

GHOST brings an important new feature to the science program model, the ability to observe two targets simultaneously.  All previous instruments are limited to a single science target per observation.  The purpose of this document is to sketch out ideas for how to implement this feature without refactoring the entire program model so that time estimates for doing the work may be created.

The GHOST concept of operations document, section 4.2 “Instrument Operating Modes”, describes a sophisticated set of modes that use the two IFUs with built in guiding capability and dedicated sky fibers in distinct ways. This prescribes a tight level of integration between target definition and instrument features that is unprecedented for Gemini instruments. A few additional facts about dual target support may be relevant to implementation ideas:

* The two target mode applies only to GHOST, and only to GHOST in a particular mode of operations: Dual Target Standard Resolution.  There are other ways to use GHOST that occupy a single target, just as for all other instruments.

* Because there are two IFUs, in beam-switching and high-resolution modes one can be observing a science target and the other taking sky background at a specific coordinate.

* The two science targets are limited to sidereal targets.  Dual non-sidereal and ToO targets are not supported.

* The concept of a base position to be used for slewing and guiding calculations remains.  The base position in dual target mode is the coordinate that is equidistant from the two targets.  In other words, the point in the middle of a line connecting the two targets.

* There is a minimum (84 arcseconds) and maximum (7 arcminutes) separation of the two targets.

* Future instruments after GHOST are also likely to support multiple targets, each with their own rules for calculating the base position and placement.

This document concerns updates to the OCS2 model(s) only.  The desire to find an ideal solution to this problem in the existing model is tempered by the complexity of updating 20 years worth of OCS code that assumes each observation is associated with a single science target.  Solutions for OCS2 should not limit how we model this feature in OCS3.

# Asterisms

The existing single-target mode can be seen as a degenerate case of a more general concept.  If we view the source as a collection of targets with a separate base position coordinate, then the single-target case is just a singleton instance with a matching base position. In general though the base position coordinate may not correspond to any of the targets in the collection. In fact, the GHOST dual-target mode specifically stipulates that the base position be the coordinate equidistant from the two targets.

We’ll call this concept an `Asterism`.  An `Asterism` has a `NonEmptyList[Target]` for some definition of `Target` and a base position coordinate.  Because targets are always moving, and because the base position coordinate may depend on one or more targets in the asterism, the base position is a function from time to `Coordinate` (just as for a `Target`’s coordinates).  

````scala
  def basePosition(when: Instant): Option[Coordinate]
````

The `Option` result is required to handle situations when we don’t have enough target information to estimate the position at the requested time.  For example, when no ephemeris data is available for non-sidereal targets.

By default the base position coordinate can be defined as the coordinate that minimizes the maximum distance to any target in the collection.  The “smallest-circle” algorithm runs in linear time and can be used to compute this value.  In the GHOST dual-target mode this computation is trivial.  Future instruments with multi-target modes might have different rules for computing the base position.

Throughout the OCS codebase we assume a single science-target per observation.  The idea now is to replace this target with an `Asterism`.  Usages that just want the coordinate at the base position will remain basically unchanged.  Usages that require target-specific information like magnitude values would need to figure out *which* target to use.

How the general `Asterism` concept might be expressed at Phase 1 vs. Phase 2 and beyond is the subject of the remainder of this document.

# Phase 1 Schema / Model / PIT

The Phase 1 observation is essentially a 4-tuple of

````scala
  (Target, Blueprint, Condition, Time)
````

The `Target` would need to be replaced with a simple `Asterism` element that contains only a non-empty list of science targets and the default base position calculation. A single base position is required for AGS checks, the ITAC queue algorithm, etc. Ideally the inclusion of two targets in an observation would be limited in the model to GHOST dual-target instrument resources. Unfortunately that seems difficult to achieve given the current Phase 1 schema, model, and user interface.  Instead, I suggest that we place constraints in the PIT UI where possible and add proposal checks that flag invalid configurations such as dual-target high-resolution observations or dual-target anything other than GHOST.

At phase 1 time, details of which IFU is assigned which target are presumed unimportant. Similarly, the location of sky positions, OIWFS guiding state, fiber agitator settings, etc. are either captured in the `Blueprint` or else deferred to Phase 2.

Asterisms will replace targets in the UI as well.  This will require a “Target Editor” popup with controls for displaying and editing multiple targets. 

## TODO

* How does this impact the “Targets” tab and target import / export, or does it impact these?
* What if a target appears in two or more asterisms?  Does it appear multiple times in the “Targets” tab?
* Do we need a UI for grouping targets imported from a file?
* Do we need to split asterisms if the user switches the instrument configuration from dual target to beam switching or high resolution?


# Phase 2 / Science Program Model

At Phase 2 and beyond, all the low-level configuration details must be specified and tracked.  This is challenging in the current model because the target environment is viewed as completely independent from the instrument configuration.  With GHOST, that separation may no longer be viable.  Depending on the mode there may be two targets, or one target and a sky position.  Further, because the two IFUs differ it is important to specify which particular IFU is assigned to a target or sky position.  GHOST configuration options depend heavily on the mode of use, so setting up to use the fiber agitator for standard resolution modes makes no sense for example.

The proposal here to to extend the `Asterism` concept to include configuration details for the three major use cases: dual target, beam switching, and high resolution.  Before diving into GHOST details though, we’ll sketch out the Phase 2 `Asterism` trait.

````scala
/** Collection of stars that make up the science target(s) for an observation, along with any
  * configuration details unique to the instrument in use.
  */
sealed trait Asterism {

  /** All targets that comprise the asterism.  There must be at least one target.
   */
  def targets: NonEmptyList[SPTarget]

  /** Computes the slew coordinates and AGS calculation base position based on the set of targets.
    */
  def basePosition(time: Instant): Option[Coordinates] = {
    // Solving the “smallest-circle problem”, a linear time algorithm, we can compute a default
    // base position that minimizes the maximum distance to any of n stars in general.  The
    // two-star case is just a simple application of the algorithm.
    //
    // Coordinate math in edu.gemini.skycalc.CoordinateDiff etc. should help with this.  If a
    // specific Asterism instance requires a different method for computing the base position,
    // it can provide a different definition than this default.
    ???
  }

  /** Analyzes the Asterism to identify validity issues wrt to star separation, brightness, etc.
    * An empty Set implies no issues.  We will likely need to distinguish “warning” vs. “error”
    * levels at least.
    *
    * TODO: maybe this doesn’t really belong here
    */
  def analysis: Set[(Level, String)]
    // Each type of Asterism defines rules for placement and magnitude, etc.  Limiting construction
    // of the Asterism to valid configurations only is problematic because SPTarget is mutable
    // and, at any rate, targets can be selected one at a time using the TPE.  This method admits
    // defeat and allows validation post-construction.


  // “Base position” convenience methods already in use extensively throughout the codebase.
  // Adding them here means we can get away with no code changes, or fewer code changes, in
  // many cases.
  //
  // Defined in terms of the basePosition.  These are methods on SPTarget for the most part
  // that are used directly in calls like targets.getBase().getRaDegrees(when)

  def getRaDegrees(time: Instant): Option[Double] =
    basePosition(time).map(_.ra.toDegrees)

  // … etc …
}
````

In the OT Target Environment and corresponding model code, the “base position” target will be replaced with an `Asterism` instance.

## Single Target Asterism

For all existing instruments, we use a simple `SingleTarget` degenerate `Asterism`.

````scala
final case class SingleTarget(target: SPTarget) extends Asterism {
  def targets: NonEmptyList[SPTarget] =
    NonEmptyList(target)

  def basePosition(time: Instant): Option[Coordinates] =
    target.coords(time) // default implementation would also work, but slightly less efficiently

  // What could go wrong?
  def analysis: Set[(Level, String)] =
    Set.empty
}
````

## GHOST-specific Asterisms

Sophisticated GHOST-specific `Asterism`s would be needed to handle dual-target mode and other instrument features. Two alternatives come to mind:

1. A generic `GhostAsterism` type that has fields for IFU1 and for IFU2.  These could be assigned science targets or sky coordinates as necessary.  Instrument features would be kept with the instrument itself.  For example, the observing mode (dual-target, beam-switching, or high-resolution) and mode-specific details would be edited in the instrument component.  Phase 2 checks would have to be utilized to determine whether the targets assigned in the Target Environment or TPE correspond to the configuration in the instrument.

2. Specific `Asterism` types that correspond to the three overarching GHOST modes: Two-Target Standard Resolution, Beam-Switching Standard Resolution, and High Resolution.  Instrument-specific details that correspond to each mode would be kept with the asterism itself.  This prevents some invalid configurations via the type-system instead of runtime checks.

We’ll cover each in turn.

### Option 1. Generic GhostAsterism

Using a generic `GhostAsterism`, nothing prevents us from building an `Asterism` that doesn’t match the instrument mode and configuration.

````scala
final case class GhostAsterism(
                   ifu1: Coordinate \/ SPTarget,
                   ifu2: Coordinate \/ SPTarget
                 ) extends Asterism {

  def targets: NonEmptyList[SPTarget] =
    (ifu1.toList ++ ifu2.toList) match {
       case Nil    => NonEmptyList(new SPTarget.Zero) // Misconfigured anyway
      case h :: t => NonEmptyList(h, t: _*)
    }
}
````

For example, two sky positions could be assigned or IFU2 could be used in high resolution mode.  We would have to settle for detection of these types of problems during Phase 2 checking.  On the plus side, the asterism is simple and most instrument specific details would be stored in the GHOST instrument component as for all other instruments.


### Option 2. Mode-specific GHOST Asterisms

In what follows I’ve attempted to sketch out the configuration information gleaned from the GHOST Concept of Operations Document.  The specific details are somewhat interesting, but the point is that instrument-specific configuration is being kept in the `Asterism` implementation itself.  Controls for setting these values could be available in the target component and/or TPE, or could be accessed from the GHOST instrument component leaving the target environment/TPE to the task of manipulating targets and sky positions.

Including instrument configuration in the target environment would be a fairly significant change in its own right beyond the updates to work with `Asterism` vs. a single base position target.  On the plus side, it keeps the configuration of each of the modes together with the target selection avoiding incorrect configurations like two-target high resolution.

#### Shared GHOST Properties

A few properties are common across the specific GHOST `Asterism` types.

````scala
/** The GHOST guide fibers can be enabled or disabled for each science target.  Typically they
  * are enabled for bright targets (< mag 18 for standard resolution, < mag 17 for high resolution)
  * but disabled for faint targets.  OIWFS state can also be explicitly disabled, say, in a crowded
  * field where OIWFS is less effective.
  */
sealed trait GhostOiwfsState

object GhostOiwfsState {
  case object Enabled  extends GhostOiwfs
  case object Disabled extends GhostOiwfs
}

/** GHOST targets will have a default guiding state based on magnitude band, but this can be
  * explicitly overridden.
  */
sealed trait GhostTarget {
  def target: SPTarget

  /** Desired value for OIWFS guiding.  If unset, guiding will default based on target magnitude.
    */
  def explicitOiwfs: Option[GhostOiwfsState]

  /** Determines the OIWFS guide state for this target.  If explicitly set, use the desired value.
    * Otherwise default according to the target B magnitude and resolution.
    */
  def oiwfs: GhostOiwfs =
    explicitOiwfs | defaultOiwfs // default according to guidingCutoff

  def defaultOiwfs: GhostOiwfs = {
    val cutoff = this match {
      case _: StandardResGhostTarget => StandardResCutoff
      case _: HighResGhostTarget.    => HighResCutoff
    }
    target.getMagnitude(MagnitudeBand.B).forall(_.value < cutoff.value) ? Enabled | Disabled
  }
}

object GhostTarget {
  val StandardResCutoff: Magnitude =
     Magnitude(18.0, MagnitudeBand.B, None, MagnitudeSystem.Vega)

  val HighResCutoff: Magnitude =
     Magnitude(17.0, MagnitudeBand.B, None, MagnitudeSystem.Vega)

  case class GhostStandardResTarget(
               target: SPTarget,
               explicitOiwfs: Option[GhostOiwfs]
             ) extends GhostTarget

  case class GhostHighResTarget(
               target: SPTarget,
               explicitOiwfs: Option[GhostOiwfs]
             ) extends GhostTarget
}

/** Identifies one of the two IFUs where the context does not make it clear which is in use.
  */
sealed trait GhostIfu 

object GhostIfu {
  case object GhostIfu1 extends GhostIfu
  case object GhostIfu2 extends GhostIfu
}

/** Base trait for the three GHOST Asterism types.
  */
sealed trait GhostAsterism extends Asterism {
  def xBinning: Int  // Should probably enumerate the possible values rather than use an Int
  def yBinning: Int
}
````

#### GHOST Standard Resolution Asterisms

There are two standard resolution modes, two-target and beam-switching.  In two-target mode both IFUs observe science targets and in beam-switching mode one IFU is used for the science target and the other for sky subtraction.  Either IFU1 or IFU2 can be used for the science target or for the sky position in beam-switching mode.

````scala
/** Binning options for standard resolution.  The exact detector binning values will
  * differ depending on whether in two-target or beam-switching modes.  Ideally these
  * would default based on target magnitude and not be user-controllable.  Is that
  * possible?
  */
sealed trait GhostStandardResolutionBinning

object GhostStandardResBinning {
  case object Bright    extends GhostStandardResBinning
  case object Faint     extends GhostStandardResBinning
  case object VeryFaint extends GhostStandardResBinning
}

case class GhostTwoTarget(
             bin:  GhostStandardResBinning, // maybe Option and default based on target magnitude?
             ifu1: GhostStandardResTarget,
             ifu2: GhostStandardResTarget
           ) extends GhostAsterism {

  def targets: NonEmptyList[SPTarget] =
    NonEmptyList(ifu1.target, ifu2.target)

  def xBinning: Int =
    bin match {
      case Bright | Faint => 1
      case VeryFaint      => 2
    }

  def yBinning: Int =
    bin match {
      case Bright            => 2
      case Faint | VeryFaint => 4
    }

  …  
}

case class GhostBeamSwitching(
             bin:       GhostStandardResBinning, // Option with default based on target magnitude?
             target:    GhostStandardResTarget,
             sky:       Coordinate,
             targetIfu: GhostIfu
           ) extends GhostAsterim {

  def targets: NonEmptyList[SPTarget] = 
    NonEmptyList(target._2.target)

  def skyIfu: GhostIfu =
    targetIfu === GhostIfu1 ? GhostIfu2 | GhostIfu1

  def xBinning: Int = …
  def yBinning: Int = …
  …
}
````

#### GHOST High Resolution Asterisms

High resolution modes are for single-targets but require additional configuration.

````scala
/** Binning options for high resolution.
  */
sealed trait GhostHighResBinning

object GhostHighResBinning {
  case object Bright extends GhostHighResBinning
  case object Faint  extends GhostHighResBinning
}

/** Fiber agitator state.
  */
sealed trait GhostFiberAgitatorState 

object GhostFiberAgitatorState {
  case object On  extends GhostFiberAgitatorState
  case object Off extends GhostFiberAgitatorState
}

sealed trait GhostHighResMode

object GhostHighResMode {
  case object OrdinaryHighRes                                                     extends GhostHighResMode
  case class PrecisionRadialVelocity(agitator: GhostFiberAgitatorState, cal: ???) extends GhostHighResMode
}


case class GhostHighResolution(
             bin:    GhostHighResBinning, // Option with default based on target magnitude?
             mode:   GhostHighResMode,
             target: GhostHighResTarget,  // always HRIFU1
             sky:    Offset               // don’t use SRIFU2
           ) extends GhostAsterism {

  def targets: NonEmptyList[SPTarget] =
    NonEmptyList(target.target)

  def xBinning: Int = …
  def yBinning: Int = …
  …
}
````

New instruments could introduce their own types of `Asterism` in the future. For example, one can imagine 1-_n_ star asterisms, asterisms with constraints on the max difference of target magnitude, etc.

