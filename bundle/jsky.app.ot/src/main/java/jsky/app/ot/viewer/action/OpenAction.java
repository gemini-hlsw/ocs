package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.shared.util.immutable.ImOption;

import jsky.app.ot.viewer.OpenUtils;
import jsky.app.ot.viewer.SPViewer;
import jsky.app.ot.viewer.ViewerManager;
import jsky.util.gui.BusyWin;

import java.awt.event.ActionEvent;

/**
 * The OpenAction class handles opening a science program in the database.
 */
public class OpenAction extends AbstractViewerAction {

    public OpenAction() {
        this(null);
    }

    public OpenAction(final SPViewer viewer) {
        super(viewer, "Open...", jsky.util.Resources.getIcon("Open24.gif", OpenAction.class));
        putValue(SHORT_NAME, "Open");
        putValue(SHORT_DESCRIPTION, "Open a science program from the local database.");
        setEnabled(true);
    }

    public boolean computeEnabledState() {
        return true;
    }

    public void actionPerformed(final ActionEvent evt) {
        BusyWin.showBusy();

        // Returns an array of one or more ISPPrograms from the database
        final ISPProgram[] progs = OpenUtils.openDBPrograms();

        // Messages are produced by above so just return
        if ((progs == null) || (progs.length == 0)) return;

        // Open them all in the current viewer window, if any.  If not find an
        // empty viewer to recycle.  If none, make a viewer to house them all.
        final SPViewer v =
            ImOption.apply(viewer)
                    .orElse(ImOption.apply(ViewerManager.findEmptyOrNull()))
                    .getOrElse(() -> ViewerManager.newViewer());

        BusyWin.showBusy();
        for (final ISPProgram prog : progs) {
            ViewerManager.open(prog, v);
        }

    }

}
