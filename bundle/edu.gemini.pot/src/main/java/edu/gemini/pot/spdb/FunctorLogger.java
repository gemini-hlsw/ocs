//
// $Id: FunctorLogger.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.pot.spdb;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logs functor start/finish and is used to track down problems in the database.
 */
final class FunctorLogger {
    private static final Logger LOG = Logger.getLogger("SpdbFunctorLogger");


    private static final String TIMEOUT_PROP                 = FunctorLogger.class.getName() + ".timeout";
    private static final String WARNING_THRESHOLD_PROP       = FunctorLogger.class.getName() + ".warningThreshold";
    private static final String QUERY_WARNING_THRESHOLD_PROP = FunctorLogger.class.getName() + ".queryWarningThreshold";

    private static final long DEFAULT_TIMEOUT                 = 60 * 1000;
    private static final long DEFAULT_WARNING_THRESHOLD       = 10 * 1000;
    private static final long DEFAULT_QUERY_WARNING_THRESHOLD = 30 * 1000;

    private static long _timeout               = -1;
    private static long _warningThreshold      = -1;
    private static long _queryWarningThreshold = -1;

    private static class FunctorWarning extends TimerTask {
        private String className;
        private String threadName;

        FunctorWarning(IDBFunctor functor) {
            className  = functor.getClass().getName();
            threadName = Thread.currentThread().getName();
        }

        public void run() {
            LOG.log(Level.WARNING, "Functor " + className + " has been running for an extended period on thread " + threadName);
        }
    }

    public static class Handback {
        private long startTime;
        private TimerTask task;
    }

    private static synchronized long getWarningThreshold() {
        if (_warningThreshold >= 0) return _warningThreshold;
        _warningThreshold = parseProp(WARNING_THRESHOLD_PROP,
                                               DEFAULT_WARNING_THRESHOLD);
        LOG.fine("Functor warning ms......: " + _warningThreshold);
        return _warningThreshold;
    }

    private static synchronized long getQueryWarningThreshold() {
        if (_queryWarningThreshold >= 0) return _queryWarningThreshold;
        _queryWarningThreshold = parseProp(QUERY_WARNING_THRESHOLD_PROP,
                                                    DEFAULT_QUERY_WARNING_THRESHOLD);
        LOG.fine("Query functor warning ms: " + _queryWarningThreshold);
        return _queryWarningThreshold;
    }

    private static synchronized long getTimeout() {
        if (_timeout >= 0) return _timeout;
        _timeout = parseProp(TIMEOUT_PROP, DEFAULT_TIMEOUT);
        LOG.fine("Functor timeout ms......: " + _timeout);
        return _timeout;
    }

    private static long parseProp(String propName, long defaultVal) {
        String propStr = System.getProperty(propName);
        if (propStr == null) return defaultVal;

        long longVal = defaultVal;
        try {
            longVal = Long.parseLong(propStr);
            if (longVal < 0) {
                LOG.warning("Value of property '" + propName +
                            "' was less than 0 (" + propStr +
                            ") using default value: " + defaultVal);
                longVal = defaultVal;
            }
        } catch (NumberFormatException ex) {
            LOG.warning("Could not parse value of property '" +
                        propName + "': " + propStr);
        }

        return longVal;
    }

    private final Timer functorTimer = new Timer("Functor Timer", true);

    Handback logStart(IDBFunctor functor) {
        return logStart(functor, false);
    }

    Handback logQueryStart(IDBFunctor functor) {
        return logStart(functor, true);
    }

    private Handback logStart(IDBFunctor functor, boolean query) {
        LOG.fine("Starting" + (query ? " query " : " ") + "functor " + functor.getClass().getName() + " on thread " + Thread.currentThread().getName());

        Handback hb = new Handback();
        hb.startTime = System.currentTimeMillis();
        hb.task      = new FunctorWarning(functor);

        functorTimer.schedule(hb.task, getTimeout());

        return hb;
    }

    void logEnd(IDBFunctor functor, Handback handback) {
        logEnd(functor, handback, false);
    }

    void logQueryEnd(IDBFunctor functor, Handback handback) {
        logEnd(functor, handback, true);
    }

    void logEnd(IDBFunctor functor, Handback handback, boolean query) {

        handback.task.cancel();

        long endTime = System.currentTimeMillis();
        long execTime = Math.max(endTime - handback.startTime, 0);
        Level level = Level.FINE;
        long threshold = query ? getQueryWarningThreshold() : getWarningThreshold();
        if (threshold <= execTime) level = Level.WARNING;

        LOG.log(level, "Finished" + (query ? " query " : " ") + " functor " + functor.getClass().getName() + " in " + execTime + " ms on thread " + Thread.currentThread().getName());
    }

    void cancel() {
        functorTimer.cancel();
    }
}
