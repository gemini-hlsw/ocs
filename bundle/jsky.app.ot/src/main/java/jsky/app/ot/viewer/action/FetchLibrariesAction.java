package jsky.app.ot.viewer.action;

import jsky.app.ot.scilib.ScienceLibraryFetcher;
import jsky.app.ot.viewer.SPViewer;

import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * The FetchLibrariesAction handles fetching Science Libraries from the online
 * database, for different instruments.
 */
public final class FetchLibrariesAction extends AbstractViewerAction {

    public FetchLibrariesAction(final SPViewer viewer) {
        super(viewer, "Fetch Libraries...");
    }

    public boolean computeEnabledState() throws Exception {
        return true;
    }

    public void actionPerformed(final ActionEvent event) {
        //get the parent window, if any
        Frame parent = null;
        if (viewer != null) parent = viewer.getParentFrame();
        //Ask the user what libraries to fetch. Will update the existing ones
        ScienceLibraryFetcher.fetchLibraries(true, null, parent);
    }

}
