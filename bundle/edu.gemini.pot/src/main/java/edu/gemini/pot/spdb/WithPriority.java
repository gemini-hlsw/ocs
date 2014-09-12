//
// $
//

package edu.gemini.pot.spdb;

import edu.gemini.pot.spdb.IDBFunctor.Priority;

/**
 * Wraps a functor execution runnable with a thread priority reset.
 */
final class WithPriority {
    private WithPriority() {}

    private static int toJavaPriority(Priority p) {
        switch (p) {
            case medium: return Thread.NORM_PRIORITY - 1;
            case low   : return Thread.NORM_PRIORITY - 2;
            default    : return Thread.NORM_PRIORITY;
        }
    }

    static void exec(Priority p, Runnable r) {
        final Thread cur      = Thread.currentThread();
        final int newPriority = toJavaPriority(p);
        final int oldPriority = cur.getPriority();

        cur.setPriority(newPriority);
        try {
            r.run();
        } finally {
            cur.setPriority(oldPriority);
        }

    }
}
