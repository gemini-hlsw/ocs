package edu.gemini.p2checker.util;

import edu.gemini.p2checker.api.IConfigRule;
import edu.gemini.p2checker.api.IConfigMatcher;

/**
 *
 */
public abstract class AbstractConfigRule implements IConfigRule {
    public IConfigMatcher getMatcher() {
        return null;
    }
}
