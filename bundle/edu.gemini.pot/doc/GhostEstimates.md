# GHOST Estimations for High-Level Software

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

Throughout the OCS codebase we assume a single science-target per observation.  The idea now is to replace this target with an `Asterism`.  Usages that just want the coordinate at the base position will remain basically unchanged.  Usages that require target-specific information like magnitude values would need to figure out *which* target to use, or whether to take into account all targets.

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

# Phase 1 / PIT Tasks

### P1 - Create `Asterism`

Create the `Asterism` immutable class, schema definition, and smallest-circle base position calculation. (We’ll want to reuse the calculation in Phase 2 so it should be accessible.)


### P1 - Replace `Target` with `Asterism` in `Observation`

In this step the schema, model and UI are made to work with `Asterism`s instead of `Target` but we still have not introduced any controls for adding multiple targets.

* AGS, GSA Lookup: this should work with the base position.  The AGS client does send “target type” which could technically be a mix of two different types eventually.I think maybe if there is a non-sidereal target then the overall type should be non-sidereal. The type is just used by AGS to select the guiding strategy.  PWFS2 for example is usually used with non-sidereal presumably because it has a larger probe range for tracking a quickly moving target.

* Target visibility calculation should just work with the new definition of base position.

* Skeleton creation code has to be updated to make this compile.  Since there is no place for storing multiple targets in Phase 2, we should probably just blow up if there are multiple targets in the phase 1 data.  We will need to circle back to this later when there is Phase 2 support.

* Observation Editor has to be updated to show multiple targets when there are multiple targets.

* The grouping by conditions, instrument config, and target is obviously impacted by this. I’m not completely sure how this works but hopefully we can swap out the target for asterism without too much strife. In Phase 1, the assignment of targets to IFUs is not possible so an asterism (t1, t2) should be the same as (t2, t1) for the purposes of grouping.

* Target view deletion confirmation check will now have to check targets in asterisms in observations.

* Problem robot should be straightforward to extend to asterisms where each target is checked individually.

### P1 - GHOST Blueprints

The GHOST instrument blueprint and decision tree can be added to schema, model, and UI at this point. There is nothing new or particularly challenging in this task.  Since there is no Phase 2 representation for this yet, this should just be made to blow up at phase 2 skeleton creation time.

### P1 - Asterism Editor

The target editor will have to have become an asterism editor, gaining support for adding new targets to the asterism.

* Since most of the cases are single-target asterisms, we should avoid being too loud about this.  In fact, if we could avoid any change to the controls for observations that aren’t configured with GHOST dual-target mode that would be ideal.

* Of course, the user can change from dual-target to single-target GHOST modes after the fact and this should be indicated as a problem of some sort in the editor.

* The asterism editor should probably have a Lookup box for finding something in the target list.

* Given an asterism with multiple targets, there should probably be a “Split” button to create single-target asterisms of all the targets it contains.


### P1 - Problem Checking

There will be additional problems that need to be checked.

* Dual target asterisms are only used with GHOST in dual-target mode and their asterisms should have exactly two targets.

* The two targets in dual target mode should fall inside the IFU range limits.


# ITAC Tasks

My hope is that ITAC need not be drastically changed.  The queue engine should continue to work more or less the same since it only cares about the base position of the observation.

### ITAC - Track Model Changes

ITAC has its own Phase 1 model that will have to be updated to track the changes to the real phase 1 model. This may be nontrivial.

### ITAC - Reports Updates

* Instrument configuration reports
* Semester statistics report

### ITAC - Queue Engine Update

The queue engine just needs to extract the base position instead of the single target coordinates.  Otherwise it needs no changes.


# Phase 2 / OT Tasks

### P2 - `Asterism` trait, `SingleTargetAsterism` Implementation

See details in the proposal above.  Here we just create the new `Asterism` trait and implementation without hooking it up to anything.  This should be able to take advantage of the smallest-circle algorithm developed during the P1 implementation.

### P2 - GHOST Asterisms

See details in the proposal above.  Here we are defining much of the GHOST model. I’m assuming that “Option 2. Mode-specific GHOST Asterisms” is the way to proceed.

### P2 - Asterism PIO

Should use the `ParamSetCodec`.

### P2 - Replace `SPTarget` Base with `Asterism` in `TargetEnv`

This is a big task but here we will mostly concentrate on fairly automatic rote changes.

#### Base Coordinates

Many cases just need the base position coordinate, and that translates directly

* AGS works with the base position coordinates and shouldn’t have to change much.  The same consideration about sidereal vs. non-sidereal mixed asterisms applies.  I think the right answer is to consider the presence of even one non-sidereal target as counting as non-sidereal. That triggers PWFS to be preferred over any OIWFS.

* ITC needs the base position coordinate in one case, but this translates directly to the asterism base.  `TargetCalculator`, which it uses extensively, is created in terms of base position as well.

* Existing P2 checks look at the science target, but updates should be straightforward.

* Canopus probe ranges depend on distance from base but again this translates to asterism base.  (Of course Canopus isn’t used with dual-targets but in general we have to handle these cases.)

* QV / QPT extracts the RA/Dec of the target but that will become the base position RA/Dec.

* ITAC extracts “rollover” observation information from the ODB.  It uses this to pre-reserve time bins.  This should continue to work if adjusted to use base position coordinates for the asterism. 


#### Base Target Name

Several usages of the name of the target at the base position will have to be replaced.  Presumably a comma-separated list of target names will suffice.

* OT target component uses the name of the target by default.  Some combination of multiple target names can be used when necessary presumably.

* The FITS `OBJECT` header is taken from a sequence param derived from the base position target name.  Again, this presumably can be a comma separated list.

* QPT displays the target name which again should be a combination of all the targets.

* WDBA sends the base position “name” to the TCC.


#### TCC Config

TODO: It isn’t clear to me what to do in the TCC config.  There may be places where just the base coordinate is needed, and others where all targets have to be listed.  This may require coordination with TCC updates.  Input from Javier on the impact of asterisms would be appreciated.

#### OT Updates

The OT will require extensive updates to support asterisms completely.  I think this work should be relegated to a separate task.  For now, it should suffice to get the code compiling and working with the first target in the asterism as if it were the only target.  While going through the code, split up the places where we will need to match on asterism type.  Make careful notes in comments where we’ll need to come back later.

#### Other

* GPI looks at base position magnitudes to set parameters in the sequence.  I think we can update this to do the same thing for all targets in the asterism (of which there will only be one of course).

* `TargetEnvironment` PIO updates for `Asterism`.  

* The `edu.gemini.spModel.target.obsComp.TargetSelection` class should be reviewed thoroughly. There is some code there that assumes a single base position.  It should work just as well with multiple targets but will need to be updated.

* QPT adds markers for non-sidereal targets, which will now have to be checked over all the asterism targets.

* QV looks for non-sidereal targets

### P2 - GHOST Instrument Component

A simple GHOST component with parameters for

* Position angle
* ISS Port
* Red and Blue Camera exposure times


### P2 - OT GHOST Editor

I believe the OT editor should present the usual controls for position angle, port, exposure times.  It should also allow selection of the instrument mode, which is one of:

* Standard Resolution Dual-Target
* Standard Resolution Beam Switching
* High Resolution (with or without precision radial velocity (PRV))

Setting the instrument mode will also set the asterism in the target environment.  This implies the ability to morph from one asterism to another making sensible changes as necessary. Going between single target modes should be fairly straightforward.  

* Dual-target -> Single-target: prompt for which target to keep if they are both configured and not just the default (0,0) targets?  The other can be stored in a user target?

* Single-target -> Dual-target.  We could convert the sky position to a dummy target or make a dummy target at 84 arcsecs north of the single target.  It’s probably not too important since a real target will have to be selected.  It shouldn’t be (0,0) though because that will place the base position at some random place in the sky.

* The TPE and target environment will be used to set the actual target and coordinates of sky positions.  Other configuration details can be edited here.  For example whether to use the guide fibers, the binning setting, whether to use the fiber agitator for high-res PRV, etc.

### P2 - Skeleton Creation Updates

Given `Asterism` and GHOST support in the model, we can wrap up skeleton creation. The template creation library will need to be provided by science in the end, but we can get started with basic GHOST skeletons that match the phase 1 configuration.

### P2 - OT Updates For Asterisms

The Target Environment component and TPE should allow the selection of targets and sky positions.  I don’t think we need to be able to switch between asterism types here though. For that, see the GHOST instrument component editor.  Nevertheless, this will be the hardest task.

These components will need to match on the asterism type to configure themselves

* Single-target not GHOST.  Everything as today.

* Standard Resolution Dual-Target GHOST.  We need support editing two targets and for assigning the specific IFU for each. Should be able to swap between the two as well.  The TPE will need selectors for each IFU.

* Standard Resolution Beam-Switching GHOST. Support for entering a sky position and assigning it to an IFU.  Swap between IFU for the target and for the sky position. Support for showing these and selecting in TPE.

* High Resolution GHOST.  Much as for beam-switching mode but I believe here we need to define IFU2 as an offset position?

### P2 - AGS Updates

For the most part GHOST uses PWFS2 for guiding.  To make AGS effective though, we need to implement vignetting support for GHOST and the PWFS2 probe arm.

### P2 - OT Browser Support

Search capability.  Needs input from science.

### P2 - GHOST-specific Phase 2 Checks

* Error if two targets in dual-target mode are not far enough (at least 84 arcseconds) or else too far (7 arcmins or more) apart.

* Warn for explicit OIWFS guiding enabled on for dim targets.

* Error when using offset positions in dual-target mode

### P2 - GHOST overhead calculations

* Details from science TBD.







