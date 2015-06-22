/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SPViewerToolBar.java 47001 2012-07-26 19:40:02Z swalker $
 */

package jsky.app.ot.viewer;

import edu.gemini.shared.gui.ButtonFlattener;
import jsky.app.ot.OTOptions;
import jsky.app.ot.viewer.action.AbstractViewerAction;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * A tool bar for the main OT window.
 */
public final class SPViewerToolBar extends JToolBar {

    /** If true, display button icons */
    private boolean showPictures = true;

    /** If true, display button labels */
    private boolean showText = true;

    /**
     * Create the toolbar for the given OT window
     */
    public SPViewerToolBar(SPViewer viewer) {
        setFloatable(false);

        add(viewer._actions.showProgramManagerAction);
        add(viewer._actions.navViewerPrevProgAction);
        add(viewer._actions.navViewerPrevAction);
        add(viewer._actions.navViewerNextAction);
        add(viewer._actions.navViewerNextProgAction);

        addSeparator();

        add(viewer._actions.cutAction);
        add(viewer._actions.copyAction);
        add(viewer._actions.pasteAction);

        addSeparator();

        add(viewer._actions.showElevationPlotAction);
        add(viewer._actions.showTPEAction);
        add(viewer._actions.getLibAction);

        addSeparator();

        add(viewer._actions.templateApplyAction);
        add(viewer._actions.templateReapplyAction);

        if (OTOptions.isStaffGlobally()) {
            addSeparator();
            add(viewer._actions.enqueueAction);
        }
        add(new JPanel());

        add(viewer._actions.conflictNextAction);
        add(viewer._actions.vcsSyncAction);
        add(viewer._actions.vcs2SyncAction);
        super.addSeparator();
    }

    public boolean isShowPictures() {
        return showPictures;
    }

    /** Set to true to show toolbar buttons with icons */
    public void setShowPictures(boolean b) {
        showPictures = b;
        update();
    }

    /** Set to true to show toolbar buttons with labels */
    public void setShowText(boolean b) {
        showText = b;
        update();
    }

    @Override protected JButton createActionComponent(Action a) {
        final JButton button = super.createActionComponent(a);
        flatten(button);

        button.addPropertyChangeListener(JButton.ICON_CHANGED_PROPERTY, new PropertyChangeListener() {
            @Override public void propertyChange(PropertyChangeEvent evt) {
                flatten(button);
                repaint();
            }
        });

        return button;
    }

    private static void flatten(JButton button) {
        ButtonFlattener.flatten(button);
        button.setContentAreaFilled(true);
    }

    @Override public void addSeparator() {
        add(new Separator(new Dimension(20, 30)) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                final Rectangle rect = getBounds();
                final int x = rect.width / 2;
                final Color c = g.getColor();
                g.setColor(Color.LIGHT_GRAY);
                g.drawLine(x, 0, x, rect.height);
                g.setColor(c);
            }
        });
    }

    /*
    private JButton makeEnableEditButton() {
        if (_enableEditButton == null) {
            _enableEditButton = makeButton("Enable/Disable editing of the current science program or observation.", null, false);
            _enableEditButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ISPProgram prog = _viewer.getProgram();
                    if (prog != null)
                        OT.setEditable(prog, !OTOptions.isEditable(prog));
                }
            });
        }

        updateEditButton();
        return _enableEditButton;
    }

    // Update the Edit button to reflect the current editable state
    void updateEditButton() {
        ISPNode root = _viewer.getRoot();
        if (OTOptions.isEditable(root)) {
            updateButton(_enableEditButton,
                         (showPictures ? "Edit" : "Edit ON"),
                         jsky.util.Resources.getIcon("Edit24.gif", this.getClass()));
        } else {
            updateButton(_enableEditButton,
                         (showPictures ? "Edit" : "Edit OFF"),
                         jsky.util.Resources.getIcon("NoEdit24.gif", this.getClass()));
        }
    }
    */

    private static final String getText(Action a) {
        final String shortName = (String) a.getValue(AbstractViewerAction.SHORT_NAME);
        return (shortName == null) ? (String) a.getValue(Action.NAME) : shortName;
    }

    private static final Icon getIcon(Action a) {
        final Icon largeIcon = (Icon) a.getValue(Action.LARGE_ICON_KEY);
        return (largeIcon == null) ? (Icon) a.getValue(Action.SMALL_ICON) : largeIcon;
    }

    /**
     * Update the toolbar display using the current text/pictures options.
     */
    public final void update() {
        for (Component comp : getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                Action a = button.getAction();
                if (a != null) {
                    button.setHideActionText(!showText);
                    if (showText) {
                        button.setText(getText(a));
                    } else {
                        button.setText(null);
                    }
                    if (showPictures) {
                        button.setIcon(getIcon(a));
                        flatten(button);
                    } else {
                        button.setIcon(null);
                    }
                }
            }
        }
    }
}

