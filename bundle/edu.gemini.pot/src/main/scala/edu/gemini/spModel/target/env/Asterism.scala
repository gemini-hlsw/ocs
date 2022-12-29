package edu.gemini.spModel.target.env

import edu.gemini.shared.util.immutable.{ImList, Option => GOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.skycalc.{Coordinates => SCoordinates}
import edu.gemini.spModel.core.{Coordinates, ProperMotion, SiderealTarget, Target}
import edu.gemini.spModel.gemini.ghost.{GhostAsterism, GhostParamSetCodecs}
import edu.gemini.spModel.target.{SPCoordinates, SPTarget, TargetParamSetCodecs}
import edu.gemini.spModel.pio.{ParamSet, Pio}
import edu.gemini.spModel.pio.codec.{MissingKey, ParamSetCodec, PioError, UnknownTag}
import edu.gemini.spModel.pio.xml.PioXmlFactory

import java.time.Instant
import scalaz._
import Scalaz._
import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GhostTarget
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GuideFiberState
import edu.gemini.spModel.gemini.ghost.GhostAsterism.PrvMode


/** Collection of stars that make up the science target(s) for an observation,
  * along with any configuration details unique to the instrument in use.
  */
trait Asterism {

  // invariant: allSpTargets.length == targets.fold(_ => 1, _ => 2)

  /** All SPTargets that comprise the asterism. */
  def allSpTargets: NonEmptyList[SPTarget]

  /** All SPTargets that comprise the asterism, as a Gemini ImList. */
  def allSpTargetsJava: ImList[SPTarget] =
    allSpTargets.list.toList.asImList

  /** All Targets that comprise the asterism. */
  def allTargets: NonEmptyList[Target]

  /** All SPCoordinates that comprise the asterism. */
  def allSpCoordinates: List[SPCoordinates]

  /** All SPCoordinates that comprise the asterism, as a Gemini ImList. */
  def allSpCoordinatesJava: ImList[SPCoordinates] =
    allSpCoordinates.asImList

  /** Slew coordinates and AGS calculation base position. */
  def basePosition(time: Option[Instant]): Option[Coordinates]

  /** Proper motion of the base position. */
  def basePositionProperMotion: Option[ProperMotion]

  /** Return a display name for this asterism. */
  def name: String =
    allTargets.map(_.name).intercalate(", ")

  /** True if all targets in the asterism are sidereal. */
  def allSidereal:    Boolean = allTargets.all(_.isSidereal)

  /** True if this asterism contains at least one non-sidereal target. */
  def hasNonSidereal: Boolean = allTargets.any(_.isNonSidereal)

  /** True if this asterism contains at least one TOO target. */
  def hasToo:         Boolean = allTargets.any(_.isToo)

  //
  // "Base position" convenience methods already in use extensively throughout
  // the codebase.  Defined in terms of the base position, these are methods
  // on SPTarget that are used directly in calls like
  // targets.getBase.getRaDegrees(when)
  //

  type GOLong   = GOption[java.lang.Long]
  type GODouble = GOption[java.lang.Double]

  private def gcoords[A](time: GOLong)(f: Coordinates => A): GOption[A] =
    basePosition(time.asScalaOpt.map(Instant.ofEpochMilli(_))).map(f).asGeminiOpt

  def getRaHours(time: GOLong): GODouble =
    gcoords(time)(_.ra.toHours)

  def getRaDegrees(time: GOLong): GODouble =
    gcoords(time)(_.ra.toDegrees)

  def getRaString(time: GOLong): GOption[String] =
    gcoords(time)(_.ra.toAngle.formatHMS)

  def getDecDegrees(time: GOLong): GODouble =
    gcoords(time)(_.dec.toDegrees)

  def getDecString(time: GOLong): GOption[String] =
    gcoords(time)(_.dec.formatDMS)

  def getSkycalcCoordinates(time: GOLong): GOption[SCoordinates] =
    gcoords(time)(cs => new SCoordinates(cs.ra.toDegrees, cs.dec.toDegrees))

  /** Construct a copy of this Asterism with cloned SPTargets (necessary because they are mutable). */
  def copyWithClonedTargets: Asterism

  // The mode of the resolultion: should be standard for everything but GHOST.
  def resolutionMode: ResolutionMode

  // Get the type of this asterism as an AsterismType.
  def asterismType: AsterismType
}

object Asterism {

  /** Construct a single-target Asterism with a [new] default "zero" SPTarget. */
  def zero: Asterism =
    Single(new SPTarget)

  // N.B. most members must be defs because `t` is mutable.
  final case class Single(t: SPTarget) extends Asterism {

    override def allSpTargets: NonEmptyList[SPTarget] =
      NonEmptyList(t) // def because Nel isn't serializable

    override def allTargets: NonEmptyList[Target] =
      NonEmptyList(t.getTarget)

    override def allSpCoordinates: List[SPCoordinates] =
      Nil

    override def basePosition(time: Option[Instant]): Option[Coordinates] =
      t.getCoordinates(time.map(_.toEpochMilli))

    override def copyWithClonedTargets: Asterism =
      Single(t.clone)

    override def basePositionProperMotion: Option[ProperMotion] =
      Target.pm.get(t.getTarget)

    override def resolutionMode: ResolutionMode =
      ResolutionMode.Standard

    override def asterismType: AsterismType =
      AsterismType.Single
  }

  object Single {
    import TargetParamSetCodecs._

    /** Construct a new "empty" Single with an "empty" target. */
    def empty: Single =
      apply(new SPTarget(SiderealTarget.empty))

    // Lenses
    val spTarget: Single @> SPTarget = Lens.lensu((a, b) => a.copy(t = b), _.t)
    val target:   Single @> Target   = spTarget.xmapB(_.getTarget)(new SPTarget(_))

    // We can't just .xmap the Target codec because Target is a tagged union and so is Asterism,
    // so we end up with two union tags if we do this. So we push the target paramset down a level.
    implicit val SingleParamSetCodec: ParamSetCodec[Single] =
      ParamSetCodec.initial(empty).withParamSet("target", target)
  }

  /** Construct a single-target Asterism by wrapping the given SPTarget. */
  def single(t: SPTarget): Asterism =
    Single(t)

  /** PIO codec for asterisms. */
  implicit val AsterismParamSetCodec: ParamSetCodec[Asterism] =
    new ParamSetCodec[Asterism] {
      val pf = new PioXmlFactory

      def encode(key: String, a: Asterism): ParamSet = {
        val tag = a.asterismType.tag
        val ps = a match {
          case a: Single                                    => Single.SingleParamSetCodec.encode(key, a)
          case a: GhostAsterism.SingleTarget                => GhostParamSetCodecs.SingleTargetParamSetCodec.encode(key, a)
          case a: GhostAsterism.DualTarget                  => GhostParamSetCodecs.DualTargetParamSetCodec.encode(key, a)
          case a: GhostAsterism.TargetPlusSky               => GhostParamSetCodecs.TargetPlusSkyParamSetCodec.encode(key, a)
          case a: GhostAsterism.SkyPlusTarget               => GhostParamSetCodecs.SkyPlusTargetParamSetCodec.encode(key, a)
          case a: GhostAsterism.HighResolutionTargetPlusSky => GhostParamSetCodecs.HRTargetPlusSkyParamSetCodec.encode(key, a)
        }
        Pio.addParam(pf, ps, "tag", tag)
        ps
      }

      def decode(ps: ParamSet): PioError \/ Asterism =
        (Option(ps.getParam("tag")).map(_.getValue) \/> MissingKey("tag")) flatMap {
          case AsterismType.Single.tag                           => Single.SingleParamSetCodec.decode(ps)
          case AsterismType.GhostSingleTarget.tag                => GhostParamSetCodecs.SingleTargetParamSetCodec.decode(ps)
          case AsterismType.GhostDualTarget.tag                  => GhostParamSetCodecs.DualTargetParamSetCodec.decode(ps)
          case AsterismType.GhostTargetPlusSky.tag               => GhostParamSetCodecs.TargetPlusSkyParamSetCodec.decode(ps)
          case AsterismType.GhostSkyPlusTarget.tag               => GhostParamSetCodecs.SkyPlusTargetParamSetCodec.decode(ps)
          case AsterismType.GhostHighResolutionTargetPlusSky.tag => GhostParamSetCodecs.HRTargetPlusSkyParamSetCodec.decode(ps)
          case other                                             => UnknownTag(other, "Asterism").left
        }
    }

  // Convenience create method for Java since trying to access nested objects and case
  // classes results cannot be resolved.
  def createSingleAsterism: Single = {
    Single(new SPTarget())
  }

  def fromTypeAndTemplateTarget(
    asterismType: AsterismType,
    target:       SPTarget
  ): Asterism = {

    def targetPlus(am: Int): SPTarget = {
      val res = new SPTarget()
      res.setTarget(
        Target.ra.mod(_.offset(Angle.fromArcmin(am.toDouble)), target.getTarget)
      )
      res
    }

    def coordinatesPlus(am: Int): SPCoordinates =
      target.getCoordinates(None).fold(new SPCoordinates()) { c =>
        new SPCoordinates(c.offset(Angle.fromArcmin(am.toDouble), Angle.zero))
      }

    def ghostTarget(sp: SPTarget): GhostTarget =
      GhostTarget(sp, GuideFiberState.Enabled)

    asterismType match {
      case AsterismType.Single                              =>
        Single(target)

      case AsterismType.GhostSingleTarget                   =>
        GhostAsterism.SingleTarget(ghostTarget(target), None)

      case AsterismType.GhostDualTarget                     =>
        GhostAsterism.DualTarget(
          ghostTarget(targetPlus(1)),
          ghostTarget(targetPlus(-1)),
          target.getCoordinates(None).map(new SPCoordinates(_))
        )

      case AsterismType.GhostTargetPlusSky                  =>
        GhostAsterism.TargetPlusSky(
          ghostTarget(target),
          coordinatesPlus(-2),
          None
        )

      case AsterismType.GhostSkyPlusTarget                  =>
        GhostAsterism.SkyPlusTarget(
          coordinatesPlus(2),
          ghostTarget(target),
          None
        )

      case AsterismType.GhostHighResolutionTargetPlusSky    =>
        GhostAsterism.HighResolutionTargetPlusSky(
          ghostTarget(target),
          coordinatesPlus(-2),
          PrvMode.PrvOff,
          None
        )

      case AsterismType.GhostHighResolutionTargetPlusSkyPrv =>
        GhostAsterism.HighResolutionTargetPlusSky(
          ghostTarget(target),
          coordinatesPlus(-2),
          PrvMode.PrvOn,
          None
        )
    }
  }
}
