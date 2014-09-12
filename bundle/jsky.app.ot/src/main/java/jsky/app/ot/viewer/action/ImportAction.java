package jsky.app.ot.viewer.action;

import jsky.app.ot.viewer.ImportWorker;
import jsky.app.ot.viewer.OpenUtils;
import jsky.app.ot.viewer.SPViewer;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * The ImportAction provides opening of an XML file and entry
 * to the database.
 */
public class ImportAction extends AbstractViewerAction {

    public ImportAction() {
        this(null);
    }

    public ImportAction(SPViewer viewer) {
        super(viewer, "Import XML...");
        setEnabled(true);
    }

    public void actionPerformed(final ActionEvent evt) {
        // parent component for file chooser
        final Component c = viewer;
        final File[] files = OpenUtils.getXmlFiles(c);
        if (files == null) {
            // This means cancel was pushed
            return;
        }

        importXmlFiles(files);
    }

    /**
     * Import the given XML files in a background thread.
     * <p/>
     * This method uses a background thread to import the XML files,
     * since the operation can be rather slow. A progress monitor window is
     * displayed while the files are being imported.
     */
    public void importXmlFiles(final File[] files) {
        if (files.length == 0) {
            return;
        }

        final ImportWorker worker = new ImportWorker(files, viewer);
        worker.start();
    }

    public boolean computeEnabledState() throws Exception {
        return true;
    }

}
