package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPProgram;
import jsky.app.ot.editor.template.ReapplicationDialog;
import jsky.app.ot.viewer.SPViewer;
import jsky.app.ot.viewer.SPViewerActions;
import jsky.util.gui.DialogUtil;

import java.awt.event.ActionEvent;

/**
* Created with IntelliJ IDEA.
* User: rnorris
* Date: 1/17/13
* Time: 2:42 PM
* To change this template use File | Settings | File Templates.
*/
public final class ReapplyTemplateAction extends AbstractTemplateAction {

    public ReapplyTemplateAction(SPViewer viewer) {
        super(viewer, "Reapply Template...", jsky.util.Resources.getIcon("reapply.png", SPViewerActions.class));
        putValue(SHORT_NAME, "Reapply");
        putValue(SHORT_DESCRIPTION, "Reapply a template group.");
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            ReapplicationDialog.open(viewer, viewer.getRoot(), viewer.getNode());
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    @Override
    public boolean computeEnabledState() {
        return isEditableContext() && isTemplateEnabled();
    }

}
