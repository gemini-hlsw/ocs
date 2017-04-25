package edu.gemini.spModel.target.env

import edu.gemini.spModel.core.{Coordinates, SiderealTarget, Target}
import edu.gemini.spModel.target.SPTarget
import edu.gemini.shared.util.immutable.{ImList, Option => GOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.skycalc.{Coordinates => SCoordinates}
import java.time.Instant

import scalaz._
import Scalaz._

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
  def allTargets: NonEmptyList[Target] =
    targets.fold(NonEmptyList(_), p => NonEmptyList(p._1, p._2))

  /** An asterism is a single generic target, or a pair of sidereal targets. These are currently the
    * only possibilities.
    * TODO: should we Church encode this instead?
    */
  def targets: Target \/ (SiderealTarget, SiderealTarget)

  /** Slew coordinates and AGS calculation base position. */
  def basePosition(time: Option[Instant]): Option[Coordinates]

  /** Return a display name for this asterism. */
  def name: String

  /** True iff all targets are sidereal. */
  def isSidereal: Boolean =
    allSpTargets.all(_.isSidereal)

  /** True iff at least one target is non-sidereal. */
  def isNonSidereal: Boolean =
    allSpTargets.any(_.isNonSidereal)

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

}

object Asterism {

  // N.B. most members must be defs because `t` is mutable.
  final case class Single(t: SPTarget) extends Asterism {
    override val allSpTargets = NonEmptyList(t)
    override def targets = t.getTarget.left
    override def basePosition(time: Option[Instant]) = t.getCoordinates(time.map(_.toEpochMilli))
    override def name = t.getName
    override def copyWithClonedTargets() = Single(t.clone)
  }

  /** Construct a single-target Asterism by wrapping the given SPTarget. */
  def single(t: SPTarget): Asterism = Single(t)

}
