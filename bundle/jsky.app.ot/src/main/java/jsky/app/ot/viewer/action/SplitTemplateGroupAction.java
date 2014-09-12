package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPTemplateGroup;
import jsky.app.ot.editor.template.SplitDialog;
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
public final class SplitTemplateGroupAction extends AbstractTemplateAction {

    public SplitTemplateGroupAction(SPViewer viewer) {
        super(viewer, "Split Template Group...");
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            SplitDialog.open(viewer, (ISPTemplateGroup) viewer.getNode());
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    @Override
    public boolean computeEnabledState() {
        return isEditableContext() && isTemplateGroup();
    }

}
