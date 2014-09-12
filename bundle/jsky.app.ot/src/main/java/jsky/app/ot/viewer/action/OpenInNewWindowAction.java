package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPProgram;
import jsky.app.ot.viewer.OpenUtils;
import jsky.app.ot.viewer.ViewerManager;
import jsky.util.gui.BusyWin;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * The OpenAction class handles opening a science program in the database.
 */
public class OpenInNewWindowAction extends AbstractAction {

    public OpenInNewWindowAction() {
        super("Open in new window...");
        setEnabled(true);
    }

    public void actionPerformed(final ActionEvent evt) {
        BusyWin.showBusy();

        // Returns an array of one or more ISPPrograms from the database
        final ISPProgram[] progs = OpenUtils.openDBPrograms();

        // Messages are produced by above so just return
        if ((progs == null) || (progs.length == 0)) return;

        BusyWin.showBusy();
        for (final ISPProgram prog : progs) {
            ViewerManager.openInNewViewer(prog);
        }

    }

}
