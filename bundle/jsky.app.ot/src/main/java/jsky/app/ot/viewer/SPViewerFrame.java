/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SPViewerFrame.java 39256 2011-11-22 17:42:49Z swalker $
 */

package jsky.app.ot.viewer;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.Platform;
import jsky.app.ot.OT;
import jsky.app.ot.OTOptions;
import jsky.app.ot.editor.eng.EngToolWindow;
import jsky.app.ot.util.Resources;
import jsky.app.ot.vcs.VcsStatusPanel;
import jsky.util.Preferences;
import jsky.util.gui.BusyWin;
import org.noos.xing.mydoggy.*;
import org.noos.xing.mydoggy.plaf.MyDoggyToolWindowManager;
import org.noos.xing.mydoggy.plaf.ui.drag.DragAndDropLock;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a top level window and menu bar for the SPViewer class.
 */
public final class SPViewerFrame extends JFrame {
    private final Logger LOG = Logger.getLogger(SPViewerFrame.class.getName());

    public static final String PROBLEM_TOOLWINDOW_KEY = "Problems";
    public static final String CONFLICT_TOOLWINDOW_KEY = "Conflicts";

    // main panel
    private SPViewer _viewer;

    // count of instances of this class
    private static int _count = 0;

    private SPViewerToolBar _toolbar;
    private ToolWindowManager _toolWindowManager;

    /**
     * Create a top level window containing a SPViewer panel.
     */
    public SPViewerFrame(final IDBDatabaseService db) {
        super("Science Program Editor");
        _viewer = new SPViewer(db);
        _count++;
        _viewer.setParentFrame(this);
        _toolbar = new SPViewerToolBar(_viewer);
        getContentPane().add(_toolbar, BorderLayout.NORTH);

        initToolWindowManager();
        // set default window size
        Preferences.manageSize(this, new Dimension(875, 680));
        Preferences.manageLocation(this, -1, -1, getClass().getName() + _count + ".pos");

        if (Platform.get() == Platform.osx) {
            hookMacQuitMenu();
        }
        Resources.setOTFrameIcon(this);

        pack();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                _viewer.closeWindow();
            }
        });
        _viewer.updateConflictToolWindow();
        setVisible(true);
    }

    @SuppressWarnings("unchecked")
    private void hookMacQuitMenu() {
        // Gracefully handle the CMD-Q action on Mac.  Requires reflection
        // since Apple specific classes are used.
        try {
            final Class applicationClass             = Class.forName("com.apple.eawt.Application");
            final Class applicationQuitHandlerClass  = Class.forName("com.apple.eawt.QuitHandler");
            final Class applicationQuitResponseClass = Class.forName("com.apple.eawt.QuitResponse");

            final Object app = applicationClass.getMethod("getApplication").invoke(null);
            applicationClass.getMethod("removeAboutMenuItem").invoke(app);
            applicationClass.getMethod("setQuitHandler", applicationQuitHandlerClass).invoke(app,
                Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{applicationQuitHandlerClass},
                        (proxy, method, args) -> {
                            getViewer().exit(); // will prompt for save, etc
                            applicationQuitResponseClass.getMethod("cancelQuit").invoke(args[1]);
                            return null; // fortunately all methods on this interface are void
                        }));

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Trouble installing Quit hook for Mac.", e);
        }
   	}

    /**
     * Return the main science program viewer panel
     */
    public SPViewer getViewer() {
        return _viewer;
    }

    public SPViewerToolBar getToolBar() {
        return _toolbar;
    }

    private void initToolWindowManager() {
        // Create a new instance of MyDoggyToolWindowManager passing the frame.
        MyDoggyToolWindowManager myDoggyToolWindowManager = new MyDoggyToolWindowManager(this);
        _toolWindowManager = myDoggyToolWindowManager;

        //Lock the drag and drop of the tabs
        DragAndDropLock.setLocked(true);

        ToolWindow win;

        if (OTOptions.isStaffGlobally()) {
            win = _toolWindowManager.registerToolWindow(
                    EngToolWindow.ENG_TOOL_WINDOW_KEY,// Id
                    "Engineering",           // Title
                    Resources.getIcon("eclipse/engineering.gif"), // Icon
                    _viewer.getEngToolWindow(),
                    ToolWindowAnchor.RIGHT);   // Anchor

            win.setIndex(0);
            win.setAvailable(true);
            setupToolWindow(win);
        }

        win = _toolWindowManager.registerToolWindow(PROBLEM_TOOLWINDOW_KEY,// Id
                "Problems",           // Title
                null,                 // Icon
                _viewer.getProblemsViewer().getPanel(), // Component
                ToolWindowAnchor.RIGHT);   // Anchor
        setupToolWindow(win);

        win = _toolWindowManager.registerToolWindow(
                CONFLICT_TOOLWINDOW_KEY,
                "Conflicts",
                null,
                _viewer.getConflictToolWindow(),
                ToolWindowAnchor.RIGHT);
        setupToolWindow(win);

        //in leopard the setting done by doggy makes a mess in the comboboxes,
        // so we revert it here
        if (OT.IS_MAC_10_5_PLUS) {
            JPopupMenu.setDefaultLightWeightPopupEnabled(true);
            ToolTipManager.sharedInstance().setLightWeightPopupEnabled(true);
        }

        initContentManager();

        // Add myDoggyToolWindowManager to the frame. MyDoggyToolWindowManager
        // is an extension of a JPanel
        final Container c = this.getContentPane();
        c.add(myDoggyToolWindowManager, BorderLayout.CENTER);
        c.add(new VcsStatusPanel(_viewer.getVcsStateTracker()).peer(), BorderLayout.SOUTH);
    }

    private void setupToolWindow(ToolWindow win) {
        win.setIndex(0);

        DockedTypeDescriptor dtd;
        dtd = (DockedTypeDescriptor) win.getTypeDescriptor(ToolWindowType.DOCKED);
        dtd.setDockLength(200);
        dtd.setPopupMenuEnabled(true);

        SlidingTypeDescriptor std;
        std = (SlidingTypeDescriptor) win.getTypeDescriptor(ToolWindowType.SLIDING);
        std.setEnabled(false);
        std.setTransparentMode(true);
        std.setTransparentRatio(0.8f);
        std.setTransparentDelay(0);

        FloatingTypeDescriptor ftd;
        ftd = (FloatingTypeDescriptor) win.getTypeDescriptor(ToolWindowType.FLOATING);
        ftd.setEnabled(true);
        ftd.setLocation(150, 200);
        ftd.setSize(320, 200);
        ftd.setModal(false);
        ftd.setTransparentMode(true);
        ftd.setTransparentRatio(0.2f);
        ftd.setTransparentDelay(1000);
    }


    private void initContentManager() {
        JPanel mainContent = _buildMainContent();
        ContentManager contentManager = _toolWindowManager.getContentManager();
        contentManager.addContent("Program",
                "Program",
                null,      // An icon
                mainContent);
        setupContentManagerUI();
    }

    private void setupContentManagerUI() {
        TabbedContentManagerUI contentManagerUI = (TabbedContentManagerUI) _toolWindowManager.getContentManager().getContentManagerUI();
        contentManagerUI.setShowAlwaysTab(false);

        TabbedContentUI contentUI = contentManagerUI.getContentUI(_toolWindowManager.getContentManager().getContent(0));
        contentUI.setCloseable(false);
        contentUI.setDetachable(false);
        contentUI.setTransparentMode(false);
    }

    private JPanel _buildMainContent() {
        JPanel panel = new JPanel(new BorderLayout());
        SPTreeToolBar treeToolbar = new SPTreeToolBar(_viewer);
        _toolbar = new SPViewerToolBar(_viewer);
        getContentPane().add(_toolbar, BorderLayout.NORTH);
        panel.add(treeToolbar, BorderLayout.WEST);
        panel.add(_viewer, BorderLayout.CENTER);
        getContentPane().add(panel, BorderLayout.CENTER);
        setJMenuBar(new SPViewerMenuBar(_viewer, _toolbar, treeToolbar));
        return panel;

    }

    public void rebuildMainContent() {

        // If I don't do this, the menu bar doesn't work
        BusyWin.showBusy();

        // Toolbar
        _toolbar = new SPViewerToolBar(_viewer);
        final Container contentPane = getContentPane();
        final BorderLayout bl = (BorderLayout) contentPane.getLayout();
        contentPane.remove(bl.getLayoutComponent(BorderLayout.NORTH));
        contentPane.add(_toolbar, BorderLayout.NORTH);

        // Tree Toolbar (in the content panel)
        final ContentManager contentManager = _toolWindowManager.getContentManager();
        final JPanel panel = (JPanel) contentManager.getContent("Program").getComponent();
        final BorderLayout panelLayout = (BorderLayout) panel.getLayout();
        final SPTreeToolBar treeToolbar = new SPTreeToolBar(_viewer);
        panel.remove(panelLayout.getLayoutComponent(BorderLayout.WEST));
        panel.add(treeToolbar, BorderLayout.WEST);
        _viewer._actions._updateEnabledStates();

        // Menu bar
        final JMenuBar menu = new SPViewerMenuBar(_viewer, _toolbar, treeToolbar);
        setJMenuBar(menu);

        // Ugh
        _viewer.updateAfterPermissionsChange();

    }

    public void rebuildNavMenu() {
        ((SPViewerMenuBar) getJMenuBar()).rebuildNavMenu();
    }

    public void rebuildPluginMenu() {
        ((SPViewerMenuBar) getJMenuBar()).rebuildPluginMenu();
    }

    public ToolWindowManager getToolWindowManager() {
        return _toolWindowManager;
    }

}

