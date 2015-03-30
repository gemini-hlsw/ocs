package edu.gemini.ags

import edu.gemini.catalog.api.{SaturationConstraint, FaintnessConstraint, MagnitudeConstraints, MagnitudeLimits}
import edu.gemini.catalog.api.MagnitudeLimits.{FaintnessLimit, SaturationLimit}
import edu.gemini.shared.util.immutable.PredicateOp
import edu.gemini.shared.util.immutable.ScalaConverters.ScalaOptionOps
import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.GuideProbeTargets

import edu.gemini.skycalc
import edu.gemini.spModel.target.system.NonSiderealTarget

import scalaz._
import Scalaz._

package object impl {

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
