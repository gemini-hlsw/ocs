package jsky.navigator;

import javax.swing.JDesktopPane;
import javax.swing.JLayeredPane;

import jsky.catalog.CatalogDirectory;
import jsky.catalog.gui.CatalogNavigator;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.SwingUtil;

/**
 * This class manages access to the Navigator window on behalf of clients.
 */
@Deprecated
public final class NavigatorManager {

    /**
     * The single Navigator, shared for all instances
     */
    private static Navigator _navigator;


    /**
     * Return the Navigator instance, if it exists, otherwise null.
     */
    public static Navigator get() {
        return _navigator;
    }

    /**
     * Open the Navigator window, creating it if necessary, and return a reference to it.
     */
    public static Navigator open() {
        if (_navigator == null && create() == null)
            return null;

        SwingUtil.showFrame(_navigator.getParentFrame());
        return _navigator;
    }


    /**
     * Create the Navigator window if necessary, and return a reference to it.
     */
    public static Navigator create() {
        if (_navigator == null) {
            CatalogDirectory dir;
            try {
                dir = CatalogNavigator.getCatalogDirectory();
            } catch (Exception e) {
                DialogUtil.error(e);
                return null;
            }

            JDesktopPane desktop = DialogUtil.getDesktop();
            if (desktop == null) {
                _navigator = new NavigatorFrame(dir).getNavigator();
            } else {
                NavigatorInternalFrame f = new NavigatorInternalFrame(desktop, dir);
                _navigator = f.getNavigator();
                desktop.add(f, JLayeredPane.DEFAULT_LAYER);
                desktop.moveToFront(f);
            }
        }

        return _navigator;
    }
}
