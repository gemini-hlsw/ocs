package edu.gemini.p2checker.rules.visitor

import edu.gemini.p2checker.api.{IP2Problems, ObservationElements, IRule}

// Empty rule set for Visitor Instrument. This is required to force the instrument to be recognized in P2Checker
// and to perform the composite rules including the AgsAnalysisRule (as per REL-2197).
class VisitorRule extends IRule {
  override def check(elements: ObservationElements): IP2Problems = null
}
