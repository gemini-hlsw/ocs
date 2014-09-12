//
// $Id: OsgiDatamanState.java 129 2005-09-14 15:40:53Z shane $
//

package edu.gemini.dataman.osgi;

import edu.gemini.dataman.context.DatamanState;
import org.osgi.framework.BundleContext;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Simple Dataman configuration properties.
 */
final class OsgiDatamanState implements DatamanState {
    private static final Logger LOG = Logger.getLogger(OsgiDatamanState.class.getName());
    private static final String STATE_FILE_NAME = "datamanstate";

    private static final String RAW_LAST_MODIFIED_PROP  = "rawLastModified";
//    private static final String WORK_LAST_MODIFIED_PROP = "workLastModified";

    private File _stateFile;
    private Properties _props;

    OsgiDatamanState(BundleContext ctx) throws IOException {
        _stateFile = ctx.getDataFile(STATE_FILE_NAME);
        _props = new Properties();
        if (!_stateFile.exists()) return;

        FileInputStream fis = new FileInputStream(_stateFile);
        try {
            _props.load(fis);
        } finally {
            fis.close();
        }
    }

    private void _updateState() {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(_stateFile));
            _props.store(bos, "Dataman state information");
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Could not store state properties", ex);
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Could close state prop file", ex);
                }
            }
        }
    }

    public synchronized long getRawLastModified() {
        String lastModifiedStr = _props.getProperty(RAW_LAST_MODIFIED_PROP, "-1");
        try {
            return Long.parseLong(lastModifiedStr);
        } catch (NumberFormatException ex) {
            LOG.log(Level.WARNING, "Could not parse '" + RAW_LAST_MODIFIED_PROP +
                                   "' as a long: " + lastModifiedStr);
            return -1;
        }
    }

    public synchronized void setRawLastModified(long time) {
        if (time < 0) throw new IllegalArgumentException("time = " + time);
        _props.setProperty(RAW_LAST_MODIFIED_PROP, String.valueOf(time));
        _updateState();
    }

    public synchronized void increaseRawLastModified(long time) {
        if (time > getRawLastModified()) {
            setRawLastModified(time);
        }
    }

    /*
    public synchronized long getWorkLastModified() {
        String lastModifiedStr = _props.getProperty(WORK_LAST_MODIFIED_PROP, "-1");
        try {
            return Long.parseLong(lastModifiedStr);
        } catch (NumberFormatException ex) {
            LOG.log(Level.WARNING, "Could not parse '" + WORK_LAST_MODIFIED_PROP +
                                   "' as a long: " + lastModifiedStr);
            return -1;
        }
    }

    public synchronized void setWorkLastModified(long time) {
        if (time < 0) throw new IllegalArgumentException("time = " + time);
        _props.setProperty(WORK_LAST_MODIFIED_PROP, String.valueOf(time));
        _updateState();
    }

    public synchronized void increaseWorkLastModified(long time) {
        if (time > getWorkLastModified()) {
            setWorkLastModified(time);
        }
    }
    */
}
