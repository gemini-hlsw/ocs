package edu.gemini.ags.api

import edu.gemini.ags.api.AgsAnalysis.NotReachable
import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.core.{MagnitudeBand, Angle}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.guide.{ValidatableGuideProbe, GuideProbe}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.shared.util.immutable.{Option => JOption, Some => JSome}
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.{OptionsList, GuideProbeTargets, TargetEnvironment}
import edu.gemini.spModel.target.system.HmsDegTarget

import scala.collection.JavaConverters._
import scala.concurrent.Future

trait AgsStrategy {
  def key: AgsStrategyKey

  def magnitudes(ctx: ObsContext, mt: MagnitudeTable): List[(GuideProbe, MagnitudeCalc)]

  def analyze(ctx: ObsContext, mt: MagnitudeTable): List[AgsAnalysis]

  def analyze(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis]

  def analyzeForJava(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): JOption[AgsAnalysis] = {
    val spTarget = new SPTarget(guideStar.coordinates.ra.toAngle.toDegrees, guideStar.coordinates.dec.toDegrees)
    if (!guideProbe.validate(spTarget, ctx)) new JSome(NotReachable(guideProbe, guideStar, probeBands))
    else analyze(ctx, mt, guideProbe, guideStar).asGeminiOpt
  }

  def candidates(ctx: ObsContext, mt: MagnitudeTable): Future[List[(GuideProbe, List[SiderealTarget])]]

  def estimate(ctx: ObsContext, mt: MagnitudeTable): Future[AgsStrategy.Estimate]

  def select(ctx: ObsContext, mt: MagnitudeTable): Future[Option[AgsStrategy.Selection]]

  def guideProbes: List[GuideProbe]

  // Return the bands used by the strategy in order
  def probeBands: List[MagnitudeBand]
}

object AgsStrategy {
  object Estimate {
    val CompleteFailure   = Estimate(0.0)
    val GuaranteedSuccess = Estimate(1.0)

    def toEstimate(probability: Double): Estimate =
      Estimate(probability).normalize
  }

  /**
   * Estimation of success of finding a guide star at phase 2 time.
   */
  case class Estimate(probability: Double) extends AnyVal {
    def normalize: Estimate =
      if (probability <= 0) Estimate.CompleteFailure
      else if (probability >= 1) Estimate.GuaranteedSuccess
      else this
  }

  /**
   * An assignment of a guide star to a particular guide probe.
   */
  case class Assignment(guideProbe: GuideProbe, guideStar: SiderealTarget)

  /**
   * Results of running an AGS selection.  The position angle for which the
   * results are valid along with all assignments of guide probes to stars.
   */
  case class Selection(posAngle: Angle, assignments: List[Assignment]) {

    /**
     * Creates a new TargetEnvironment with guide stars for each assignment in
     * the Selection.
     */
    def applyTo(env: TargetEnvironment): TargetEnvironment = {
      def findMatching(gpt: GuideProbeTargets, target: SPTarget): Option[SPTarget] =
        Option(target.getTarget.getName).flatMap { n =>
          gpt.getOptions.toList.asScala.find { t =>
            Option(t.getTarget.getName).map(_.trim).exists(_ == n.trim)
          }
        }

      (env /: assignments) { (curEnv, ass) =>
        val target = new SPTarget(HmsDegTarget.fromSkyObject(ass.guideStar.toOldModel))
        val oldGpt = curEnv.getPrimaryGuideProbeTargets(ass.guideProbe).asScalaOpt

        val newGpt = oldGpt.fold(GuideProbeTargets.create(ass.guideProbe, target)) { gpt =>
          // We already have guide probe targets for guide probe.
          // Does one with the same target name already exist? If so, mark it as
          // primary and replace it with the target we just made.  If not, add the
          // target and mark it as primary.
          findMatching(gpt, target).fold(gpt.update(OptionsList.UpdateOps.appendAsPrimary(target))) { existing =>
            gpt.selectPrimary(existing).setPrimary(target)
          }
        }

        curEnv.putPrimaryGuideProbeTargets(newGpt)
      }
    }
  }
}
