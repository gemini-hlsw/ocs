package jsky.app.ot.viewer.action;

import edu.gemini.util.security.ext.auth.ui.AuthDialog;
import jsky.app.ot.OT;
import jsky.app.ot.viewer.SPViewer;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.Resources;

import java.awt.event.ActionEvent;

/**
* Created with IntelliJ IDEA.
* User: rnorris
* Date: 1/17/13
* Time: 1:33 PM
* To change this template use File | Settings | File Templates.
*/
public class OpenKeyManagerAction extends AbstractViewerAction {

    private final String detailText = "Database keys allow you to access programs and OT features.";

    public OpenKeyManagerAction(final SPViewer viewer) {
        super(viewer, "Manage Keys...");
    }

    public void actionPerformed(ActionEvent e) {
        try {

            // Let the user muck around with keys. This can change the content
            // of our current Subject, affecting our privileges.
            AuthDialog authDialog = AuthDialog.instance().createWithDetailText(OT.getKeyChain(), detailText);
            // Need to set the icon before opening
            Resources.setOTFrameIcon(authDialog.peer());
            authDialog.open(scala.swing.Component.wrap(viewer));

            Resources.setOTFrameIcon(authDialog.peer());

            if (viewer != null) {
                viewer.authListener.propertyChange(null); // force redraw of menu and editor
            }

        } catch (Exception ex) {
            DialogUtil.error(ex);
        }
    }

    @Override
    public boolean computeEnabledState() {
        return true;
    }
}
