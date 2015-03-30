package edu.gemini.ags

import edu.gemini.catalog.api.{SaturationConstraint, FaintnessConstraint, MagnitudeConstraints, MagnitudeLimits}
import edu.gemini.catalog.api.MagnitudeLimits.{FaintnessLimit, SaturationLimit}
import edu.gemini.shared.util.immutable.PredicateOp
import edu.gemini.shared.util.immutable.ScalaConverters.ScalaOptionOps
import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.core._
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

  implicit class OldMagnitude2New(val m: skyobject.Magnitude) extends AnyVal {
    def toNewModel: Magnitude = new Magnitude(m.getBrightness, m.getBand.toNewModel)
  }

  implicit class NewMagnitude2Old(val m: Magnitude) extends AnyVal {
    def toOldModel: skyobject.Magnitude = new skyobject.Magnitude(m.band.toOldModel, m.value)
  }

  implicit class HmsDegCoords2Coordinates(val c: skyobject.coords.HmsDegCoordinates) extends AnyVal {
    def toNewModel: Coordinates = Coordinates(RightAscension.fromAngle(c.getRa.toNewModel), Declination.fromAngle(c.getDec.toNewModel).getOrElse(Declination.zero))
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

}
