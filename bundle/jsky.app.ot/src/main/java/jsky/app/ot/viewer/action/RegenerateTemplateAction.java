package jsky.app.ot.viewer.action;

import jsky.app.ot.editor.template.RegenerationDialog;
import jsky.app.ot.viewer.SPViewer;
import jsky.util.gui.DialogUtil;

import java.awt.event.ActionEvent;

/**
* Created with IntelliJ IDEA.
* User: rnorris
* Date: 1/17/13
* Time: 2:42 PM
* To change this template use File | Settings | File Templates.
*/
public final class RegenerateTemplateAction extends AbstractTemplateAction {

    public RegenerateTemplateAction(SPViewer viewer) {
        super(viewer, "Regenerate...");
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            RegenerationDialog.open(viewer, viewer.getRoot());
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    @Override
    public boolean computeEnabledState() {
        return isEditableContext() && isTemplateFolder();
    }

}
