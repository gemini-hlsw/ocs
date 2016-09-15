package jsky.app.ot.userprefs.ui;

import edu.gemini.shared.util.immutable.Option;

import javax.swing.*;
import java.awt.*;

/**
 * An interface for describing a preference panel, for use in a
 * {@link PreferenceDialog}.
 */
public interface PreferencePanel {

    /**
     * Gets the display name of the panel, which will be shown in the tool
     * bar button associated with it.
     */
    String getDisplayName();

    /**
     * Gets the (optional) tool tip to display when the mouse hovers over
     * the associated button.
     */
    Option<String> getToolTip();

    /**
     * Gets the (optional) icon to display in the button associated with this
     * panel.
     */
    Option<Icon> getIcon();

    /**
     * Gets the user interface to present when the user selects this
     * panel.
     */
    Component getUserInterface();
}
