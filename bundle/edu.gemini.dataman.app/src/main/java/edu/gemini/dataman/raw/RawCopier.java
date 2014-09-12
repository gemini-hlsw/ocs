//
// $Id: RawCopier.java 452 2006-06-27 21:25:47Z shane $
//

package edu.gemini.dataman.raw;

import edu.gemini.dataman.context.DatamanContext;
import edu.gemini.dataman.util.DatamanFileUtil;
import edu.gemini.dataman.util.DatamanLoggers;
import edu.gemini.datasetfile.DatasetFileName;
import edu.gemini.dirmon.DirEvent;
import edu.gemini.dirmon.DirListener;
import edu.gemini.dirmon.MonitoredDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copies files from the raw "pristine" storage area (eg,
 * <tt>/staging/perm</tt>) to the the working storage area.
 */
public final class RawCopier implements DirListener {
    private static final Logger LOG = Logger.getLogger(RawCopier.class.getName());

    private File _destDir;
    private DatamanContext _ctx;
    private MonitoredDir _rawMonitoredDir;
    private MonitoredDir _workMonitoredDir;

    /**
     * @param ctx contains configuration information, from which the working
     * storage area is obtained, and state information from/to which the last
     * directory modification times are read/written
     */
    public RawCopier(DatamanContext ctx) {
        _ctx = ctx;
        _destDir = ctx.getConfig().getWorkDir();
        if (_destDir == null) {
            throw new IllegalArgumentException("work dir not specified");
        }
    }

    /**
     * Records the MonitoredDir for the working storage area.
     */
    public synchronized void setWorkMonitoredDir(MonitoredDir dir) {
        LOG.fine("work monitored dir = " + dir);
        _workMonitoredDir = dir;
    }

    /**
     * Returns the MonitoredDir for the working storage area, if any.
     */
    public synchronized MonitoredDir getWorkMonitoredDir() {
        return _workMonitoredDir;
    }

    private void _notifyWorkingStoreDir(String fname) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Expect in working store: " + fname);
        }

//        try {
            MonitoredDir md = getWorkMonitoredDir();
            if (md != null) md.expectUpdates(fname);
//        } catch (RemoteException ex) {
            // this was only an optimization so just log a warning
//            LOG.log(Level.WARNING, "Remote exception calling monitored directory", ex);
//        }
    }

    /**
     * Upon initialization, copies all files from the raw storage are to the
     * working store that have been added or modified since the last time the
     * Dataman app ran.  Uses the <code>lastModified</code> time provided by
     * the {@link MonitoredDir} as the limit of files to discover.
     *
     * @param dir {@link MonitoredDir} that will supply @link DirEvent}s when
     * files are added/modified/removed
     *
     * @param lastModified current modification time of the MonitoredDir upon
     * adding the {@link RawCopier} as a listener; serves as a limit on the
     * events that could have been missed
     *
     * @throws RemoteException should never happen, since we are copying from
     * a mounted filesystem, not over the intranet
     */
    public void init(MonitoredDir dir, long lastModified) {
        long since = _ctx.getState().getRawLastModified();
        if ((since != -1) && (since < lastModified)) {
            Collection<File> files = dir.getModified(since, lastModified);
            for (File f : files) {
                _copyRawFile(f);
            }
        }

        // Remember when we last heard from the raw directory.
        _ctx.getState().increaseRawLastModified(lastModified);
        _setMonitoredDir(dir);
    }

    private synchronized void _setMonitoredDir(MonitoredDir monDir) {
        _rawMonitoredDir = monDir;
    }

    private synchronized MonitoredDir _getMonitoredDir() {
        return _rawMonitoredDir;
    }

    private void _expectNextDatasets(Collection<File> newFiles) {
        MonitoredDir monDir = _getMonitoredDir();
        if (monDir == null) return;
        if (newFiles.size() == 0) return;

        String lastName = "";
        for (File f : newFiles) {
            String curName = f.getName();
            if (curName.compareTo(lastName) > 0) {
                lastName = curName;
            }
        }
        DatasetFileName dfn = new DatasetFileName(lastName);
        if (!dfn.isValid()) return;

        Collection<String> res = new ArrayList<String>(5);
        for (int i=0; i<5; ++i) {
            dfn = dfn.getNext();
            res.add(dfn.toString());
        }

//        try {
            monDir.expectUpdates(res);
//        } catch (RemoteException ex) {
            // this was only an optimization so just log a warning
//            LOG.log(Level.WARNING, "Remote exception calling monitored directory", ex);
//        }
    }

    private static final long ONE_WEEK = 1000 * 60 * 60 * 24 * 7;

    /**
     * Handles a directory modification event on the raw/pristine FITS
     * directory.  Attempts to copy each new and modified file to the working
     * storage area.
     *
     * @param evt event describing the changes to the directory
     */
    public void dirModified(DirEvent evt) {
        long lastMod = _ctx.getState().getRawLastModified();
        lastMod = lastMod - ONE_WEEK;

        Collection<File> files = evt.getNewFiles();
        _expectNextDatasets(files);
        for (File f : files) {
            // filter out "new" files that are really old
            if (f.lastModified() < lastMod) continue;

            _copyRawFile(f);
        }
        files = evt.getModifiedFiles();
        for (File f : files) {
            _copyRawFile(f);
        }

        // Remember when we last heard from the raw directory.
        _ctx.getState().increaseRawLastModified(evt.getLastModified());
    }

    private void _copyRawFile(File f) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Copying raw file '" + f.getName() + "' to working storage.");
        }

        if (!f.getName().endsWith(".fits")) {
            LOG.info("Not copying raw file '" + f.getPath() +
                    "' to working storage because it is not a FITS file.");
            return; // skip non-fits files
        }

        String fileName = f.getName();

        // If the file already exists, don't overwrite, but do warn.
        File destFile = new File(_destDir, fileName);
        if (destFile.exists()) {
            String msg = "Dataset '" + f.getPath() +
              "' updated, but already exists in working storage.  Not copied.";
            DatamanLoggers.DATASET_PROBLEM_LOGGER.warning(msg);
            return;
        }

        // Copy the file over to the working directory.
        String tmpFileName  = fileName + ".tmp";
        File tmpFile = new File(_destDir, tmpFileName);
        // Keep doing the copy until an MD5 on both source and dest come
        // out the same.
        boolean success = false;
        for (int attempt=0; attempt<5; ++attempt) {
            if (_copyRawFileHelper(f, tmpFile, attempt)) {
                success = true;
                break;
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                break;
            }
        }

        if (!success) {
            tmpFile.delete();
            String msg = "Failed to copy raw file '" + f.getPath() +
                         "'.";
            DatamanLoggers.DATASET_PROBLEM_LOGGER.warning(msg);
            LOG.severe(msg);
            return;
        }

        // Move the .tmp file to the .fits file so that the dataset-file
        // code will pick it up and process it.
        tmpFile.renameTo(destFile);

        // Let the working storage directory monitor know.
        _notifyWorkingStoreDir(destFile.getName());

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Copied raw file '" + f.getName() + "' to working storage");
        }
    }

    private boolean _copyRawFileHelper(File src, File dest, int attempt) {
        byte[] srcMd5, destMd5;
        try {
            srcMd5 = DatamanFileUtil.md5(src);
            DatamanFileUtil.copyFile(src, dest);
            destMd5 = DatamanFileUtil.md5(dest);
            
        } catch (InterruptedException ex) {
            StringBuilder buf = new StringBuilder();
            buf.append('(').append(attempt);
            buf.append(") interrupted while copying raw file '");
            buf.append(src.getPath()).append("'.");
            LOG.log(Level.WARNING, buf.toString(), ex);
            return false;

        } catch (IOException ex) {
            StringBuilder buf = new StringBuilder();
            buf.append('(').append(attempt);
            buf.append(") I/O exception copying raw file '");
            buf.append(src.getPath()).append("'.");
            if (attempt == 0) {
                LOG.log(Level.WARNING, buf.toString(), ex);
            } else {
                LOG.warning(buf.toString());
            }
            return false;
        }

        if (!Arrays.equals(srcMd5, destMd5)) {
            StringBuilder buf = new StringBuilder();
            buf.append('(').append(attempt);
            buf.append(") MD5 mismatch.  Attempt to copy raw file '");
            buf.append(src.getPath()).append("' failed.");
            LOG.warning(buf.toString());
            return false;
        }

        return true;
    }
}
