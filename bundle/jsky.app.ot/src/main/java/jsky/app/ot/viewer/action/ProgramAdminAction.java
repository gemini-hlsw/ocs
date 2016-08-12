package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPProgram;
import jsky.app.ot.OTOptions;
import jsky.app.ot.progadmin.AdminDialog;
import jsky.app.ot.viewer.SPViewer;
import jsky.app.ot.viewer.ViewerManager;
import jsky.util.gui.DialogUtil;

import java.awt.event.ActionEvent;

/**
* Created with IntelliJ IDEA.
* User: rnorris
* Date: 1/17/13
* Time: 1:22 PM
* To change this template use File | Settings | File Templates.
*/
public final class ProgramAdminAction extends AbstractViewerAction {

    public ProgramAdminAction(SPViewer viewer) {
        super(viewer, "Program Admin...");
    }

    public void actionPerformed(ActionEvent evt) {
        ISPProgram prog = viewer.getProgram();
        if (prog != null) {
            try {
                final ISPProgram res = AdminDialog.showAdminDialog(viewer.getDatabase(), prog);
                if (res != prog)
                    ViewerManager.open(res, viewer);
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    }

    @Override
    public boolean computeEnabledState() throws Exception {
        return getProgram() != null && OTOptions.isStaff(getProgram().getProgramID());
    }

}
