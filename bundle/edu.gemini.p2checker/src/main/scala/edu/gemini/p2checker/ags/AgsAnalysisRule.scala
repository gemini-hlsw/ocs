package edu.gemini.p2checker.ags

import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.ags.api.{AgsSeverity, AgsAnalysis, AgsRegistrar}
import edu.gemini.p2checker.api.{P2Problems, IP2Problems, Problem, ObservationElements, IRule}
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.rich.shared.immutable._


class AgsAnalysisRule(mt: MagnitudeTable) extends IRule {
  override def check(elements: ObservationElements): IP2Problems = {
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
          val analysis = AgsRegistrar.currentStrategy(ctx).fold(List.empty[AgsAnalysis])(_.analyze(ctx, mt))

          // All analyses that are not DeliversRequestedIq in quality should lead to a warning or error.
          // This equates to all analyses with a severity level.
          for {
            h <- analysis
            s <- h.severityLevel
          } yield {
            problems.append(new Problem(severityToProblemType(s), Prefix + "StrategyRule",
                            AgsAnalysis.analysisToMessage(h, showGuideProbeName = true), targetNode))
          }
        })
      })
    }
    problems
  }
}


object AgsAnalysisRule {
  val Prefix = "AgsAnalysisRule_"

  val severityToProblemType = Map[AgsSeverity,Problem.Type](
    AgsSeverity.Warning -> Problem.Type.WARNING,
    AgsSeverity.Error   -> Problem.Type.ERROR
  )
}
