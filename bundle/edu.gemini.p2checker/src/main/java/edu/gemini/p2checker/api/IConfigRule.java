//
// $Id$
//

package edu.gemini.p2checker.api;

import edu.gemini.spModel.config2.Config;

/**
 * A specific rule to check for a particular configuration.  Typically an
 * instrument rule will be made up of a collection of IConfigRule.  The sequence
 * is checked one {@link Config} at a time, applying IConfigRules to each in
 * turn.
 */
public interface IConfigRule {

    /**
     * Checks the provided configuration for the problem(s) associated with this
     * rule.
     *
     * @param config complete configuration to be sent to the control system
     *
     * @param step the step count of the sequence to which the configuration
     * applies
     *
     * @param elems collection of nodes and data objects associated with the
     * observation
     *
     * @param state state information needed to apply rules that need to
     * examine multiple steps
     *
     * @return the problem if it applies to the given configuration
     */
    Problem check(Config config, int step, ObservationElements elems, Object state);


    IConfigMatcher getMatcher();
}
