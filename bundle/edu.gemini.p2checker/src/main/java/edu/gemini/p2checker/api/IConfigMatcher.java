package edu.gemini.p2checker.api;

import edu.gemini.spModel.config2.Config;

public interface IConfigMatcher {
    boolean matches(Config config, int step, ObservationElements elems);
    IConfigMatcher ALWAYS = (config, step, elems) -> true;
}
