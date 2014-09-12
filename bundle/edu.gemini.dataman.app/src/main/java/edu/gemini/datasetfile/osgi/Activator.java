//
// $Id: Activator.java 150 2005-09-27 12:36:41Z shane $
//

package edu.gemini.datasetfile.osgi;

import edu.gemini.datasetfile.DatasetFileService;
import edu.gemini.datasetfile.impl.DatasetFileServiceImpl;
import edu.gemini.datasetfile.impl.DatasetFileState;
import edu.gemini.dirmon.DirLocation;
import edu.gemini.dirmon.DirListener;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Hashtable;

/**
 *
 */
public class Activator implements BundleActivator {
    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    private static final String STATE_FILE_NAME = "datasetfilestate";
    private static final String WORKING_DIR_PROP = "edu.gemini.datasetfile.workDir";

    private DatasetFileListenerTracker _dsetFileTracker;

    public void start(BundleContext ctx) throws Exception {

        // Get the directory to watch.
        String workDirStr = ctx.getProperty(WORKING_DIR_PROP);
        if (workDirStr == null) {
            LOG.log(Level.SEVERE, "Missing property '" + WORKING_DIR_PROP + "'");
            throw new RuntimeException("Missing property '" + WORKING_DIR_PROP + "'");
        }
        File workDir = new File(workDirStr);

        File stateFile = ctx.getDataFile(STATE_FILE_NAME);
        DatasetFileState state = new DatasetFileState(stateFile);

        DatasetFileServiceImpl srv = new DatasetFileServiceImpl(workDir, state);

        // Register as a DatasetFileService.
        ctx.registerService(DatasetFileService.class.getName(), srv, null);

        // Register as a directory monitor.
        Hashtable<String,Object> props = new Hashtable<String,Object>();
        props.put(DirLocation.DIR_PATH_PROP, workDir.getPath());
        ctx.registerService(DirListener.class.getName(), srv, props);

        // Start watching for DatasetFileListeners
        _dsetFileTracker = new DatasetFileListenerTracker(ctx, srv);
        _dsetFileTracker.start();
    }

    public void stop(BundleContext ctx) throws Exception {
        _dsetFileTracker.stop();
    }
}
