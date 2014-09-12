package jsky.app.ot.viewer.action;

import jsky.app.ot.util.History;
import jsky.app.ot.viewer.SPViewer;
import jsky.app.ot.viewer.SPViewerActions;

import java.awt.event.ActionEvent;

public abstract class NavAction extends AbstractViewerAction {

    public NavAction(SPViewer viewer, String text, String icon, String desc) {
        super(viewer, text, jsky.util.Resources.getIcon(icon, SPViewerActions.class));
        if (desc != null) putValue(SHORT_DESCRIPTION, desc);
    }

    public void actionPerformed(ActionEvent evt) {
        viewer.tryNavigate(navigate(viewer.getHistory()));
    }

    public boolean computeEnabledState() throws Exception {
        return isEnabled(viewer.getHistory());
    }

    protected abstract History navigate(History h);

    protected abstract Boolean isEnabled(History h);

}

