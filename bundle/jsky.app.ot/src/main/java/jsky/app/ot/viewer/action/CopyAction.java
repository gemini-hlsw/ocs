package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPProgram;
import jsky.app.ot.viewer.SPViewer;
import jsky.app.ot.viewer.SPViewerActions;
import jsky.util.gui.ClipboardHelper;
import jsky.util.gui.DialogUtil;

import java.awt.event.ActionEvent;

/**
* Created with IntelliJ IDEA.
* User: rnorris
* Date: 1/17/13
* Time: 2:01 PM
* To change this template use File | Settings | File Templates.
*/
public final class CopyAction extends AbstractViewerAction {

    public CopyAction(SPViewer viewer) {
        super(viewer, "Copy", jsky.util.Resources.getIcon("Copy24.gif", SPViewerActions.class));
        putValue(SHORT_DESCRIPTION, "Copy the selected tree node to the clipboard.");
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            ISPNode[] rnodes = viewer.getTree().getSelectedNodes();
            if (rnodes != null) {
                for (int i = 0; i < rnodes.length; i++) {
                    if (rnodes[i] instanceof ISPProgram) {
                        ISPNode[] nodes = new ISPNode[1];
                        nodes[0] = rnodes[i];
                        rnodes = nodes;
                        break;
                    }
                }
                ClipboardHelper.setClipboard(rnodes);
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    public boolean computeEnabledState() throws Exception {
        final ISPNode[] rnodes = viewer.getTree().getSelectedNodes();
        return rnodes != null && rnodes.length > 0;
    }

}
