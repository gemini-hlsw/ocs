//
// $Id$
//

package edu.gemini.p2checker.util;

import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.P2Problems;

/**
 * Utility methods for P2 checking.
 */
public final class P2CheckerUtil {
    /**
     * An unmodifiable instance of an empty {@link IP2Problems} object.
     */
    public static IP2Problems NO_PROBLEMS = unmodifiableP2Problems(new P2Problems());

    private P2CheckerUtil() {
    }

    /**
     * Obtains an unmodifiable implementation of {@link IP2Problems} containing
     * the same problems as the <code>problems</code> arguemnt.  Note that if
     * the caller subsequently modifies <code>problems</code>, it will be visible
     * to users of the IP2Problems returned by this method.
     */
    public static IP2Problems unmodifiableP2Problems(IP2Problems problems) {
        if (problems instanceof UnmodifiableP2Problems) return problems;
        return new UnmodifiableP2Problems(problems);
    }
}
