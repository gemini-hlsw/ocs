package jsky.app.ot.viewer.action;

import jsky.app.ot.tpe.TelescopePosEditor;
import jsky.app.ot.tpe.TpeManager;
import jsky.app.ot.viewer.SPViewer;
import jsky.app.ot.viewer.SPViewerActions;
import jsky.util.gui.DialogUtil;

import java.awt.Cursor;
import java.awt.event.ActionEvent;

/**
* Created with IntelliJ IDEA.
* User: rnorris
* Date: 1/17/13
* Time: 2:36 PM
* To change this template use File | Settings | File Templates.
*/
public final class ShowTPEAction extends AbstractViewerAction {
    public ShowTPEAction(SPViewer viewer) {
        super(viewer, "Show the Position Editor", jsky.util.Resources.getIcon("ImageDisplay24.gif", SPViewerActions.class));
        putValue(SHORT_NAME, "Image");
        putValue(SHORT_DESCRIPTION, "Show position editor.");
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            viewer.getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            final TelescopePosEditor tpe = TpeManager.open();
            if (tpe != null) tpe.reset(viewer.getNode());
        } catch (Exception e) {
            DialogUtil.error(e);
        } finally {
            viewer.getParent().setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override
    public boolean computeEnabledState() throws Exception {
        return getProgram() != null;
    }
}
