package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.spModel.data.ISPDataObject;
import jsky.app.ot.viewer.SPViewer;
import jsky.util.gui.DialogUtil;

import java.awt.event.ActionEvent;

/**
* Created with IntelliJ IDEA.
* User: rnorris
* Date: 1/17/13
* Time: 1:06 PM
* To change this template use File | Settings | File Templates.
*/
public final class EditItemTitleAction extends AbstractViewerAction {

    public EditItemTitleAction(SPViewer viewer) {
        super(viewer, "Edit Item Title...");
    }

    public void actionPerformed(ActionEvent evt) {
        if (viewer.getCurrentEditor() != null) {
            ISPDataObject dataObj = viewer.getCurrentEditor().getDataObject();
            if (dataObj != null) {
                String s = dataObj.getEditableTitle();
                s = DialogUtil.input(viewer, "New Title:", s);
                if (s != null && s.length() != 0) {
                    try {
                        dataObj.setTitle(s);
                    } catch (Exception e) {
                        DialogUtil.error("Can't change the title for this item."); // TODO: WAT
                        return;
                    }
                    viewer.apply();
                }
            }
        }
    }

    @Override
    public boolean computeEnabledState() throws Exception {
        return isEditableContext() && getContextNode(ISPNode.class) != null;
    }
}
