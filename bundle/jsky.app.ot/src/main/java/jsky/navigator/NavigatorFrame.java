package jsky.navigator;

import jsky.catalog.gui.BasicTablePlotter;
import jsky.catalog.gui.TablePlotter;
import jsky.image.gui.MainImageDisplay;
import jsky.util.Preferences;

import javax.swing.*;
import java.awt.*;

/**
 * Provides a top level window and menubar for the Navigator class.
 */
@Deprecated
public class NavigatorFrame extends JFrame {

    /** Main panel */
    protected Navigator navigator;

    /** Set to true until setVisible is called */
    private boolean firstTime = true;


    /**
     * Create a top level window containing a Navigator panel and
     * display the contents of the given catalog directory in it.
     *
     * @param imageDisplay optional widget to use to display images (if not specified,
     *                     or null, a new window will be created)
     */
    public NavigatorFrame(MainImageDisplay imageDisplay) {
        super("Catalog Navigator");

        TablePlotter plotter = new BasicTablePlotter();

        navigator = new Navigator(this, plotter, imageDisplay);

        getContentPane().add(navigator, BorderLayout.CENTER);

        // set default window size and remember changes between sessions
        Preferences.manageSize(navigator, new Dimension(650, 650));
        Preferences.manageLocation(this);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    /**
     * Create a top level window containing a Navigator panel and
     * display the contents of the given catalog directory in it.
     */
    public NavigatorFrame() {
        this(null);
    }


    /** Return the navigator panel. */
    public Navigator getNavigator() {
        return navigator;
    }


    /** Delay pack until first show of window to avoid linux display bug */
    public void setVisible(boolean b) {
        if (b && firstTime) {
            firstTime = false;
            pack();
        }
        super.setVisible(b);
    }

}

