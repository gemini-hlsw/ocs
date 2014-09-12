package edu.gemini.p2checker.ags

import edu.gemini.ags.api.AgsGuideQuality._
import edu.gemini.ags.api.{AgsAnalysisWithGuideProbe, AgsAnalysis, AgsRegistrar, DefaultMagnitudeTable}
import edu.gemini.p2checker.api.{P2Problems, IP2Problems, Problem, ObservationElements, IRule}
import edu.gemini.p2checker.rules.general.GeneralRule
import edu.gemini.spModel.obs.{SPObservation, ObsClassService}
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.too.Too


class AgsAnalysisRule extends IRule {
  override def check(elements: ObservationElements): IP2Problems = {
    import AgsAnalysis._
    import AgsAnalysisRule._

    val problems = new P2Problems()
    val obsShell = elements.getObservationNode

    // We only report warnings and errors here if:
    // 1. This observation has a sequence with a science observe in it (GeneralRule.hasScienceObserves(elements.getSequence)) -- NO LONGER DONE
    // 2. We are not a day calibration observation, and
    // 3. The target is not a target of opportunity.
    if (SPObservation.needsGuideStar(obsShell)) {
      elements.getObsContext.asScalaOpt.map(ctx => {
        elements.getTargetObsComponentNode.asScalaOpt.map(targetNode => {
          // Perform the analysis.
          // TODO: Need to change the magnitude table here.
          val mt = DefaultMagnitudeTable(ctx)
          val analysis = AgsRegistrar.selectedStrategy(ctx).fold(List.empty[AgsAnalysis])(_.analyze(ctx, mt))

          // All analyses that are not DeliversRequestedIq in quality should lead to a warning or error.
          analysis.filterNot(_.qualityOption == Some(DeliversRequestedIq)).map { h =>
            new Problem(analysisProblemType(h), Prefix + "StrategyRule", analysisMessage(h), targetNode)
          }.map(problems.append)
        })
      })
    }
    problems
  }
}


object AgsAnalysisRule {
  val Prefix = "AgsAnalysisRule_"

  /**
   * Note that DeliversRequestedIq is already filtered out by this point, so we simply do not care
   * what Problem.Type is returned for it since this should never happen.
   */
  def analysisProblemType(analysis: AgsAnalysis): Problem.Type = analysis.qualityOption match {
      case Some(PossibleIqDegradation) | Some(IqDegradation) | Some(PossiblyUnusable) => Problem.Type.WARNING
      case _ => Problem.Type.ERROR
    }

  def analysisMessage(analysis: AgsAnalysis): String = (analysis match {
    case agp: AgsAnalysisWithGuideProbe => s"${agp.guideProbe.getKey}: "
    case _ => ""
  }) + analysis.message
}
