/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SPViewerMenuBar.java 46728 2012-07-12 16:39:26Z rnorris $
 */

package jsky.app.ot.viewer;

import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.util.security.permission.StaffPermission;
import jsky.app.ot.OT;
import jsky.app.ot.OTOptions;
import jsky.app.ot.gemini.obscat.ObsCatalog;
import jsky.app.ot.plugin.OtActionPlugin;
import jsky.app.ot.session.SessionQueuePanel;
import jsky.app.ot.userprefs.general.GeneralPreferencesPanel;
import jsky.app.ot.userprefs.observer.ObserverPreferencesPanel;
import jsky.app.ot.userprefs.ui.PreferenceDialog;
import jsky.app.ot.userprefs.ui.PreferencePanel;
import jsky.app.ot.util.History;
import jsky.util.gui.Resources;
import jsky.app.ot.util.RootEntry;
import jsky.app.ot.viewer.action.*;
import jsky.app.ot.viewer.plugin.PluginViewerAction;
import jsky.app.ot.viewer.plugin.PluginRegistry;
import jsky.util.Preferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.util.List;

/**
 * Implements a menubar for an SPViewer window.
 * @author Allan Brighton
 * @version $Revision: 46728 $
 */
final class SPViewerMenuBar extends JMenuBar {

    /** The target science program editor */
    private final SPViewer _viewer;

    /** The main OT window toolbar */
    private final SPViewerToolBar _mainToolBar;

    /** The OT toolbar with tree related items. */
    private final SPTreeToolBar _treeToolBar;

    private final JMenu navMenu;
    private final JMenu pluginMenu;

    SPViewerMenuBar(final SPViewer viewer, final SPViewerToolBar mainToolBar, final SPTreeToolBar treeToolBar) {
        _viewer = viewer;
        _mainToolBar = mainToolBar;
        _treeToolBar = treeToolBar;

        add(createFileMenu());
        add(createEditMenu());
        add(createViewMenu());
        add(navMenu = createGoMenu());
        add(pluginMenu = createPluginMenu());
        add(Box.createHorizontalGlue());
        add(createHelpMenu());

    }

    private JMenu createFileMenu() {
        final JMenu menu = new JMenu("File");
        menu.add(new NewAction()); // RCN: always in a new window :-\
        menu.add(OpenNightly$.MODULE$.Calibration());
        menu.add(OpenNightly$.MODULE$.Engineering());
        menu.add(new OpenAction(_viewer));
        menu.add(new OpenInNewWindowAction());
        menu.addSeparator();
        menu.add(new ImportAction(_viewer));
        menu.add(new ExportAction(_viewer));
        menu.add(_viewer._actions.vcsSyncAction);
        menu.add(_viewer._actions.syncAllAction);
        menu.addSeparator();
        menu.add(new FetchLibrariesAction(_viewer));
        menu.addSeparator();
        menu.add(new QueryAction(ObsCatalog.QUERY_MANAGER));

        final ISPProgram r = _viewer.getProgram();
        final SPProgramID id = (r == null) ? null : r.getProgramID();
        if (OTOptions.hasPermission(new StaffPermission(id))) {
            menu.addSeparator();
            menu.add(_viewer._actions.enqueueAction);
            menu.add(createFileSessionQueueMenuItem());
        }

        menu.addSeparator();
        menu.add(new CloseAction(_viewer));
        menu.add(new CloseWindowAction(_viewer));
        menu.add(createFileExitMenuItem());
        return menu;
    }

    private JMenuItem createPreferencesItem() {
        final JMenuItem menuItem = new JMenuItem("Preferences ...");
        menuItem.addActionListener(e -> {
            final PreferencePanel pref = new GeneralPreferencesPanel(_viewer);
            final ImList<PreferencePanel> lst = OTOptions.isStaffGlobally() ?
                    DefaultImList.create(pref, new ObserverPreferencesPanel()) :
                    DefaultImList.create(pref);
            final PreferenceDialog dialog = new PreferenceDialog(lst);
            dialog.show(getFrame(), pref);
        });
        return menuItem;
    }

    private Frame getFrame() {
        final Component comp = SwingUtilities.getRoot(_viewer);
        return (comp instanceof Frame) ? (Frame) comp : null;
    }

    private JMenuItem createFileSessionQueueMenuItem() {
        final JMenuItem menuItem = new JMenuItem("Display Session Queue");
        menuItem.addActionListener(e -> SessionQueuePanel.getInstance().showFrame());
        return menuItem;
    }

    private JMenuItem createFileExitMenuItem() {
        final JMenuItem menuItem = new JMenuItem("Exit");
        menuItem.addActionListener(ae -> _viewer.exit());
        return menuItem;
    }

    private JMenu createViewMenu() {
        final JMenu menu = new JMenu("View");
        menu.add(createViewMainToolBarMenuItem());
        menu.add(createViewTreeToolBarMenuItem());
        menu.addSeparator();
        menu.add(createViewShowMainToolBarAsMenu());
        menu.add(createViewShowTreeToolBarAsMenu());
        menu.addSeparator();
        menu.add(_viewer._actions.showTPEAction);
        menu.add(_viewer._actions.showElevationPlotAction);
        menu.addSeparator();
        menu.add(_viewer._actions.expandObsAction);
        menu.add(_viewer._actions.collapseObsAction);
        menu.addSeparator();
        menu.add(_viewer._actions.expandProgAction);
        menu.add(_viewer._actions.collapseProgAction);
        return menu;
    }

    private JCheckBoxMenuItem createViewMainToolBarMenuItem() {
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem("Main Toolbar");
        final String prefName = getClass().getName() + ".ShowMainToolBar";
        menuItem.addItemListener(e -> {
            final JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
            _mainToolBar.setVisible(rb.getState());
            if (rb.getState())
                Preferences.set(prefName, "true");
            else
                Preferences.set(prefName, "false");
        });
        final String pref = Preferences.get(prefName);
        if (pref != null)
            menuItem.setState(pref.equals("true"));
        else
            menuItem.setState(true);
        return menuItem;
    }

    private JCheckBoxMenuItem createViewTreeToolBarMenuItem() {
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem("Tree Toolbar");
        final String prefName = getClass().getName() + ".ShowTreeToolBar";
        menuItem.addItemListener(e -> {
            final JCheckBoxMenuItem rb = (JCheckBoxMenuItem) e.getSource();
            _treeToolBar.setVisible(rb.getState());
            if (rb.getState())
                Preferences.set(prefName, "true");
            else
                Preferences.set(prefName, "false");
        });
        final String pref = Preferences.get(prefName);
        if (pref != null)
            menuItem.setState(pref.equals("true"));
        else
            menuItem.setState(true);
        return menuItem;
    }

    private JMenu createViewShowMainToolBarAsMenu() {
        final JMenu menu = new JMenu("Show Main Toolbar As");
        final JRadioButtonMenuItem b1 = new JRadioButtonMenuItem("Pictures and Text");
        final JRadioButtonMenuItem b2 = new JRadioButtonMenuItem("Pictures Only");
        final JRadioButtonMenuItem b3 = new JRadioButtonMenuItem("Text Only");

        b1.setSelected(true);
        _mainToolBar.setShowPictures(true);
        _mainToolBar.setShowText(true);

        menu.add(b1);
        menu.add(b2);
        menu.add(b3);

        final ButtonGroup group = new ButtonGroup();
        group.add(b1);
        group.add(b2);
        group.add(b3);

        // name used to store setting in user preferences
        final String prefName = getClass().getName() + ".ShowMainToolBarAs";

        final ItemListener itemListener = e -> {
            final JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
            if (rb.isSelected()) {
                if (rb.getText().equals("Pictures and Text")) {
                    _mainToolBar.setShowPictures(true);
                    _mainToolBar.setShowText(true);
                    Preferences.set(prefName, "1");
                } else if (rb.getText().equals("Pictures Only")) {
                    _mainToolBar.setShowPictures(true);
                    _mainToolBar.setShowText(false);
                    Preferences.set(prefName, "2");
                } else if (rb.getText().equals("Text Only")) {
                    _mainToolBar.setShowPictures(false);
                    _mainToolBar.setShowText(true);
                    Preferences.set(prefName, "3");
                }
            }
        };

        b1.addItemListener(itemListener);
        b2.addItemListener(itemListener);
        b3.addItemListener(itemListener);

        // check for a previous preference setting
        final String pref = Preferences.get(prefName);
        if (pref != null) {
            final JRadioButtonMenuItem[] ar = new JRadioButtonMenuItem[]{null, b1, b2, b3};
            try {
                ar[Integer.parseInt(pref)].setSelected(true);
            } catch (Exception e) {
                // ignore
            }
        }

        return menu;
    }

    private JMenu createViewShowTreeToolBarAsMenu() {
        final JMenu menu = new JMenu("Show Tree Toolbar As");

        final JRadioButtonMenuItem b1 = new JRadioButtonMenuItem("Pictures and Text");
        final JRadioButtonMenuItem b2 = new JRadioButtonMenuItem("Pictures Only");
        final JRadioButtonMenuItem b3 = new JRadioButtonMenuItem("Text Only");

        b1.setSelected(true);
        _treeToolBar.setShowPictures(true);
        _treeToolBar.setShowText(true);

        menu.add(b1);
        menu.add(b2);
        menu.add(b3);

        final ButtonGroup group = new ButtonGroup();
        group.add(b1);
        group.add(b2);
        group.add(b3);

        // name used to store setting in user preferences
        final String prefName = getClass().getName() + ".ShowTreeToolBarAs";

        final ItemListener itemListener = e -> {
            final JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
            if (rb.isSelected()) {
                if (rb.getText().equals("Pictures and Text")) {
                    _treeToolBar.setShowPictures(true);
                    _treeToolBar.setShowText(true);
                    Preferences.set(prefName, "1");
                } else if (rb.getText().equals("Pictures Only")) {
                    _treeToolBar.setShowPictures(true);
                    _treeToolBar.setShowText(false);
                    Preferences.set(prefName, "2");
                } else if (rb.getText().equals("Text Only")) {
                    _treeToolBar.setShowPictures(false);
                    _treeToolBar.setShowText(true);
                    Preferences.set(prefName, "3");
                }
            }
        };

        b1.addItemListener(itemListener);
        b2.addItemListener(itemListener);
        b3.addItemListener(itemListener);

        // check for a previous preference setting
        final String pref = Preferences.get(prefName);
        if (pref != null) {
            final JRadioButtonMenuItem[] ar = new JRadioButtonMenuItem[]{null, b1, b2, b3};
            try {
                ar[Integer.parseInt(pref)].setSelected(true);
            } catch (Exception e) {
                // ignore
            }
        }

        return menu;
    }

    private JMenu createGoMenu() {
        final JMenu menu = new JMenu("Go");
        rebuildNavMenu(menu);
        return menu;
    }

    private JMenu createPluginMenu() {
        final JMenu menu = new JMenu("Tools");
        rebuildPluginMenu(menu);
        return menu;
    }

    private JMenu createHelpMenu() {
        final JMenu menu = new JMenu("Help");
        menu.add(OT.getAboutOTAction());
        menu.add(new SmartGcalInfoAction(_viewer));
        menu.add(OT.getHelpAction());
        return menu;
    }

    private JMenu createEditMenu() {
        final JMenu menu = new JMenu("Edit");

        // Template submenu
        final JMenu _templateMenu = new JMenu("Template");
        _templateMenu.setIcon(Resources.getIcon("template.gif"));
        for (final Action a : _viewer._actions.templateActions)
            _templateMenu.add(new JMenuItem(a));
        menu.add(_templateMenu);
        menu.addSeparator();

        // Observation submenu
        final JMenu obsMenu = new JMenu("Create an Observation");
        obsMenu.setIcon(Resources.getIcon("observation.gif"));
        for (final AbstractAction action : _viewer._actions.addObservationActions) {
            obsMenu.add(new JMenuItem(action));
        }
        menu.add(obsMenu);

        // Group item
        final JMenu groupMenu = new JMenu("Create a Group");
        groupMenu.setIcon(Resources.getIcon("obsGroup.gif"));
        for (final Action action : _viewer._actions.addGroupActions) {
            groupMenu.add(new JMenuItem(action));
        }
        menu.add(groupMenu);

        // Note submenu
        final JMenu noteMenu = new JMenu("Create a Note");
        noteMenu.setIcon(Resources.getIcon("post-it-note18.gif"));
        for (final AbstractAction action : _viewer._actions.addNoteActions) {
            noteMenu.add(new JMenuItem(action));
        }
        menu.add(noteMenu);

        // Observation Component submenu
        final JMenu compMenu = new JMenu("Create an Observation Component");
        compMenu.setIcon(Resources.getIcon("component.gif"));
        compMenu.add(new JMenuItem(_viewer._actions.addSiteQualityAction));
        compMenu.add(new JMenuItem(_viewer._actions.addTargetListAction));
        compMenu.addSeparator();
        for (final AbstractAction action : _viewer._actions.addInstrumentActions) {
            compMenu.add(new JMenuItem(action));
        }
        compMenu.addSeparator();
        for (final AbstractAction action : _viewer._actions.addAOActions) {
            compMenu.add(new JMenuItem(action));
        }
        if (OTOptions.isStaffGlobally()) {
            for (final AbstractAction action : _viewer._actions.addEngineeringActions) {
                compMenu.add(new JMenuItem(action));
            }
        }
        menu.add(compMenu);

        // Iterator Component submenu
        final JMenu iterCompMenu = new JMenu("Create an Iterator Component");
        iterCompMenu.setIcon(Resources.getIcon("iterComp.gif"));
        iterCompMenu.add(new JMenuItem(_viewer._actions.addSequenceAction));
        iterCompMenu.addSeparator();
        for (final AbstractAction action : _viewer._actions.addInstrumentIteratorActions) {
            iterCompMenu.add(new JMenuItem(action));
        }
        menu.add(iterCompMenu);

        // Observation Iterator submenu
        final JMenu iterObsMenu = new JMenu("Create an Observe Iterator");
        iterObsMenu.setIcon(Resources.getIcon("iterObs.gif"));
        for (final AbstractAction action : _viewer._actions.addGenericSeqCompActions) {
            iterObsMenu.add(new JMenuItem(action));
        }
        menu.add(iterObsMenu);

        menu.addSeparator();

        menu.add(_viewer._actions.cutAction);
        menu.add(_viewer._actions.copyAction);
        menu.add(_viewer._actions.pasteAction);

        menu.addSeparator();

        menu.add(_viewer._actions.resolveConflictsAction);
        menu.add(_viewer._actions.conflictPrevAction);
        menu.add(_viewer._actions.conflictNextAction);

        menu.addSeparator();

        menu.add(_viewer._actions.moveToTopAction);
        menu.add(_viewer._actions.moveUpAction);
        menu.add(_viewer._actions.moveDownAction);
        menu.add(_viewer._actions.moveToBottomAction);

        menu.addSeparator();

        menu.add(_viewer._actions.editItemTitleAction);

        menu.add(_viewer._actions.programAdminAction);
        menu.add(_viewer._actions.setPhase2StatusAction);
        menu.add(_viewer._actions.setExecStatusAction);
        menu.add(_viewer._actions.purgeEphemerisAction);

        menu.add(_viewer._actions.showKeyManagerAction);

        menu.addSeparator();
        menu.add(createPreferencesItem());

        return menu;
    }

    void rebuildNavMenu() {
        rebuildNavMenu(navMenu);
    }

    private void rebuildNavMenu(JMenu menu) {
        menu.removeAll();
        menu.add(_viewer._actions.navViewerPrevProgAction);
        menu.add(_viewer._actions.navViewerPrevAction);
        menu.add(_viewer._actions.navViewerNextAction);
        menu.add(_viewer._actions.navViewerNextProgAction);

        // If there are 2 or more programs open, show them in the nav menu.
        final List<RootEntry> rootEntries = _viewer.getHistory().rootEntriesAsJava();
        if (rootEntries.size() > 1) {
            menu.addSeparator();
            for (final RootEntry e: rootEntries) {
                final SPProgramID pid = e.root().getProgramID();
                final String title = (pid == null) ? "- No Program ID -" : pid.toString();
                final JRadioButtonMenuItem item = new JRadioButtonMenuItem(new AbstractAction(title) {
                    public void actionPerformed(ActionEvent actionEvent) {
                        final History h = _viewer.getHistory().go(e.node());
                        _viewer.tryNavigate(h);
                    }
                });
                item.setSelected(e.root() == _viewer.getRoot());
                menu.add(item);
            }
        }
    }

    void rebuildPluginMenu() {
        rebuildPluginMenu(pluginMenu);
    }

    private void rebuildPluginMenu(JMenu menu) {
        final List<OtActionPlugin> plugins = PluginRegistry.pluginsByNameForJava();
        System.out.println("rebuildPluginMenu: " + plugins.size() + " plugins");
        for (int i=0; i<menu.getItemCount(); ++i) {
            Action a = menu.getItem(i).getAction();
            if (a instanceof AbstractViewerAction) {
                ((AbstractViewerAction) a).uninstall(_viewer);
            }
        }
        menu.removeAll();
        for (OtActionPlugin plugin : plugins) {
            menu.add(new PluginViewerAction(OT.getKeyChain(), _viewer, plugin, OT.getMagnitudeTable()));
            System.out.println("Adding menu item for: " + plugin.name());
        }
        menu.setVisible(plugins.size() > 0);
    }
}
