package jsky.app.ot.viewer.action;

import jsky.app.ot.viewer.SPViewer;
import jsky.util.gui.BusyWin;
import jsky.util.gui.DialogUtil;

import java.awt.event.ActionEvent;

/**
* Created with IntelliJ IDEA.
* User: rnorris
* Date: 1/17/13
* Time: 2:30 PM
* To change this template use File | Settings | File Templates.
*/
public final class ExpandProgAction extends AbstractViewerAction {

    public ExpandProgAction(SPViewer viewer) {
        super(viewer, "Expand Whole Program");
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            if (viewer.getRoot() != null) {
                BusyWin.showBusy();
                viewer.getTree().expandAll(viewer.getRoot());
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    @Override
    public boolean computeEnabledState() {
        return true;
    }

}
