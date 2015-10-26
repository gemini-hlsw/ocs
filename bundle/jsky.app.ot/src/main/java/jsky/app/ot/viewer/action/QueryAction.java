package jsky.app.ot.viewer.action;

import jsky.app.ot.viewer.QueryManager;
import jsky.util.gui.BusyWin;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * The QueryAction is for querying the online database and displaying
 * the science programs found.
 */
public final class QueryAction extends AbstractAction {
    // An object that can display a window for querying the science program database
    private final QueryManager _queryManager;

    /**
     * Set the (observatory specific) object responsible for displaying a window where
     * the user can query the science program database
     */
    public QueryAction(final QueryManager queryManager) {
        super("OT Browser...");
        _queryManager = queryManager;
        if (_queryManager == null) {
            setEnabled(false);
        }
    }

    public void actionPerformed(final ActionEvent evt) {
        if (_queryManager != null) {
            BusyWin.showBusy();
            _queryManager.queryDB();
        }
    }

}
