package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable._
import edu.gemini.spModel.core
import edu.gemini.spModel.target.SPTarget

import scalaz._, Scalaz._

// P1 and P2 models are almost identical now
object SpTargetFactory {

  val dra  = core.ProperMotion.deltaRA  >=> core.RightAscensionAngularVelocity.velocity >=> core.AngularVelocity.masPerYear
  val ddec = core.ProperMotion.deltaDec >=> core.DeclinationAngularVelocity.velocity    >=> core.AngularVelocity.masPerYear

  def create(t: Target, time: Long): Either[String, SPTarget] = {
    val ct: core.Target =
      t match {
        case too: TooTarget         => core.TooTarget(too.name)
        case nsd: NonSiderealTarget => initNonSidereal(nsd, time).exec(core.NonSiderealTarget.empty)
        case sid: SiderealTarget    => initSidereal(sid, time).exec(core.SiderealTarget.empty)
      }
    Right(new SPTarget(ct))
  }

  private def toCoreEphemeris(e: List[EphemerisElement]): core.Ephemeris =
    ==>>.fromList(e.map { case EphemerisElement(coords, _, time) => (time -> coords) })

  private def apparentMag(d: Double): core.Magnitude =
    new core.Magnitude(d, core.MagnitudeBand.AP, core.MagnitudeSystem.AB)

  def coreProperMotion(pm: ProperMotion): core.ProperMotion =
    ((dra  := pm.deltaRA) *> (ddec := pm.deltaDec)).exec(core.ProperMotion.zero)

  private def initNonSidereal(nsid: NonSiderealTarget, time: Long): State[core.NonSiderealTarget, Unit] =
    for {
      _ <- core.NonSiderealTarget.name       := nsid.name
      _ <- core.NonSiderealTarget.ephemeris  := toCoreEphemeris(nsid.ephemeris)
      _ <- core.NonSiderealTarget.magnitudes := nsid.magnitude(time).map(apparentMag).toList
    } yield ()

  private def initSidereal(sid: SiderealTarget, time: Long): State[core.SiderealTarget, Unit] =
    for {
      _ <- core.SiderealTarget.name         := sid.name
      _ <- core.SiderealTarget.coordinates  := sid.coords
      _ <- core.SiderealTarget.properMotion := sid.properMotion.map(coreProperMotion)
      _ <- core.SiderealTarget.magnitudes   := sid.magnitudes
    } yield ()

}
