package edu.gemini.spModel.gemini.ghost

import edu.gemini.spModel.core.{Coordinates, Magnitude, MagnitudeBand, MagnitudeSystem, Offset}
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.Asterism

import java.time.{Duration, Instant}

import scalaz._
import Scalaz._

/** Base trait for the three GHOST asterism types: two target, beam switching,
  * and high resolution.
  */
sealed trait GhostAsterism extends Asterism {
  import GhostAsterism.{XBinning, YBinning}

  def xBinning: XBinning
  def yBinning: YBinning
}

/** GHOST asterism model.
  */
object GhostAsterism {

  /** The GHOST guide fibers can be enabled or disabled for each science target.
    * Typically they are enabled for bright targets (< B mag 18 for standard
    * resolution, < B mag 17 for high resolution) but disabled for faint targets.
    * Guide state can also be explicitly disabled, say, in a crowded field where
    * guide fibers are less effective.
    */
  sealed trait GuideFiberState extends Product with Serializable

  object GuideFiberState {
    case object Enabled  extends GuideFiberState
    case object Disabled extends GuideFiberState

    val enabled: GuideFiberState  = Enabled
    val disabled: GuideFiberState = Disabled

    val All = NonEmptyList(enabled, disabled)

    implicit val EqualGuideFiberState: Equal[GuideFiberState] =
      Equal.equalA[GuideFiberState]
  }


  /** GHOST targets are associated with a guiding state (enabled or disabled),
    * referring to whether the dedicated guide fibers surrounding the science
    * target should be used.
    *
    * There is a default guiding state based on magnitude, but this can be
    * explicitly overridden.
    */
  final case class GhostTarget(target: SPTarget,
                               explicitGuideFiberState: Option[GuideFiberState]) {

    def coordinates(when: Option[Instant]): Option[Coordinates] =
      target.getCoordinates(when.map(_.toEpochMilli))
  }

  object GhostTarget {

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
      t.target.getMagnitude(MagnitudeBand.B).forall(_.value < cutoff.value) ? GuideFiberState.enabled | GuideFiberState.disabled

    private def guideFiberState(t: GhostTarget, cutoff: Magnitude): GuideFiberState =
      t.explicitGuideFiberState | defaultGuideFiberState(t, cutoff)

    /** Computes the GuideFiberState for the given target in standard resolution mode. */
    def standardResGuideFiberState(t: GhostTarget): GuideFiberState =
      guideFiberState(t, StandardResCutoff)

    /** Computes the GuideFiberState for the given target in high resolution mode. */
    def highResGuideFiberState(t: GhostTarget): GuideFiberState =
      guideFiberState(t, HighResCutoff)
  }


  /** X-binning options. */
  sealed abstract class XBinning(val intValue: Int) extends Product with Serializable {
    override def toString: String =
      intValue.toString
  }

  object XBinning {
    case object One extends XBinning(1)
    case object Two extends XBinning(2)

    val one: XBinning = One
    val two: XBinning = Two

    val All = NonEmptyList(one, two)

    implicit val OrderXBinning: Order[XBinning] =
      Order.orderBy(_.intValue)
  }


  /** Y-binning options. */
  sealed abstract class YBinning(val intValue: Int) extends Product with Serializable {
    override def toString: String =
      intValue.toString
  }

  object YBinning {
    case object One   extends YBinning(1)
    case object Two   extends YBinning(2)
    case object Four  extends YBinning(4)
    case object Eight extends YBinning(8)

    val one: YBinning   = One
    val two: YBinning   = Two
    val four: YBinning  = Four
    val eight: YBinning = Eight

    val All = NonEmptyList(one, two, four, eight)

    implicit val OrderYBinning: Order[YBinning] =
      Order.orderBy(_.intValue)
  }


  /** Binning options for standard resolution.  The exact detector binning
    * values differ depending on whether in two-target or object+sky mode.
    * (TODO: can these be set automatically in terms of target magnitude? The
    *  ConOps doc lists faint and very faint both as starting at mag 18.)
    */
  sealed trait StandardResBinning extends Product with Serializable

  object StandardResBinning {
    case object Bright    extends StandardResBinning
    case object Faint     extends StandardResBinning
    case object VeryFaint extends StandardResBinning

    val bright: StandardResBinning    = Bright
    val faint: StandardResBinning     = Faint
    val veryFaint: StandardResBinning = VeryFaint

    val All = NonEmptyList(bright, faint, veryFaint)

    implicit val EqualStandardResBinning: Equal[StandardResBinning] =
      Equal.equalA[StandardResBinning]
  }

  /** GHOST two-target standard resolution asterism type.  In this mode, two
    * targets are observed simultaneously with both IFUs at standard resolution.
    *
    * The base position for the asterism defaults to the midway point between
    * the two targets, but may be explicitly specified if necessary to reach a
    * particular PWFS2 guide star.
    */
  final case class TwoTarget(
                     bin:  StandardResBinning,
                     ifu1: GhostTarget,
                     ifu2: GhostTarget,
                     base: Option[Coordinates]) extends GhostAsterism {

    /** Defines the targets in this asterism to be the two science targets. */
    override def targets: NonEmptyList[SPTarget] =
      NonEmptyList(ifu1.target, ifu2.target)

    /** Calculates the coordinates exactly halfway along the great circle
      * connecting the two targets.
      */
    def defaultBasePosition(when: Instant): Option[Coordinates] =
      for {
        c1 <- ifu1.coordinates(Some(when))
        c2 <- ifu2.coordinates(Some(when))
      } yield c1.interpolate(c2, 0.5)

    /** Obtains the base position, which defaults to the half-way point between
      * the two targets but may be explicitly specified instead.
      */
    override def basePosition(when: Instant): Option[Coordinates] =
      base orElse defaultBasePosition(when)

    import StandardResBinning._

    override def xBinning: XBinning =
      bin match {
        case Bright | Faint    => XBinning.One
        case VeryFaint         => XBinning.Two
      }

    override def yBinning: YBinning =
      bin match {
        case Bright            => YBinning.Two
        case Faint | VeryFaint => YBinning.Four
      }

    def ifu1GuideFiberState: GuideFiberState =
      GhostTarget.standardResGuideFiberState(ifu1)

    def ifu2GuideFiberState: GuideFiberState =
      GhostTarget.standardResGuideFiberState(ifu2)
  }


  /** GHOST beam switching standard resolution asterism type.  In this mode, a
    * target and sky position at a fixed coordinate are observed simultaneously
    * with both IFUs at standard resultion.  There are two sky positions and
    * the telescope switches IFU1 and IFU2 between the science object and the
    * sky positions.  By default, the second sky position is diametrically
    * opposed to the first position so that movement of the positioners can
    * be avoided in favor of pure telescope movement.  A different sky
    * position can be explicitly configured regardless if necessary.
    *
    * Since both IFUs observe the science object (at different times),
    * it is assumed that assigning a particular IFU to start with is not
    * necessary.
    */
  final case class BeamSwitching(
                     bin:          StandardResBinning,
                     target:       GhostTarget,
                     sky1:         Coordinates,
                     explicitSky2: Option[Coordinates]) extends GhostAsterism {

    /** Defines the target list to be the single standard resolution target. */
    override def targets: NonEmptyList[SPTarget] =
      NonEmptyList(target.target)

    /** Defines the base position to be the same as the target position. */
    override def basePosition(when: Instant): Option[Coordinates] =
      target.coordinates(Some(when))

    import StandardResBinning._

    override def xBinning: XBinning =
      bin match {
        case Bright | Faint    => XBinning.One
        case VeryFaint         => XBinning.Two
      }

    override def yBinning: YBinning =
      bin match {
        case Bright            => YBinning.Two
        case Faint | VeryFaint => YBinning.Eight
      }

    /** Deterimines the guide fiber state for the IFU observing the science
      * object. For the IFU observing a sky position, GuideFiberState is always
      * disabled.
      */
    def guideFiberState: GuideFiberState =
      GhostTarget.standardResGuideFiberState(target)

    // Calculate the diametrically opposed position, assuming we know where
    // the target is.
    private def defaultSky2(when: Instant): Option[Coordinates] =
      basePosition(when).map { bc =>
        val off = Coordinates.difference(bc, sky1).offset * -1
        bc.offset(off.p.toAngle, off.q.toAngle)
      }

    /** Gets the coordinates of the second sky position for beam-switching,
      * assuming it is explicitly provided or else we know where the science
      * target is at the given time.
      */
    def sky2(when: Instant): Option[Coordinates] =
      explicitSky2 orElse defaultSky2(when)
  }


  /** Binning options for high resolution.
    */
  sealed trait HighResBinning extends Product with Serializable

  object HighResBinning {
    case object Bright extends HighResBinning
    case object Faint  extends HighResBinning

    val bright: HighResBinning = Bright
    val faint: HighResBinning  = Faint

    val All = NonEmptyList(bright, faint)

    implicit val EqualHighResBinning: Equal[HighResBinning] =
      Equal.equalA[HighResBinning]
  }


  /** Fiber agitator state.
    */
  sealed trait FiberAgitatorState extends Product with Serializable

  object FiberAgitatorState {
    case object On  extends FiberAgitatorState
    case object Off extends FiberAgitatorState

    val on: FiberAgitatorState  = On
    val off: FiberAgitatorState = Off

    val All = NonEmptyList(on, off)

    implicit val EqualFiberAgitatorState: Equal[FiberAgitatorState] =
      Equal.equalA[FiberAgitatorState]
  }

  /** The high resolution asterism comes in two flavors, normal high resolution
    * mode and precision radial velocity (PRV) mode.  Normal high resolution
    * mode can use bright or faint detector binning, but PRV is always bright.
    * PRV can be set up with fiber agitation while normal high resolution mode
    * cannot.
    */
  sealed trait HighResMode extends Product with Serializable {
    import HighResMode._
    import HighResBinning._

    def xBinning: XBinning =
      XBinning.One

    def yBinning: YBinning =
      this match {
        case Normal(bin) =>
          bin match {
            case Bright => YBinning.One
            case Faint  => YBinning.Eight
          }

        case Prv(_, _)   =>
          YBinning.One
      }
  }

  object HighResMode {

    /** Normal high res mode can use bright or faint binning options. (Prv is
      * always bright).
      */
    final case class Normal(bin: HighResBinning) extends HighResMode

    /** Prv can be set up with a fiber agitator. The simultaneous calibration
      * lamp can default to an appropriate value for the target magnitude, or
      * can be explicitly set to a particular duration.
      *
      * (TODO: Do we need to be able to turn off the lamp altogether?  Perhaps
      * that is the same as setting the duration to 0.)
      */
    final case class Prv(agitator: FiberAgitatorState, calDuration: Option[Duration]) extends HighResMode
  }

  /** High resolution GHOST asterism.
    *
    * The target is always observed using the high resolution IFU1.  The sky
    * coordinates are observed using the sky fibers of IFU2, not SRIFU2. The
    * guide fibers will be used by default because the target must be bright,
    * but can be explicitly turned off.
    */
  final case class HighRes(
                     mode:   HighResMode,
                     target: GhostTarget,
                     sky:    Coordinates) extends GhostAsterism {

    /** Defines the target list to be the single high resolution target. */
    override def targets: NonEmptyList[SPTarget] =
      NonEmptyList(target.target)

    /** Defines the base position to be the same as the target position. */
    override def basePosition(when: Instant): Option[Coordinates] =
      target.coordinates(Some(when))

    override def xBinning: XBinning =
      mode.xBinning

    override def yBinning: YBinning =
      mode.yBinning

    /** Deterimines the guide fiber state for the HRIFU1.  Typically this will
      * be enabled since the target is bright but may be explicitly turned off.
      */
    def guideFiberState: GuideFiberState =
      GhostTarget.highResGuideFiberState(target)
  }
}
