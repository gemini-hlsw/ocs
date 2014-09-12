package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPNode;
import jsky.app.ot.viewer.OpenUtils;
import jsky.app.ot.viewer.SPViewer;

import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * The ExportAction handles exporting a Science Program to an
 * XML file.
 */
public final class ExportAction extends AbstractViewerAction {

    public ExportAction(SPViewer viewer) {
        super(viewer, "Export as XML...");
    }

    public void actionPerformed(final ActionEvent evt) {
        ISPNode root = null;
        Component c = null;   // parent component for file chooser
        if (viewer != null) {
            c = viewer;
            root = viewer.getRoot();
        }
        OpenUtils.exportProgAsXml(root, null, c);
    }

    public boolean computeEnabledState() throws Exception {
        return true;
    }

}
