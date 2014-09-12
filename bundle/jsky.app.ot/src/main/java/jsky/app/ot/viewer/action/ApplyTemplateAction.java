package jsky.app.ot.viewer.action;

import jsky.app.ot.editor.template.InstantiationDialog;
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
public final class ApplyTemplateAction extends AbstractTemplateAction {

    public ApplyTemplateAction(SPViewer viewer) {
        super(viewer, "Apply Template...",  jsky.util.Resources.getIcon("apply.png", SPViewerActions.class));
        putValue(SHORT_NAME, "Apply");
        putValue(SHORT_DESCRIPTION, "Apply a template group.");
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            InstantiationDialog.open(viewer, viewer.getRoot(), viewer.getNode());
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    @Override
    public boolean computeEnabledState() {
        return isEditableContext() && isTemplateEnabled();
    }

}
