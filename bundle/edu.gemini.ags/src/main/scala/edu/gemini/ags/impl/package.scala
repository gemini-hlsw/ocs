package edu.gemini.ags

import edu.gemini.catalog.api.{SaturationConstraint, FaintnessConstraint, MagnitudeConstraints, MagnitudeLimits}
import edu.gemini.catalog.api.MagnitudeLimits.{FaintnessLimit, SaturationLimit}
import edu.gemini.shared.util.immutable.PredicateOp
import edu.gemini.shared.util.immutable.ScalaConverters.ScalaOptionOps
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.GuideProbeTargets

import edu.gemini.skycalc
import edu.gemini.shared.skyobject
import edu.gemini.spModel.target.system.NonSiderealTarget

import scalaz._
import Scalaz._

package object impl {

  implicit class OldAngle2New(val angle: skycalc.Angle) extends AnyVal{
    def toNewModel: Angle = Angle.fromDegrees(angle.toDegrees.getMagnitude)
  }

  implicit class NewAngle2Old(val angle: Angle) extends AnyVal {
    def toOldModel: skycalc.Angle = skycalc.Angle.degrees(angle.toDegrees)
  }

  implicit class OldOffset2New(val offset: skycalc.Offset) extends AnyVal {
    def toNewModel: Offset = Offset(offset.p().toNewModel, offset.q().toNewModel)
  }

  implicit class OldCoordinates2New(val c: skycalc.Coordinates) extends AnyVal {
    def toNewModel: Coordinates = Coordinates(RightAscension.fromAngle(c.getRa.toNewModel), Declination.fromAngle(c.getDec.toNewModel).getOrElse(Declination.zero))
  }

  implicit class OldMagnitudeBand2New(val band: skyobject.Magnitude.Band) extends AnyVal {
    def toNewModel: MagnitudeBand = band match {
      case edu.gemini.shared.skyobject.Magnitude.Band.u  => MagnitudeBand._u
      case edu.gemini.shared.skyobject.Magnitude.Band.g  => MagnitudeBand._g
      case edu.gemini.shared.skyobject.Magnitude.Band.r  => MagnitudeBand._r
      case edu.gemini.shared.skyobject.Magnitude.Band.i  => MagnitudeBand._i
      case edu.gemini.shared.skyobject.Magnitude.Band.z  => MagnitudeBand._z

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
  }

  implicit class NewMagnitudeBand2Old(val band: MagnitudeBand) {
    def toOldModel: skyobject.Magnitude.Band = band match {
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
  }

  implicit class OldMagnitude2New(val m: skyobject.Magnitude) extends AnyVal {
    def toNewModel: Magnitude = new Magnitude(m.getBrightness, m.getBand.toNewModel)
  }

  implicit class NewMagnitude2Old(val m: Magnitude) extends AnyVal {
    def toOldModel: skyobject.Magnitude = new skyobject.Magnitude(m.band.toOldModel, m.value)
  }

  implicit class HmsDegCoords2Coordinates(val c: skyobject.coords.HmsDegCoordinates) extends AnyVal {
    def toNewModel: Coordinates = Coordinates(RightAscension.fromAngle(c.getRa.toNewModel), Declination.fromAngle(c.getDec.toNewModel).getOrElse(Declination.zero))
  }

  implicit class SiderealTarget2SkyObject(val st:SiderealTarget) extends AnyVal {
    def toOldModel: skyobject.SkyObject = {
      val ra          = skycalc.Angle.degrees(st.coordinates.ra.toAngle.toDegrees)
      val dec         = skycalc.Angle.degrees(st.coordinates.dec.toAngle.toDegrees)
      val coordinates = new skyobject.coords.HmsDegCoordinates.Builder(ra, dec).build()
      val mags        = st.magnitudes.map(_.toOldModel)
      new skyobject.SkyObject.Builder(st.name, coordinates).magnitudes(mags: _*).build()
    }
  }

  implicit class SkyObject2SiderealTarget(val so:skyobject.SkyObject) extends AnyVal {
    def toNewModel:SiderealTarget = {
      import scala.collection.JavaConverters._

      val ra          = Angle.fromDegrees(so.getHmsDegCoordinates.getRa.toDegrees.getMagnitude)
      val dec         = Angle.fromDegrees(so.getHmsDegCoordinates.getDec.toDegrees.getMagnitude)
      val coordinates = Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
      val mags        = so.getMagnitudes.asScala.map(_.toNewModel)
      SiderealTarget(so.getName, coordinates, None, mags.toList, None)
    }
  }

  implicit class SPTarget2SiderealTarget(val sp:SPTarget) extends AnyVal {
    def toNewModel:SiderealTarget = {
      val name        = sp.getTarget.getName
      val coords      = sp.getTarget.getSkycalcCoordinates
      val mags        = sp.getTarget.getMagnitudes.asScalaList.map(_.toNewModel)
      val ra          = Angle.fromDegrees(coords.getRaDeg)
      val dec         = Angle.fromDegrees(coords.getDecDeg)
      val coordinates = Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
      SiderealTarget(name, coordinates, None, mags, None)
    }
  }

  // REMOVE When AGS is fully ported
  @Deprecated
  implicit class MagnitudeConstraints2MagnitudeLimits(val mc: MagnitudeConstraints) extends AnyVal {

    def toMagnitudeLimits = {
      val saturation: Option[SaturationLimit] = mc.saturationConstraint.map(s => new SaturationLimit(s.brightness))
      new MagnitudeLimits(mc.band.toOldModel, new FaintnessLimit(mc.faintnessConstraint.brightness), new ScalaOptionOps(saturation).asGeminiOpt)
    }
  }

  @Deprecated
  implicit class MagnitudeLimits2MagnitudeConstraints(val ml: MagnitudeLimits) extends AnyVal {

    def toMagnitudeConstraints = {
      MagnitudeConstraints(ml.getBand.toNewModel, FaintnessConstraint(ml.getFaintnessLimit.getBrightness), ml.getSaturationLimit.asScalaOpt.map(s => SaturationConstraint(s.getBrightness)))
    }
  }

  def find(gpt: GuideProbeTargets, targetName: String): Option[SPTarget] =
    Option(targetName).map(_.trim).flatMap { tn =>
      gpt.getOptions.find(new PredicateOp[SPTarget] {
        def apply(spt: SPTarget): java.lang.Boolean =
          Option(spt.getTarget.getName).map(_.trim).exists(_ == tn)
      }).asScalaOpt
    }

  def isSidereal(ctx: ObsContext): Boolean =
    !ctx.getTargets.getBase.getTarget.isInstanceOf[NonSiderealTarget]

  def isAo(ctx: ObsContext): Boolean = !ctx.getAOComponent.isEmpty

  def ctx180(c: ObsContext): ObsContext =
    c.withPositionAngle(c.getPositionAngle.add(180.0, skycalc.Angle.Unit.DEGREES))

  def brightness(so: SiderealTarget, b: MagnitudeBand): Option[Double] =
    so.magnitudeIn(b).map(_.value)

  def brightest[A](lst: List[A], band: MagnitudeBand)(toSiderealTarget: A => SiderealTarget): Option[A] = {
    lazy val max = new Magnitude(Double.MaxValue, band)
    if (lst.isEmpty) None
    else Some(lst.minBy(toSiderealTarget(_).magnitudeIn(band).getOrElse(max)))
  }

}
