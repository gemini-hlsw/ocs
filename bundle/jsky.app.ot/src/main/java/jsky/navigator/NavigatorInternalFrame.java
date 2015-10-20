package jsky.navigator;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

import jsky.catalog.CatalogDirectory;
import jsky.catalog.gui.BasicTablePlotter;
import jsky.catalog.gui.CatalogNavigator;
import jsky.catalog.gui.CatalogTree;
import jsky.catalog.gui.TablePlotter;
import jsky.image.gui.MainImageDisplay;
import jsky.util.Preferences;

/**
 * Provides a top level window (internal frame version) for a
 * Navigator panel.
 */
@Deprecated
public class NavigatorInternalFrame extends JInternalFrame {

    /** Main panel */
    protected Navigator navigator;

    // These are used make new frames visible by putting them in different locations
    private static int openFrameCount = 0;
    private static final int xOffset = 30, yOffset = 30;

    /** Set to true until setVisible is called */
    private boolean firstTime = true;

    /**
     * Create a top level window containing an Navigator panel and
     * set it to display the contents of the given catalog directory.
     *
     * @param desktop The JDesktopPane to add the frame to.
     * @param catDir The top level catalog directory to display
     * @param imageDisplay optional widget to use to display images (if not specified,
     *                     or null, a new window will be created)
     */
    public NavigatorInternalFrame(JDesktopPane desktop, CatalogDirectory catDir,
                                  MainImageDisplay imageDisplay) {
        super("Catalog Navigator", true, false, true, true);

        CatalogTree catalogTree = new CatalogTree(catDir);
        TablePlotter plotter = new BasicTablePlotter();

        navigator = new Navigator(this, catalogTree, plotter, imageDisplay);
        navigator.setDesktop(desktop);
        catalogTree.setQueryResult(catDir);

        NavigatorToolBar toolbar = new NavigatorToolBar(navigator);
        getContentPane().add(toolbar, BorderLayout.NORTH);
        getContentPane().add(navigator, BorderLayout.CENTER);
        // NOTE Deleted because NavigatorMenuBar is deprecated
        //setJMenuBar(new NavigatorMenuBar(navigator, toolbar));

        // set default window size and remember changes between sessions
        Preferences.manageSize(navigator, new Dimension(650, 650));
        Preferences.manageLocation(this, xOffset * openFrameCount, yOffset * openFrameCount);
        openFrameCount++;

        setClosable(true);
        setMaximizable(true);

        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }


    /**
     * Create a top level window containing a Navigator panel and
     * display the contents of the given catalog directory in it.
     *
     * @param desktop The JDesktopPane to add the frame to.
     * @param catDir The top level catalog directory to display
     */
    public NavigatorInternalFrame(JDesktopPane desktop, CatalogDirectory catDir) {
        this(desktop, catDir, null);
    }


    /**
     * Create a top level window containing a Navigator panel and
     * display the contents of the default catalog directory in it.
     *
     * @param desktop The JDesktopPane to add the frame to.
     */
    public NavigatorInternalFrame(JDesktopPane desktop) {
        this(desktop, CatalogNavigator.getCatalogDirectory());
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
            revalidate();
        }
        super.setVisible(b);
    }

}

