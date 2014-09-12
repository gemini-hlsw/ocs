//
// $Id: DatasetFileState.java 55 2005-08-31 18:50:14Z shane $
//

package edu.gemini.datasetfile.impl;

import java.io.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Simple Dataman configuration properties.
 */
public final class DatasetFileState {
    private static final Logger LOG = Logger.getLogger(DatasetFileState.class.getName());

    private static final String LAST_MODIFIED_PROP = "lastModified";

    private File _stateFile;
    private Properties _props;

    public DatasetFileState(File stateFile) throws IOException {
        _stateFile = stateFile;
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

    public synchronized long getLastModified() {
        String lastModifiedStr = _props.getProperty(LAST_MODIFIED_PROP, "-1");
        try {
            return Long.parseLong(lastModifiedStr);
        } catch (NumberFormatException ex) {
            LOG.log(Level.WARNING, "Could not parse '" + LAST_MODIFIED_PROP +
                                   "' as a long: " + lastModifiedStr);
            return -1;
        }
    }

    public synchronized void setLastModified(long time) {
        if (time < 0) throw new IllegalArgumentException("time = " + time);
        _props.setProperty(LAST_MODIFIED_PROP, String.valueOf(time));
        _updateState();
    }

    public synchronized void increaseLastModified(long time) {
        if (time > getLastModified()) {
            setLastModified(time);
        }
    }
}
