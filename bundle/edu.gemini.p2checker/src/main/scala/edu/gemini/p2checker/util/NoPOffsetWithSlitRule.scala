package edu.gemini.p2checker.util

import edu.gemini.p2checker.api._
import edu.gemini.skycalc.Offset
import edu.gemini.spModel.config2.Config

/**
 * REL-1811: Warn if a slit spectroscopy observation uses a p-offset.
 */
class NoPOffsetWithSlitRule(prefix: String, predicate: (Config, ObservationElements) => java.lang.Boolean) extends IConfigRule {

  override def check(config: Config, step: Int, elems: ObservationElements, state: Object): Problem =
  {
    // unfortunately java callers expect null or Problem instead of Option<Problem>
    if (predicate(config, elems))
      SequenceRule.
        getPOffset(config).
        filterNot(Offset.isZero(_)).
        map(_ =>
          new Problem(
            IRule.WARNING,
            prefix + "NO_P_OFFSETS_WITH_SLIT_SPECTROSCOPY_RULE",
            NoPOffsetWithSlitRule.Message,
            SequenceRule.getInstrumentOrSequenceNode(step, elems))
        ).orNull
    else
      null
  }

  // this rule is to be applied on science observations and nighttime calibrations
  override def getMatcher: IConfigMatcher = SequenceRule.SCIENCE_NIGHTTIME_CAL_MATCHER

}

object NoPOffsetWithSlitRule {
  val Message = "P-offsets will move the slit off of the target."
}

