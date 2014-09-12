//
// $Id: OsgiMonitoredDirConfig.java 244 2006-01-03 18:47:49Z shane $
//

package edu.gemini.dirmon.impl.osgi;

import edu.gemini.dirmon.impl.CompletionPolicy;
import edu.gemini.dirmon.impl.IdleTimeCompletionPolicy;
import edu.gemini.dirmon.impl.MonitoredDirConfig;
import edu.gemini.file.util.osgi.OsgiPatternFileFilterFactory;
import org.osgi.framework.BundleContext;

import java.io.FileFilter;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 */
public final class OsgiMonitoredDirConfig implements MonitoredDirConfig, Serializable {
    private static final Logger LOG = Logger.getLogger(OsgiMonitoredDirConfig.class.getName());

    static final String FULL_DIR_POLL_PERIOD_PROP   = "edu.gemini.dirmon.fullDirPollPeriod";

    static final String ACTIVE_SET_POLL_PERIOD_PROP = "edu.gemini.dirmon.activeSetPollPeriod";

    static final String ACTIVE_SET_SIZE_PROP        = "edu.gemini.dirmon.activeSetSize";

    static final String COMP_POLICY_PROP = "edu.gemini.dirmon.compPolicy";
    static final String IDLE_TIME_PROP   = "edu.gemini.dirmon.maxIdleTime";

    static final String INCLUDE_FILE_FILTER = "edu.gemini.dirmon.includes";
    static final String EXCLUDE_FILE_FILTER = "edu.gemini.dirmon.excludes";

    private long _fullPollPeriod   = DEFAULT_FULL_DIR_POLL_PERIOD_MS;
    private long _activePollPeriod = DEFAULT_ACTIVE_SET_POLL_PERIOD_MS;
    private int  _activeSetSize    = DEFAULT_ACTIVE_SET_SIZE;

    private CompletionPolicy _compPolicy = new IdleTimeCompletionPolicy();

    private FileFilter _fileFilter;

    public OsgiMonitoredDirConfig(BundleContext ctx) {
        // Get the full poll period to use.
        String propStr = ctx.getProperty(FULL_DIR_POLL_PERIOD_PROP);
        if (propStr != null) {
            try {
                _fullPollPeriod = Long.parseLong(propStr);
                if (_fullPollPeriod < 0) {
                    _fullPollPeriod = DEFAULT_FULL_DIR_POLL_PERIOD_MS;
                    LOG.warning("Cannot use a negative full dir poll period: " + propStr);
                } else {
                    LOG.info("Using fullDirPollPeriod=" + _fullPollPeriod);
                }
            } catch (NumberFormatException ex) {
                LOG.warning("Illegal value for full poll period: " + propStr);
            }
        }

        // Get the active set poll period to use.
        propStr = ctx.getProperty(ACTIVE_SET_POLL_PERIOD_PROP);
        if (propStr != null) {
            try {
                _activePollPeriod = Long.parseLong(propStr);
                if (_activePollPeriod < 0) {
                    _activePollPeriod = DEFAULT_ACTIVE_SET_POLL_PERIOD_MS;
                    LOG.warning("Cannot use a negative active set poll period: " + propStr);
                } else {
                    LOG.info("Using activeSetPollPeriod=" + _activePollPeriod);
                }
            } catch (NumberFormatException ex) {
                LOG.warning("Illegal value for active poll period: " + propStr);
            }
        }

        // Get the active set size to use.
        propStr = ctx.getProperty(ACTIVE_SET_SIZE_PROP);
        if (propStr != null) {
            try {
                _activeSetSize = Integer.parseInt(propStr);
                if (_activeSetSize < 0) {
                    _activeSetSize = DEFAULT_ACTIVE_SET_SIZE;
                    LOG.warning("Cannot use a negative active set size: " + propStr);
                } else {
                    LOG.info("Using activeSetSize=" + _activeSetSize);
                }
            } catch (NumberFormatException ex) {
                LOG.warning("Illegal value for active set size: " + propStr);
            }
        }

        // Get the max write idle time
        long maxIdle = IdleTimeCompletionPolicy.DEFAULT_MAX_WRITE_IDLE_TIME;
        propStr = ctx.getProperty(IDLE_TIME_PROP);
        if (propStr != null) {
            try {
                maxIdle = Long.parseLong(propStr);
            } catch (NumberFormatException ex) {
                LOG.warning("Illegal value for max idle time: " + propStr);
            }
        }

        // Get the completion policy to use... kind of lame
        propStr = ctx.getProperty(COMP_POLICY_PROP);
        if (propStr != null) {
            try {
                Class c = Class.forName(propStr);
                _compPolicy = (CompletionPolicy) c.newInstance();
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Could not instantiate: "  + propStr, ex);
            }
        }
        if (_compPolicy == null) {
            _compPolicy = new IdleTimeCompletionPolicy();
        }

        if (_compPolicy instanceof IdleTimeCompletionPolicy) {
            ((IdleTimeCompletionPolicy) _compPolicy).setMaxWriteIdleTime(maxIdle);
        }

        try {
            _fileFilter = createFileFilter(ctx);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not create the FileFilter", ex);
        }
    }

    private static FileFilter createFileFilter(BundleContext ctx) {
        return OsgiPatternFileFilterFactory.create(ctx);
    }

    public long getFullDirPollPeriod() {
        return _fullPollPeriod;
    }

    public long getActiveSetPollPeriod() {
        return _activePollPeriod;
    }

    public int getActiveSetSize() {
        return _activeSetSize;
    }

    public CompletionPolicy getCompletionPolicy() {
        return _compPolicy;
    }

    public FileFilter getFileFilter() {
        return _fileFilter;
    }
}
