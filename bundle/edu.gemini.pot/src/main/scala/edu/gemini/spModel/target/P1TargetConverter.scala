package edu.gemini.spModel.target

import edu.gemini.model.p1.immutable._
import edu.gemini.spModel.core

import scalaz._, Scalaz._
import edu.gemini.spModel.core.HorizonsDesignation

/**
 * Converts a P1 Target to an SPTarget.
 */
object P1TargetConverter {

  private val dra  = core.ProperMotion.deltaRA  >=> core.RightAscensionAngularVelocity.velocity >=> core.AngularVelocity.masPerYear
  private val ddec = core.ProperMotion.deltaDec >=> core.DeclinationAngularVelocity.velocity    >=> core.AngularVelocity.masPerYear

  def toSpTarget(s: core.Site, t: Target, time: Long): SPTarget =
    new SPTarget(t match {
        case too: TooTarget         => core.TooTarget(too.name)
        case nsd: NonSiderealTarget => initNonSidereal(s, nsd, time).exec(core.NonSiderealTarget.empty)
        case sid: SiderealTarget    => initSidereal(sid).exec(core.SiderealTarget.empty)
    })

  private def toCoreEphemeris(s: core.Site, e: List[EphemerisElement]): core.Ephemeris =
    core.Ephemeris(s, ==>>.fromList(e.map { case EphemerisElement(coords, _, time) => (time -> coords) }))

  private def apparentMag(d: Double): core.Magnitude =
    new core.Magnitude(d, core.MagnitudeBand.AP, core.MagnitudeSystem.AB)

  def coreProperMotion(pm: ProperMotion): core.ProperMotion =
    ((dra  := pm.deltaRA) *> (ddec := pm.deltaDec)).exec(core.ProperMotion.zero)

  private def initNonSidereal(s: core.Site, nsid: NonSiderealTarget, time: Long): State[core.NonSiderealTarget, Unit] =
    for {
      _ <- core.NonSiderealTarget.name       := nsid.name
      _ <- core.NonSiderealTarget.ephemeris  := toCoreEphemeris(s, nsid.ephemeris)
      _ <- core.NonSiderealTarget.magnitudes := nsid.magnitude(time).map(apparentMag).toList
      _ <- core.NonSiderealTarget.horizonsDesignation := nsid.horizonsDesignation.flatMap(HorizonsDesignation.read)
    } yield ()

  private def initSidereal(sid: SiderealTarget): State[core.SiderealTarget, Unit] =
    for {
      _ <- core.SiderealTarget.name         := sid.name
      _ <- core.SiderealTarget.coordinates  := sid.coords
      _ <- core.SiderealTarget.properMotion := sid.properMotion.map(coreProperMotion)
      _ <- core.SiderealTarget.magnitudes   := sid.magnitudes
    } yield ()

}
