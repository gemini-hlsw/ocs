package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPNode;
import jsky.app.ot.nsp.SPTreeEditUtil;
import jsky.app.ot.viewer.SPViewer;
import jsky.app.ot.viewer.SPViewerActions;
import jsky.util.gui.BusyWin;
import jsky.util.gui.ClipboardHelper;
import jsky.util.gui.DialogUtil;

import java.awt.event.ActionEvent;

/**
* Created with IntelliJ IDEA.
* User: rnorris
* Date: 1/17/13
* Time: 1:57 PM
* To change this template use File | Settings | File Templates.
*/
public final class CutAction extends AbstractViewerAction {

    public CutAction(SPViewer viewer) {
        super(viewer, "Cut", jsky.util.Resources.getIcon("Cut24.gif", SPViewerActions.class));
        putValue(SHORT_DESCRIPTION, "Cut the selected tree node to the clipboard.");
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            BusyWin.showBusy();
            final ISPNode[] rnodes = viewer.getTree().getSelectedNodes();
            if (rnodes != null) {
                for (ISPNode n: rnodes)
                    SPTreeEditUtil.removeNode(n);
                ClipboardHelper.setClipboard(rnodes);
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    @Override
    public boolean computeEnabledState() throws Exception {
        final ISPNode n = getContextNode(ISPNode.class);
        return isEditableContext() && n != null && SPTreeEditUtil.isOkayToDelete(viewer.getDatabase(), n);
    }

}
