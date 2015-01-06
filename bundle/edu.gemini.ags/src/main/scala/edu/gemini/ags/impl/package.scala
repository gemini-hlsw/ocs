package edu.gemini.ags

import edu.gemini.shared.util.immutable.PredicateOp
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.GuideProbeTargets
import edu.gemini.spModel.target.system.INonSiderealTarget

import edu.gemini.skycalc
import edu.gemini.shared.skyobject

import scalaz._
import Scalaz._

package object impl {

  implicit val oldAngle2New: skycalc.Angle => Angle = a => Angle.fromDegrees(a.toDegrees.getMagnitude)
  implicit val newAngle2Old: Angle => skycalc.Angle = a => skycalc.Angle.degrees(a.toDegrees)
  implicit val oldCoordinates2New: skycalc.Coordinates => Coordinates = c => Coordinates(RightAscension.fromAngle(c.getRa), Declination.fromAngle(c.getDec).getOrElse(Declination.zero))

  implicit val oldMagnitudeBand2New: skyobject.Magnitude.Band => MagnitudeBand = {
    case edu.gemini.shared.skyobject.Magnitude.Band.u => MagnitudeBand._u
    case edu.gemini.shared.skyobject.Magnitude.Band.g => MagnitudeBand._g
    case edu.gemini.shared.skyobject.Magnitude.Band.r => MagnitudeBand._r
    case edu.gemini.shared.skyobject.Magnitude.Band.i => MagnitudeBand._i
    case edu.gemini.shared.skyobject.Magnitude.Band.z => MagnitudeBand._z

    case edu.gemini.shared.skyobject.Magnitude.Band.U  => MagnitudeBand.U
    case edu.gemini.shared.skyobject.Magnitude.Band.B  => MagnitudeBand.B
    case edu.gemini.shared.skyobject.Magnitude.Band.V  => MagnitudeBand.V
    case edu.gemini.shared.skyobject.Magnitude.Band.UC => MagnitudeBand.UC
    case edu.gemini.shared.skyobject.Magnitude.Band.R  => MagnitudeBand.R
    case edu.gemini.shared.skyobject.Magnitude.Band.I  => MagnitudeBand.I
    case edu.gemini.shared.skyobject.Magnitude.Band.Y  => MagnitudeBand.Y
    case edu.gemini.shared.skyobject.Magnitude.Band.J  => MagnitudeBand.J
    case edu.gemini.shared.skyobject.Magnitude.Band.H  => MagnitudeBand.H
    case edu.gemini.shared.skyobject.Magnitude.Band.K  => MagnitudeBand.K
    case edu.gemini.shared.skyobject.Magnitude.Band.L  => MagnitudeBand.L
    case edu.gemini.shared.skyobject.Magnitude.Band.M  => MagnitudeBand.M
    case edu.gemini.shared.skyobject.Magnitude.Band.N  => MagnitudeBand.N
    case edu.gemini.shared.skyobject.Magnitude.Band.Q  => MagnitudeBand.Q
    case edu.gemini.shared.skyobject.Magnitude.Band.AP => MagnitudeBand.AP
  }
  implicit val newMagnitudeBand2Old: MagnitudeBand => skyobject.Magnitude.Band = {
    case MagnitudeBand._u => edu.gemini.shared.skyobject.Magnitude.Band.u
    case MagnitudeBand._g => edu.gemini.shared.skyobject.Magnitude.Band.g
    case MagnitudeBand._r => edu.gemini.shared.skyobject.Magnitude.Band.r
    case MagnitudeBand._i => edu.gemini.shared.skyobject.Magnitude.Band.i
    case MagnitudeBand._z => edu.gemini.shared.skyobject.Magnitude.Band.z

    case MagnitudeBand.U  => edu.gemini.shared.skyobject.Magnitude.Band.U
    case MagnitudeBand.B  => edu.gemini.shared.skyobject.Magnitude.Band.B
    case MagnitudeBand.G  => edu.gemini.shared.skyobject.Magnitude.Band.g
    case MagnitudeBand.V  => edu.gemini.shared.skyobject.Magnitude.Band.V
    case MagnitudeBand.UC => edu.gemini.shared.skyobject.Magnitude.Band.UC
    case MagnitudeBand.R  => edu.gemini.shared.skyobject.Magnitude.Band.R
    case MagnitudeBand.I  => edu.gemini.shared.skyobject.Magnitude.Band.I
    case MagnitudeBand.Z  => edu.gemini.shared.skyobject.Magnitude.Band.z
    case MagnitudeBand.Y  => edu.gemini.shared.skyobject.Magnitude.Band.Y
    case MagnitudeBand.J  => edu.gemini.shared.skyobject.Magnitude.Band.J
    case MagnitudeBand.H  => edu.gemini.shared.skyobject.Magnitude.Band.H
    case MagnitudeBand.K  => edu.gemini.shared.skyobject.Magnitude.Band.K
    case MagnitudeBand.L  => edu.gemini.shared.skyobject.Magnitude.Band.L
    case MagnitudeBand.M  => edu.gemini.shared.skyobject.Magnitude.Band.M
    case MagnitudeBand.N  => edu.gemini.shared.skyobject.Magnitude.Band.N
    case MagnitudeBand.Q  => edu.gemini.shared.skyobject.Magnitude.Band.Q

    case MagnitudeBand.AP => edu.gemini.shared.skyobject.Magnitude.Band.AP
  }

  implicit val oldMagnitude2New: skyobject.Magnitude => Magnitude = m => new Magnitude(m.getBrightness, m.getBand)
  implicit val newMagnitude2Old: Magnitude => skyobject.Magnitude = m => new skyobject.Magnitude(m.band, m.value)

  implicit val hmsDegCoords2Coordinates: skyobject.coords.HmsDegCoordinates => Coordinates = hmsDegCoords =>
    Coordinates(RightAscension.fromAngle(hmsDegCoords.getRa), Declination.fromAngle(hmsDegCoords.getDec).getOrElse(Declination.zero))

  implicit val siderealTarget2SkyObject:SiderealTarget => skyobject.SkyObject = s => {
    val ra          = skycalc.Angle.degrees(s.coordinates.ra.toAngle.toDegrees)
    val dec         = skycalc.Angle.degrees(s.coordinates.dec.toAngle.toDegrees)
    val coordinates = new skyobject.coords.HmsDegCoordinates.Builder(ra, dec).build()
    val mags        = s.magnitudes.map(newMagnitude2Old)
    new skyobject.SkyObject.Builder(s.name, coordinates).magnitudes(mags: _*).build()
  }

  implicit val skyObject2SiderealTarget:skyobject.SkyObject => SiderealTarget = s => {
    import scala.collection.JavaConverters._

    val ra          = Angle.fromDegrees(s.getHmsDegCoordinates.getRa.toDegrees.getMagnitude)
    val dec         = Angle.fromDegrees(s.getHmsDegCoordinates.getDec.toDegrees.getMagnitude)
    val coordinates = Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
    val mags        = s.getMagnitudes.asScala.map(oldMagnitude2New)
    SiderealTarget(s.getName, coordinates, Equinox.J2000, None, mags.toList, None)
  }

  def find(gpt: GuideProbeTargets, targetName: String): Option[SPTarget] =
    Option(targetName).map(_.trim).flatMap { tn =>
      gpt.getOptions.find(new PredicateOp[SPTarget] {
        def apply(spt: SPTarget): java.lang.Boolean =
          Option(spt.getName).map(_.trim).exists(_ == tn)
      }).asScalaOpt
    }

  def isSidereal(ctx: ObsContext): Boolean =
    !ctx.getTargets.getBase.getTarget.isInstanceOf[INonSiderealTarget]

  def isAo(ctx: ObsContext): Boolean = !ctx.getAOComponent.isEmpty

  def ctx180(c: ObsContext): ObsContext =
    c.withPositionAngle(c.getPositionAngle.add(180.0, skycalc.Angle.Unit.DEGREES))

  def brightness(so: SiderealTarget, b: MagnitudeBand): Option[Double] =
    so.magnitudeOn(b).map(_.value)

  def brightest[A](lst: List[A], band: MagnitudeBand)(toSiderealTarget: A => SiderealTarget): Option[A] = {
    lazy val max = new Magnitude(Double.MaxValue, band)
    if (lst.isEmpty) None
    else Some(lst.minBy(toSiderealTarget(_).magnitudeOn(band).getOrElse(max)))
  }

  def skyObjectFromScienceTarget(target: SPTarget): skyobject.SkyObject = {
    val name            = target.getName
    val coords          = target.getSkycalcCoordinates
    val skyObjectCoords = new skyobject.coords.HmsDegCoordinates.Builder(coords.getRa, coords.getDec).build
    val magnitudes      = target.getMagnitudes
    new skyobject.SkyObject.Builder(name, skyObjectCoords).build.withMagnitudes(magnitudes)
  }
}
