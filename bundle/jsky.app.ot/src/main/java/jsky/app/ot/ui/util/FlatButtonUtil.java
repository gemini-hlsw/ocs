//
// $
//

package jsky.app.ot.ui.util;

import edu.gemini.shared.gui.ButtonFlattener;
import jsky.util.gui.Resources;

import javax.swing.*;

/**
 * Utility for creating commonly used flat buttons.
 */
public final class FlatButtonUtil {
    private FlatButtonUtil() { }

    /**
     * Creates a flat JButton with the indicated icon.
     *
     * @param iconFilePath relative path to the OT image resource
     * 
     * @return new JButton initialized to appear flat and have
     * appropriate rollover and pressed icons
     */
    public static JButton create(String iconFilePath) {
        JButton btn = new JButton(Resources.getIcon(iconFilePath));
        ButtonFlattener.flatten(btn);
        return btn;
    }

    /**
     * Creates a flat button with a little red X icon.
     */
    public static JButton createSmallRemoveButton() {
        return create("eclipse/remove_small.gif");
    }

    /**
     * Creates a flat, transparent button with a green + icon.
     */
    public static JButton createSmallAddButton() {
        return create("eclipse/add_small.gif");
    }

    /**
     * Creates a flat, transparent button with a ? for help.
     */
    public static JButton createHelpButton() {
        return create("eclipse/help.gif");
    }
}
