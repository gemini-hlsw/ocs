package edu.gemini.p2checker.ags

import edu.gemini.ags.api.AgsGuideQuality._
import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.ags.api.{AgsAnalysis, AgsRegistrar}
import edu.gemini.p2checker.api.{P2Problems, IP2Problems, Problem, ObservationElements, IRule}
import edu.gemini.spModel.gemini.gems.Canopus
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw
import edu.gemini.spModel.guide.GuideProbeGroup
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
      elements.getObsContext.asScalaOpt.foreach(ctx => {
        elements.getTargetObsComponentNode.asScalaOpt.foreach( targetNode => {
          // Perform the analysis.
          val analysis = AgsRegistrar.currentStrategy(ctx).fold(List.empty[AgsAnalysis])(_.analyze(ctx, mt))

          // Analyses that are not DeliversRequestedIq in quality should lead to a warning or error.
          // This equates to all analyses with a severity level.
          // We also omit ODGW and Canopus errors because these are already reported with different P2 messages.
          for {
            h <- analysis
            if (h match {
              case AgsAnalysis.NoGuideStarForGroup(group) => !ignoredProbeGroups.contains(group)
              case _                                      => true
            })
            s <- severity(h)
          } problems.append(new Problem(s, Prefix + "StrategyRule", h.message(withProbe = true), targetNode))
        })
      })
    }
    problems
  }
}

object AgsAnalysisRule {
  val Prefix = "AgsAnalysisRule_"

  // We want to ignore AgsAnalysis problems for ODGW and Canopus.
  val ignoredProbeGroups: Set[GuideProbeGroup] = Set(GsaoiOdgw.Group.instance, Canopus.Wfs.Group.instance)

  def severity(a: AgsAnalysis): Option[Problem.Type] =
    a.quality match {
      case DeliversRequestedIq   => None
      case PossibleIqDegradation => Some(Problem.Type.WARNING)
      case IqDegradation         => Some(Problem.Type.WARNING)
      case PossiblyUnusable      => Some(Problem.Type.WARNING)
      case Unusable              => Some(Problem.Type.ERROR)
    }

}
