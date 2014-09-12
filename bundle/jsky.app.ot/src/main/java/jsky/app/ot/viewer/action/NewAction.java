package jsky.app.ot.viewer.action;

import jsky.app.ot.viewer.SPViewer;
import jsky.app.ot.viewer.ViewerManager;
import jsky.util.gui.BusyWin;
import jsky.util.gui.DialogUtil;

import java.awt.event.ActionEvent;

/**
* Created with IntelliJ IDEA.
* User: rnorris
* Date: 7/30/13
* Time: 8:45 AM
* To change this template use File | Settings | File Templates.
*/
public class NewAction extends AbstractViewerAction {

    public NewAction() {
        this(null);
    }

    public NewAction(SPViewer viewer) {
        super(viewer, "New Program");
        setEnabled(true);
    }

    public void actionPerformed(final ActionEvent evt) {
        BusyWin.showBusy();
        try {
            ViewerManager.newProgram(viewer);
        } catch (Exception ex) {
            DialogUtil.error(ex);
        }
    }

    public boolean computeEnabledState() throws Exception {
        return true;
    }

}
