package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable._
import edu.gemini.shared.{skyobject => SO}
import edu.gemini.shared.util.immutable.DefaultImList
import edu.gemini.spModel.core.{Coordinates, MagnitudeSystem, MagnitudeBand, Magnitude}
import edu.gemini.spModel.{target => SP}

import java.text.DateFormat
import scala.collection.JavaConverters._

object SpTargetFactory {

  def create(t: Target, time: Long): Either[String, SP.SPTarget] =
    t match {
      case too: TooTarget         => Right(createTooTarget(too))
      case nsd: NonSiderealTarget => createNonSiderealTarget(nsd, time)
      case sid: SiderealTarget    => createSiderealTarget(sid, time)
    }

  private def createTooTarget(too: TooTarget): SP.SPTarget = {
    val sp   = new SP.SPTarget(0.0, 0.0)
    sp.setName(too.name)
    sp
  }

  private def siderealCoordinates(t: Target, time: Long): Either[String, Coordinates] =
    t.coords(time).toRight("Target coordinates could not be determined on %s".format(formatTime(time)))

  private def nonSiderealCoordinates(nsid: NonSiderealTarget, time: Long): Either[String, (Coordinates, Long)] =
    closestCoordinates(nsid, time).toRight("Non-sidreal target coordinates missing ephemeris data.")

  private def closestCoordinates(nsid: NonSiderealTarget, time: Long): Option[(Coordinates, Long)] =
    closestEphemeris(nsid, time) map { e => (e.coords, e.validAt) }

  private def closestEphemeris(nsid: NonSiderealTarget, time: Long): Option[EphemerisElement] =
    nsid.ephemeris match {
      case Nil => None
      case lst => Some(lst minBy { e => (time - e.validAt).abs })
    }

  private def formatTime(time: Long): String =
    DateFormat.getDateTimeInstance.format(new java.util.Date(time))


  private def createNonSiderealTarget(nsid: NonSiderealTarget, time: Long): Either[String, SP.SPTarget] =
    ??? // TODO: P1 to P2
//    for {
//      coordsAt <- nonSiderealCoordinates(nsid, time).right
//    } yield {
//      val itarget = new SP.system.ConicTarget()
//      val (coords, when) = coordsAt
//      itarget.setDateForPosition(new java.util.Date(when))
//
//      val spTarget = new SP.SPTarget(itarget)
//      spTarget.setName(nsid.name)
//      setRaDec(spTarget, coords)
//
//      // Add apparent magnitude, if any.
//      nsid.magnitude(time)
//        .map(new SO.Magnitude(SO.Magnitude.Band.AP, _, MagnitudeSystem.AB))
//        .foreach(spTarget.putMagnitude)
//
//      spTarget
//    }

  private def createSiderealTarget(sid: SiderealTarget, time: Long): Either[String, SP.SPTarget] =
    ??? // TODO: P1 to P2
//    for {
//      coords <- siderealCoordinates(sid, time).right
//      mags   <- siderealMags(sid).right
//    } yield {
//      val itarget  = new SP.system.HmsDegTarget()
//      sid.properMotion.foreach { pm =>
//        val ra  = pm.deltaRA
//        val dec = pm.deltaDec
//        itarget.setPM1(new SPProperMotionRA(ra,   SP.system.CoordinateParam.Units.MILLI_ARCSECS_PER_YEAR))
//        itarget.setPM2(new SPProperMotionDec(dec, SP.system.CoordinateParam.Units.MILLI_ARCSECS_PER_YEAR))
//      }
//
//      val spTarget = new SP.SPTarget(itarget)
//      setRaDec(spTarget, coords)
//      spTarget.setName(sid.name)
//      spTarget.setMagnitudes(DefaultImList.create(mags.asJava))
//      spTarget
//    }

  private def setRaDec(spt: SP.SPTarget, c: Coordinates) {
    spt.setRaDecDegrees(c.ra.toAngle.toDegrees, c.dec.toDegrees)
  }

}
