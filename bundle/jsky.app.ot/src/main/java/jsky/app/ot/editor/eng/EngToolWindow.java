//
// $
//

package jsky.app.ot.editor.eng;

import javax.swing.*;
import java.awt.*;

/**
 * The docking window to be used with the OT docking framework.  The framework
 * requires registering a component which cannot later be changed for a
 * different one.  This class provides a wrapper panel whose content can be
 * updated to show the appropriate engineering panel.
 */
public final class EngToolWindow extends JPanel {

    /**
     * The key with which this panel is registered with the docking
     * framework.
     */
    public static final String ENG_TOOL_WINDOW_KEY = "Engineering";

    private Component comp;

    public EngToolWindow() {
        super(new BorderLayout());
    }

    /**
     * Gets the engineering component to display in the window, if any.
     */
    public Component getComponent() {
        return comp;
    }

    /**
     * Sets the engineering component to display in the window, if any.
     *
     * @param comp component to display, may be <code>null</code> if there
     * is no UI to show
     */
    public void setComponent(Component comp) {
        this.comp = comp;
        removeAll();
        add(comp, BorderLayout.CENTER);
        setPreferredSize(comp.getPreferredSize());
        revalidate();
        repaint();
    }
}
