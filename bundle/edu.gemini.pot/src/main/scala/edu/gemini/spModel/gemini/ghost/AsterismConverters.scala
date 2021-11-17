package edu.gemini.spModel.gemini.ghost

import edu.gemini.shared.util.immutable.ImList
import edu.gemini.spModel.core.SiderealTarget
import edu.gemini.spModel.target.{SPCoordinates, SPTarget}
import edu.gemini.spModel.target.env.{TargetEnvironment, UserTarget}

import scalaz._
import Scalaz._

/** Allow conversions between target environments containing the different GHOST asterism types.
  * Coordinates are immutable, but SPTargets are mutable, so we must clone them.
  */
object AsterismConverters {
  import GhostAsterism._

  type BasePosition = Option[SPCoordinates]
  type SkyPosition  = Option[SPCoordinates]

  sealed trait AsterismConverter {
    def name: String
    def convert(env: TargetEnvironment): Option[TargetEnvironment]
  }

  sealed trait GhostAsterismConverter extends AsterismConverter {
    override def convert(env: TargetEnvironment): Option[TargetEnvironment] =
      env.getAsterism match {
        case ga: GhostAsterism =>
          ga match {
            case SingleTarget(t, b)                   => creator(env, t,     None,   None, b).some
            case DualTarget(t1, t2, b)                => creator(env, t1, t2.some,   None, b).some
            case TargetPlusSky(t, s, b)               => creator(env, t,     None, s.some, b).some
            case SkyPlusTarget(s, t, b)               => creator(env, t,     None, s.some, b).some
            case HighResolutionTargetPlusSky(t, s, b) => creator(env, t,     None, s.some, b).some
          }
        case _                 =>
          None
      }

    /** This is just a method that takes the targets / coordinates common to all GHOST asterism types so that we can
      * more easily extract parameters from one asterism type and turn it into another without having to have n*n
      * cases, were n is the number of asterism types. */
    protected def creator(env: TargetEnvironment, t1: GhostTarget, t2: Option[GhostTarget], s: SkyPosition, b: BasePosition): TargetEnvironment
  }

  case object GhostSingleTargetConverter extends GhostAsterismConverter {
    override def name: String = "GhostAsterism.SingleTarget"

    override protected def creator(env: TargetEnvironment, t: GhostTarget, t2: Option[GhostTarget], s: SkyPosition, b: BasePosition): TargetEnvironment = {
      val asterism    = SingleTarget(t, b)
      val userTargets = appendCoords(appendTarget(env.getUserTargets, gT2UT(t2)), s)
      TargetEnvironment.createWithClonedTargets(asterism, env.getGuideEnvironment, userTargets)
    }
  }

  case object GhostDualTargetConverter extends GhostAsterismConverter {
    override def name: String = "GhostAsterism.DualTarget"

    override protected def creator(env: TargetEnvironment, t: GhostTarget, t2: Option[GhostTarget], s: SkyPosition, b: BasePosition): TargetEnvironment = {
      val asterism    = DualTarget(t, t2.getOrElse(GhostTarget.empty), b)
      val userTargets = appendCoords(env.getUserTargets, s)
      TargetEnvironment.createWithClonedTargets(asterism, env.getGuideEnvironment, userTargets)
    }
  }

  case object GhostTargetPlusSkyConverter extends GhostAsterismConverter {
    override def name: String = "GhostAsterism.TargetPlusSky"

    override protected def creator(env: TargetEnvironment, t: GhostTarget, t2: Option[GhostTarget], s: SkyPosition, b: BasePosition): TargetEnvironment = {
      val asterism    = TargetPlusSky(t, s.getOrElse(new SPCoordinates), b)
      val userTargets = appendTarget(env.getUserTargets, gT2UT(t2))
      TargetEnvironment.createWithClonedTargets(asterism, env.getGuideEnvironment, userTargets)
    }
  }

  case object GhostSkyPlusTargetConverter extends GhostAsterismConverter {
    override def name: String = "GhostAsterism.SkyPlusTarget"

    override protected def creator(env: TargetEnvironment, t: GhostTarget, t2: Option[GhostTarget], s: SkyPosition, b: BasePosition): TargetEnvironment = {
      val asterism    = SkyPlusTarget(s.getOrElse(new SPCoordinates), t, b)
      val userTargets = appendTarget(env.getUserTargets, gT2UT(t2))
      TargetEnvironment.createWithClonedTargets(asterism, env.getGuideEnvironment, userTargets)
    }
  }

  case object GhostHRTargetPlusSkyConverter extends GhostAsterismConverter {
    override def name: String = "GhostAsterism.HighResolutionTargetPlusSky"

    override protected def creator(env: TargetEnvironment, t: GhostTarget, t2: Option[GhostTarget], s: SkyPosition, b: BasePosition): TargetEnvironment = {
      val asterism    = HighResolutionTargetPlusSky(t, s.getOrElse(new SPCoordinates), b)
      val userTargets = appendTarget(env.getUserTargets, gT2UT(t2))
      TargetEnvironment.createWithClonedTargets(asterism, env.getGuideEnvironment, userTargets)
    }
  }


  private def appendCoords(userTargets: ImList[UserTarget], c: Option[SPCoordinates]): ImList[UserTarget] =
    appendTarget(userTargets, c.map(c2UT))

  private def appendTarget(userTargets: ImList[UserTarget], t: Option[UserTarget]): ImList[UserTarget] =
    t.map(userTargets.append).getOrElse(userTargets)

  /** Convert an SPTarget to a UserTarget. */
  private def t2UT(t: SPTarget): UserTarget =
    new UserTarget(UserTarget.Type.other, t)

  /** Convert a Coordinate to an empty UserTarget. */
  private def c2UT(c: SPCoordinates): UserTarget = {
    val t = SiderealTarget.coordinates.set(SiderealTarget.empty, c.getCoordinates)
    t2UT(new SPTarget(t))
  }

  /** Convert a GhostTarget to a UserTarget. */
  private def gT2UT(tOpt: Option[GhostTarget]): Option[UserTarget] =
    tOpt.map(t => t2UT(t.spTarget))
}
