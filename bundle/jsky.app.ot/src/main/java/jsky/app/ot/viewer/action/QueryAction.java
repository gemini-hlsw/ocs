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
    private static QueryManager _queryManager = null;

    /**
     * Set the (observatory specific) object responsible for displaying a window where
     * the user can query the science program database.
     * This method should be called once when the application starts. Callers should
     * use the OpenActions.QueryAction class for menu and toolbar items.
     */
    public static void setQueryManager(final QueryManager queryManager) {
        _queryManager = queryManager;
    }

    public QueryAction() {
        super("OT Browser...");
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
