package jsky.app.ot.viewer;

import jsky.app.ot.OTOptions;
import jsky.util.gui.Resources;
import jsky.util.gui.GenericToolBar;

import javax.swing.*;
import java.awt.*;

/**
 * A tool bar for the OT tree window.
 */
public class SPTreeToolBar extends GenericToolBar {

    /** The target science program editor */
    private SPViewer _viewer;

    // toolbar buttons
    private JButton _observationMenuButton;
    private JButton _observationGroupButton;
    private JButton _componentMenuButton;
    private JButton _noteMenuButton;
    private JButton _iterCompMenuButton;
    private JButton _iterObsMenuButton;

    /**
     * Create a toolbar with tree related actions for the given OT window.
     */
    public SPTreeToolBar(SPViewer viewer) {
        super(null, false, VERTICAL);
        setLayout(new GridLayout(11, 1));
        _viewer = viewer;
        setFloatable(false);
        showPictures = true;
        showText = true;
        addToolBarItems();
        _viewer._actions._updateEnabledStates();
    }

    /**
     * Add the items to the tool bar.
     */
    protected void addToolBarItems() {
        add(makeObservationMenuButton());
        add(makeObservationGroupButton());
        add(makeNoteMenuButton());

        add(makeComponentMenuButton());
        add(makeIterCompMenuButton());
        add(makeIterObsMenuButton());
    }

    /**
     * Make the Observation menu button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the Observation menu button
     */
    protected JButton makeObservationMenuButton() {
        if (_observationMenuButton == null) {
            JPopupMenu menu = new JPopupMenu();
            for (AbstractAction action : _viewer._actions.addObservationActions) {
                menu.add(new JMenuItem(action));
            }
            _observationMenuButton = makeMenuButton("Create an observation.", menu);
        }

        updateButton(_observationMenuButton, "Observation",
                Resources.getIcon("observationMenu.gif"));

        return _observationMenuButton;
    }

    /**
     * Make the ObservationGroup button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the Observation button
     */
    protected JButton makeObservationGroupButton() {
        if (_observationGroupButton == null)  {

            JPopupMenu menu = new JPopupMenu();
            for (Action action: _viewer._actions.addGroupActions) {
                menu.add(new JMenuItem(action));
            }
            _observationGroupButton = makeMenuButton("Create an observation group containing the selected observations"
                                                 + ", or move them out of the group, if already there.",
                                                 menu);
        }
        updateButton(_observationGroupButton,
                     "Group",
                     Resources.getIcon("obsGroupCreate.gif"));
        return _observationGroupButton;
    }

    /**
     * Make the Observation Component menu button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the Observation Component menu button
     */
    protected JButton makeComponentMenuButton() {
        if (_componentMenuButton == null) {
            JPopupMenu menu = new JPopupMenu();
            menu.add(new JMenuItem(_viewer._actions.addSiteQualityAction));
            menu.add(new JMenuItem(_viewer._actions.addTargetListAction));

            menu.addSeparator();

            for (AbstractAction action : _viewer._actions.addInstrumentActions) {
                menu.add(new JMenuItem(action));
            }

            menu.addSeparator();

            for (AbstractAction action : _viewer._actions.addAOActions) {
                menu.add(new JMenuItem(action));
            }

            if (OTOptions.isStaffGlobally()) {
                for (AbstractAction action : _viewer._actions.addEngineeringActions) {
                    menu.add(new JMenuItem(action));
                }
            }

            _componentMenuButton = makeMenuButton("Create an observation component.", menu);

        }

        updateButton(_componentMenuButton,
                     "Component",
                     Resources.getIcon("componentMenu.gif"));

        return _componentMenuButton;
    }

    /**
     * Make the Note button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the Note button
     */
    protected JButton makeNoteMenuButton() {
        if (_noteMenuButton == null) {
            JPopupMenu menu = new JPopupMenu();
            for (AbstractAction action : _viewer._actions.addNoteActions) {
                menu.add(new JMenuItem(action));
            }
            _noteMenuButton = makeMenuButton("Create a Note.", menu);
        }

        updateButton(_noteMenuButton, "Note",
                Resources.getIcon("post-it-note-menu18.gif"));

        return _noteMenuButton;

    }

    /**
     * Make the LibFolder button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the LibFolder button
     *
     protected JButton makeLibFolderButton() {
     if (_libFolderButton == null)
     _libFolderButton = makeButton("Create a library folder.", _viewer.getLibFolderAction(), false);

     updateButton(_libFolderButton,
     "Library",
     Resources.getIcon("libFolder.gif"));
     return _libFolderButton;
     }
     */

    /**
     * Make the Iterator Component menu button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the Iterator Component menu button
     */
    protected JButton makeIterCompMenuButton() {
        if (_iterCompMenuButton == null) {
            JPopupMenu menu = new JPopupMenu();
            menu.add(new JMenuItem(_viewer._actions.addSequenceAction));
            menu.addSeparator();
            for (AbstractAction action : _viewer._actions.addInstrumentIteratorActions) {
                menu.add(new JMenuItem(action));
            }
            _iterCompMenuButton = makeMenuButton("Create an iterator component.", menu);
        }

        updateButton(_iterCompMenuButton,
                     "Iterator",
                     Resources.getIcon("iterCompMenu.gif"));
        return _iterCompMenuButton;
    }

    /**
     * Make the Observe Iterator menu button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the Observe Iterator menu button.
     */
    protected JButton makeIterObsMenuButton() {
        if (_iterObsMenuButton == null) {
            JPopupMenu menu = new JPopupMenu();
            for (AbstractAction action : _viewer._actions.addGenericSeqCompActions) {
                menu.add(new JMenuItem(action));
            }
            _iterObsMenuButton = makeMenuButton("Create an observation iterator.", menu);
        }

        updateButton(_iterObsMenuButton,
                     "Observe",
                     Resources.getIcon("iterObsMenu.gif"));
        return _iterObsMenuButton;
    }


    /**
     * Update the toolbar display using the current text/pictures options.
     * (redefined from the parent class).
     */
    public void update() {
        makeObservationMenuButton();
        makeObservationGroupButton();
        makeComponentMenuButton();
        makeNoteMenuButton();
        makeIterCompMenuButton();
        makeIterObsMenuButton();
    }
}

