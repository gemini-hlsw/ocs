package jsky.app.ot.viewer;

import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.Problem;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.sp.vcs.reg.VcsRegistrar;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.obs.ObsPhase2Status;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.obscomp.SPNote;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.obslog.ObsQaLog;
import edu.gemini.spModel.util.SPTreeUtil;
import jsky.app.ot.OT;
import jsky.app.ot.OTOptions;
import jsky.app.ot.ags.BagsManager;
import jsky.app.ot.editor.EdObsGroup;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.editor.eng.EngEditor;
import jsky.app.ot.editor.eng.EngToolWindow;
import jsky.app.ot.editor.seq.EdIteratorFolder;
import jsky.app.ot.gemini.editor.EdProgram;
import jsky.app.ot.nsp.UIInfo;
import jsky.app.ot.plugin.OtActionPlugin;
import jsky.app.ot.tpe.TelescopePosEditor;
import jsky.app.ot.tpe.TpeManager;
import jsky.app.ot.ui.util.UIConstants;
import jsky.app.ot.util.History;
import jsky.app.ot.util.PropertyChangeMultiplexer;
import jsky.util.gui.Resources;
import jsky.app.ot.util.RootEntry;
import jsky.app.ot.vcs.VcsStateTracker;
import jsky.app.ot.vcs.SyncAllDialog;
import jsky.app.ot.vcs.VcsOtClient;
import jsky.app.ot.viewer.plugin.PluginConsumer;
import jsky.app.ot.viewer.plugin.PluginRegistry;
import jsky.util.gui.BusyWin;
import jsky.util.gui.DialogUtil;
import org.noos.xing.mydoggy.DockedTypeDescriptor;
import org.noos.xing.mydoggy.ToolWindow;
import org.noos.xing.mydoggy.ToolWindowManager;
import org.noos.xing.mydoggy.ToolWindowType;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A widget for viewing OT science programs.
 * @author Allan Brighton
 */
public final class SPViewer extends SPViewerGUI implements PropertyChangeListener, PluginConsumer {
    private static final Logger LOG = Logger.getLogger(SPViewer.class.getName());

    // List of SPViewer instances
    private static final LinkedList<SPViewer> _orderedInstances = new LinkedList<>();

    // Hmm
    private final SPEventManager _eventManager;

    // Reference to the current item editor
    private OtItemEditor<ISPNode, ISPDataObject> _currentEditor;

    // Used to cache item editor windows for a given class
    private final Hashtable<Class<?>, OtItemEditor<ISPNode, ISPDataObject>> _editorTable = new Hashtable<>();

    // Listens for changes in the editable state (from the OT)
    private final OT.EditableStateListener _editableStateListener;

    // Used to show the problems (if any) found on the program nodes
    private final SPProblemsViewer _problemViewer = new SPProblemsViewer(this);
    private final SPConflictToolWindow _conflictPanel = new SPConflictToolWindow(this::updateConflictToolWindow, this, getTree());


    private final EngToolWindow _engToolWindow = new EngToolWindow();

    private final VcsStateTracker _vcsStateTracker = new VcsStateTracker();


    private final IDBDatabaseService _db;

    public final SPViewerActions _actions;

    // Listener that rebuilds the menu, toolbars, and editor in response to changes to stuff that can cause the
    // authorization situation to change. Right now this means any manipulation of the keychain and any change to
    // the ISPProgram's data object.
    public final PropertyChangeListener authListener = evt -> {
        rebuildMenusAndToolbars();
        _updateFrameTitle();
    };

    private final Map<SPNodeKey, SPTree.StateSnapshot> treeSnapshots = new HashMap<>();

    private final P2CheckerCowboy _checker = new P2CheckerCowboy();

    public static List<SPViewer> instances() {
        return new ArrayList<>(_orderedInstances);
    }

    // Zipper for our history
    private  History _history;

    /** Constructor, called by ViewerManager */
    SPViewer(final IDBDatabaseService db) {

        _db = db;
        _history = new History(_db);

        // Our actions are sliced off into another object, to keep things
        // a bit more organized.
        _actions = new SPViewerActions(this);
        ViewerService.instance().get().registerView(this);

        // Monitor change events
        final PropertyChangeMultiplexer _relay = new PropertyChangeMultiplexer();
        _relay.addPropertyChangeListener(this);
        _eventManager = new SPEventManager(_relay);

        getTree().setViewer(this);

        // handle tree selections
        getTree().addSPTreeListener(event -> {
            if (getRoot() != null) {
                // show some selection feedback right away
                getTree().paintImmediately(getTree().getTree().getBounds());
                _treeNodeSelected(event.getViewable());
            }
        });

        // setup the tree node popup menus
        final JPopupMenu _nodeMenu = _makeNodeMenu();
        getTree().getTree().addMouseListener(new MouseAdapter() {
            public void mousePressed(final MouseEvent e) {
                if (e.isPopupTrigger()) {
                    _nodeMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            public void mouseReleased(final MouseEvent e) {
                if (e.isPopupTrigger()) {
                    _nodeMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        _orderedInstances.add(this);
        PluginRegistry.registerConsumer(this);

        // arrange to be notified when the OT editable state changes
        _editableStateListener = new OT.EditableStateListener() {
            @Override public ISPNode getEditedNode() { return getRoot(); }
            @Override public void updateEditableState() {
                if (getRoot() != null) {
                    try {
                        _updateEditableState();
                    } catch (Exception e) {
                        DialogUtil.error(e);
                    }
                }
            }
        };
        OT.addEditableStateListener(_editableStateListener);
    }

    private void rebuildMenusAndToolbars() {
        SwingUtilities.invokeLater(() -> {
            try {
                getParentFrame().rebuildMainContent();
                _showEditor();
                _updateListeners();
            } catch (Exception ex) {
                DialogUtil.error(ex);
            }
        });
    }

    public IDBDatabaseService getDatabase() {
        return _db;
    }

    public ISPFactory getFactory() {
        return getDatabase().getFactory();
    }

    public VcsStateTracker getVcsStateTracker() {
        return _vcsStateTracker;
    }

    // Called when a tree node is selected.
    private void _treeNodeSelected(final NodeData viewable) {
        if (getRoot() != null) {  // TODO: is this even possible?
            try {
                // Ok, add this to the history and preen deleted nodes.
                _history = _history.go(viewable.getNode()).preen();
                getParentFrame().rebuildNavMenu();

                final ISPNode node = getNode();

                //Update the problem viewer with the new selected node, if the engine is turned on
                if (OTOptions.isCheckingEngineEnabled()) {
                    _problemViewer.setNodeData(viewable);
                }
                _conflictPanel.setNode(node);
                updateConflictToolWindow();

                _resetPositionEditor();
                _showEditor();
                _updateListeners();
                OT.updateEditableState(getRoot());
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    }

    /** Make and return a popup menu for tree nodes */
    private JPopupMenu _makeNodeMenu() {
        final JPopupMenu menu = new JPopupMenu();

        // Template submenu
        final JMenu _templateMenu = new JMenu("Template");
        _templateMenu.setIcon(Resources.getIcon("template.gif"));
        for (final Action a : _actions.templateActions)
            _templateMenu.add(new JMenuItem(a));
        menu.add(_templateMenu);
        menu.addSeparator();

        // Observation menu
        final JMenu obsMenu = new JMenu("Create an Observation");
        obsMenu.setIcon(Resources.getIcon("observation.gif"));
        for (final AbstractAction action : _actions.addObservationActions) {
            obsMenu.add(new JMenuItem(action));
        }
        menu.add(obsMenu);

        // Group item
        final JMenu groupMenu = new JMenu("Create a Group");
        groupMenu.setIcon(Resources.getIcon("obsGroup.gif"));
        for (final Action action : _actions.addGroupActions) {
            groupMenu.add(new JMenuItem(action));
        }
        menu.add(groupMenu);

        // Note menu
        final JMenu noteMenu = new JMenu("Create a Note");
        noteMenu.setIcon(Resources.getIcon("post-it-note18.gif"));
        for (final Action action : _actions.addNoteActions) {
            noteMenu.add(new JMenuItem(action));
        }
        menu.add(noteMenu);

        // Observation Component submenu
        final JMenu compMenu = new JMenu("Create an Observation Component");
        compMenu.setIcon(Resources.getIcon("component.gif"));
        compMenu.add(new JMenuItem(_actions.addSiteQualityAction));
        compMenu.add(new JMenuItem(_actions.addTargetListAction));
        compMenu.addSeparator();
        for (final Action action : _actions.addInstrumentActions) {
            compMenu.add(new JMenuItem(action));
        }

        compMenu.addSeparator();

        for (final AbstractAction action : _actions.addAOActions) {
            compMenu.add(new JMenuItem(action));
        }
        menu.add(compMenu);

        for (final AbstractAction action : _actions.addEngineeringActions) {
            compMenu.add(new JMenuItem(action));
        }
        menu.add(compMenu);

        // Iterator Component submenu
        final JMenu iterCompMenu = new JMenu("Create an Iterator Component");
        iterCompMenu.setIcon(Resources.getIcon("iterComp.gif"));
        iterCompMenu.add(new JMenuItem(_actions.addSequenceAction));

        iterCompMenu.addSeparator();

        for (final AbstractAction action : _actions.addInstrumentIteratorActions) {
            iterCompMenu.add(new JMenuItem(action));
        }
        menu.add(iterCompMenu);

        // Observation Iterator submenu
        final JMenu iterObsMenu = new JMenu("Create an Observe Iterator");
        iterObsMenu.setIcon(Resources.getIcon("iterObs.gif"));
        for (final AbstractAction action : _actions.addGenericSeqCompActions) {
            iterObsMenu.add(new JMenuItem(action));
        }
        menu.add(iterObsMenu);

        menu.addSeparator();
        menu.add(_actions.cutAction);
        menu.add(_actions.copyAction);
        menu.add(_actions.pasteAction);
        menu.addSeparator();
        menu.add(_actions.moveToTopAction);
        menu.add(_actions.moveUpAction);
        menu.add(_actions.moveDownAction);
        menu.add(_actions.moveToBottomAction);
        menu.addSeparator();
        menu.add(_actions.editItemTitleAction);
        menu.add(_actions.setPhase2StatusAction);
        menu.add(_actions.setExecStatusAction);
        menu.addSeparator();
        menu.add(_actions.expandObsAction);
        menu.add(_actions.collapseObsAction);
        menu.addSeparator();
        menu.add(_actions.expandProgAction);
        menu.add(_actions.collapseProgAction);

        return menu;
    }

    // SW: Matching the behavior of the old feature that allows the user to set the status of multiple observations at
    // once.  The UI interaction is bizarre though since it is only the observation status that behaves this way.  The
    // correct thing to do would be a separate control or, even better, implement editing multiple items at once in
    // general in the way that iTunes edits multiple song attributes -- show the values of matching attributes and
    // blanks where there are differences.
    public void setPhase2Status(final ObsPhase2Status status) {
        final ISPObservation[] os = getSelectedObservations();
        if ((os == null) || (os.length == 0)) return;
        for (final ISPObservation o : os) {
            final SPObservation obj = (SPObservation) o.getDataObject();
            obj.setPhase2Status(status);
            o.setDataObject(obj);
        }
    }

    // Return an array of observation status choices for the given observation, based on the user's role.
    public ObsPhase2Status[] getObsStatusChoices(final ISPObservation[] obs) {
        // TODO: this logic probably doesn't make sense
        final ObsPhase2Status[] result;
        final SPProgramID pid = obs[0].getProgramID();
        result = SPObservation.getObservationStatusChoices(obs[0], OTOptions.isPI(pid), OTOptions.isNGO(pid), OTOptions.isStaff(pid));
        // Check that user is allowed to change all of these observations status values
        for (final ISPObservation o : obs) {
            final SPObservation spObs = (SPObservation) o.getDataObject();
            final ObsPhase2Status status = spObs.getPhase2Status();
            if (!_checkObservationStatus(result, status)) return null;
        }
        return result;
    }

    // True if choices contains status.
    private  boolean _checkObservationStatus(final ObsPhase2Status[] choices, final ObsPhase2Status status) {
        for (final ObsPhase2Status cur : choices) {
            if (cur == status) {
                return true;
            }
        }
        return false;
    }

    /** Return the root of the tree, if it is a science program, otherwise null. */
    public ISPProgram getProgram() {
        return getRoot();
    }

    /** Set the top level parent frame (or internal frame) used to close the window */
    public void setParentFrame(final SPViewerFrame f) {
        super.setParentFrame(f);
        _updateFrameTitle();
    }

    /** Make sure this window's frame is visible. */
    public void showParentFrame() {
        final Frame frame = getParentFrame();
        frame.setVisible(true);
        if (frame.getState() == Frame.ICONIFIED)
            frame.setState(Frame.NORMAL);
        frame.toFront();
    }

    /** Display the item editor for the currently selected node. */
    @SuppressWarnings("unchecked")
    private void _showEditor()
            throws ClassNotFoundException,
            InstantiationException,
            IllegalAccessException {

        if (getCurrentEditor() != null) {
            getContentPresentation().remove(getCurrentEditor().getWindow());
        }

        // This method can be called as a result of manipulating keys even
        // when there is no open program.  In that case, there is no viewable.
        // If there is no selected node, there's no editor to display either.
        final NodeData viewable = getTree().getViewable();
        if (viewable == null) return;

        final UIInfo uiInfo = viewable.getUIInfo();
        final String className = uiInfo.getUIClassName();

        final Class<OtItemEditor<ISPNode, ISPDataObject>> c = (Class<OtItemEditor<ISPNode, ISPDataObject>>)Class.forName(className);
        _currentEditor = _getEditor(c);

        // Show the engineering component for this editor (if any)
        Component eng = null;
        if (getCurrentEditor() instanceof EngEditor) {
            eng = ((EngEditor) getCurrentEditor()).getEngineeringComponent();
        }
        updateEngToolWindow(eng);

        getContentPresentation().add(getCurrentEditor().getWindow());
        getCurrentEditor().init(PropagationId.EMPTY, getNode());

        getTitledBorder().setTitle(uiInfo.getDisplayName());
        getDescriptionBox().setText(uiInfo.getShortDescription());
        getContentPresentation().revalidate();
        repaint();
    }

    /** Reset the position editor, if it exists. */
    private void _resetPositionEditor() {
        final TelescopePosEditor tpe = TpeManager.get();
        if (tpe != null) tpe.reset(getNode());
    }

    public void updateAfterPermissionsChange() {
        _updateEditableState();
    }

    /** Update the editable state (based on the Edit toolbar button and the observation status settings. */
    private void _updateEditableState() {
        _actions._updateEnabledStates();
    }

    /** Return an editor window for the given class, creating one if necessary */
    private OtItemEditor<ISPNode, ISPDataObject> _getEditor(final Class<OtItemEditor<ISPNode, ISPDataObject>> c) throws InstantiationException, IllegalAccessException {
        final OtItemEditor<ISPNode, ISPDataObject> ed = _editorTable.get(c);
        if (ed == null) {
            final OtItemEditor<ISPNode, ISPDataObject> newEditor = c.newInstance();
            _editorTable.put(c, newEditor);
            return newEditor;
        } else {
            return ed;
        }
    }

    /** Called from notify() whenever a node's data object is modified */
    private void _dataObjectChanged(final PropagationId propId, final ISPNode sourceNode, final ISPDataObject oldValue, final ISPDataObject newValue) {
        if (getRoot() != null) { // can this ever be non-null?

            final ISPProgram prog = getProgram();
            final ISPNode currentNode;

            if (prog != null) {
                currentNode = getTree().getCurrentNode();
            } else {
                currentNode = getTree().getSelectedNode();
            }

            getTree().dataObjectChanged(sourceNode, oldValue, newValue);

            // check if the change affects the currently displayed editor
            if (getCurrentEditor() != null && sourceNode != null && newValue != null
                    && currentNode != null && sourceNode.equals(currentNode)) {
                // update the current editor
                getCurrentEditor().reinitialize(propId);
            } else if (getCurrentEditor() instanceof EdIteratorFolder) {
                // The sequence editor is being displayed
                if (sourceNode instanceof ISPSeqComponent) {
                    // the sequence display depends on the sequence nodes, so update it
                    getCurrentEditor().reinitialize(propId);
                } else if (sourceNode instanceof ISPObsComponent) {
                    // the sequence display also depends on the instrument node, check for that
                    final SPComponentType type = ((ISPObsComponent) sourceNode).getType();
                    final SPComponentBroadType broadType = type.broadType;
                    if (broadType.equals(SPComponentBroadType.INSTRUMENT)) {
                        getCurrentEditor().reinitialize(propId);
                    }
                }
            } else if (getCurrentEditor() instanceof EdProgram || getCurrentEditor() instanceof EdObsGroup) {
                // The program and group node editors may need to update the total planned time display,
                // which is based on the contained observations.
                getCurrentEditor().reinitialize(propId);
            }

            // Update the window title if the program data object changed.
            if (sourceNode instanceof ISPProgram) {
                _updateFrameTitle();
            }

            _updateListeners();
            updateConflictToolWindow();
        }
    }

    /** Called from notify() whenever the SP tree structure is modified */
    private void _treeStructureChanged(final PropagationId propId, final ISPNode modifiedNode, final List<Object> oldValue, final List<Object> newValue) {

        BusyWin.showBusy();

        final ISPNode currentNode = getTree().getCurrentNode();
        getTree().treeStructureChanged(modifiedNode);

        // determine if nodes were added, removed, or just rearrangedhttp://www.grantland.com/story/_/id/7887639/looking-back-hunter-s-thompson-classic-story-kentucky-derby
        final int oldSize = oldValue.size();
        final int newSize = newValue.size();
        // find out which nodes were added or removed
        final List<Object> changedList;
        boolean added = false;
        boolean moved = false;
        if (newSize > oldSize) {
            // nodes were added
            changedList = new ArrayList<>(newValue);
            changedList.removeAll(oldValue);
            added = true;
        } else if (newSize < oldSize) {
            // nodes were removed
            changedList = new ArrayList<>(oldValue);
            changedList.removeAll(newValue);
        } else {
            // nodes were rearranged
            changedList = new ArrayList<>(newValue);
            moved = true;
        }

        if (!moved) {
            final boolean expandAndSelect = (changedList.size() == 1);
            for (final Object aChangedList : changedList) {
                final ISPNode node = (ISPNode) aChangedList;
                if (added) {

                    //check if the new node is already checked. If not,
                    //perform a new check on it
                    final DefaultMutableTreeNode treeNode = getTree().getTreeNode(node);
                    if (OTOptions.isCheckingEngineEnabled() && (treeNode != null) && (node instanceof ISPProgramNode)) {
                        final NodeData viewable = (NodeData) treeNode.getUserObject();
                        if ((viewable != null) && !viewable.isCheckedForProblems()) {
                            _checker.check(node, getTree(), OT.getMagnitudeTable());
                        }
                    }

                    // if a node was added, make sure it is visible (expand the tree).
                    // Note: need to do this later to avoid problems with data object being
                    // null when the node is selected.
                    if (expandAndSelect) {
                        SwingUtilities.invokeLater(() -> {
                            try {
                                getTree().setSelectedNode(node);
                                getTree().expandNode(node);
                            } catch (Exception e) {
                                e.printStackTrace(); // RCN: worth a shot...
                                //do nothing
                            }
                        });
                    }
                } else {
                    // if a node was removed, check if it is the currently selected one, and if so,
                    // select the parent of the deleted node
                    if (node.equals(currentNode) ||
                            SPTreeUtil.nodeContainsNode(node, currentNode)) {
                        getTree().setSelectedNode(modifiedNode);
                    }
                }
            }
        }

        if (getCurrentEditor() instanceof EdIteratorFolder) {
            // the sequence display depends on the sequence nodes, so update it
            getCurrentEditor().reinitialize(propId);
        }

        _actions._updateEnabledStates();
    }

    /** PropertyChange interface: called whenever something in the current editor is changed */
    public void propertyChange(final PropertyChangeEvent value) {
        if (getRoot() != null) { // can this ever not be the case?
            try {
                final PropagationId propId = (value.getPropagationId() instanceof PropagationId) ? (PropagationId) value.getPropagationId() : PropagationId.EMPTY;
                if (value instanceof SPCompositeChange) {
                    final SPCompositeChange spcc = (SPCompositeChange) value;
                    final String prop = spcc.getPropertyName();
                    if (prop.equals(SPUtil.getDataObjectPropertyName())) {
                        // a node's data object changed
                        _dataObjectChanged(propId, spcc.getModifiedNode(),
                                (ISPDataObject) spcc.getOldValue(),
                                (ISPDataObject) spcc.getNewValue());
                        //Recheck the node, since the data objects have changed
                        _notifyChecker(spcc.getModifiedNode(), spcc.getNewValue());
                    } else if (prop.equals(ISPProgram.PROGRAM_ID_PROP)) {
                        // the prog id changed: update the display
                        getCurrentEditor().reinitialize(propId);
                        _updateFrameTitle();
                    } else if (prop.equals(ISPNode.EVENTS_ACTIVATED)) {
                        setRootNode(getRoot());
                    }
                } else if (value instanceof SPNestedChange) {
                    final SPNestedChange spnc = (SPNestedChange) value;
                    _treeStructureChanged(propId, spnc.getModifiedNode(),
                            _getList(spnc.getOldValue()),
                            _getList(spnc.getNewValue()));
                    if (value instanceof SPStructureChange) {
                        final SPStructureChange change = (SPStructureChange) value;
                        _notifyChecker(change.getModifiedNode(), null);
                    }
                } else {
                    apply();
                }
            } catch (Exception ex) {
                DialogUtil.error(ex);
            }
        }
    }

    private void _notifyChecker(final ISPNode nodeChanged, final Object dataObj) {
        if (OTOptions.isCheckingEngineEnabled()) {
            // Check for changes that don't impact P2 checks. Changes to the obslog
            // and notes don't have to generate p2 checks.  We should probably
            // create a marker interface for these nodes.  IP2CheckInnocuous
            if (dataObj instanceof ObsQaLog) return;
            if (dataObj instanceof ObsExecLog) return;
            if (dataObj instanceof SPNote) return;

            _checker.check(nodeChanged, getTree(), OT.getMagnitudeTable()); // REL-337

            //update the problem viewer window
            _problemViewer.update();
        }
    }

    /**
     * If the given object is not a List object, return a list containing it, otherwise return the object. If the
     * argument is null, return an empty list.
     */
    @SuppressWarnings("unchecked")
    private static List<Object> _getList(final Object o) {
        if (o == null) {
            return new ArrayList<>();
        }
        if (!(o instanceof List)) {
            final List<Object> l = new ArrayList<>();
            //noinspection unchecked
            l.add(o);
            return l;
        } else {
            return (List<Object>) o;
        }
    }

    /**
     * Set the root node to display in this viewer, or null to clear the viewer. In general you should use
     * SPViewer.tryNavigate or ViewerManager.open, but calling this directly can be useful because it forces a complete
     * redraw of the viewer. This is not ideal.
     */
    public void setRootNode(final ISPProgram root) {
        try {
            // Reset the TPE to the new root
            final TelescopePosEditor tpe = TpeManager.get();
            if (tpe != null) tpe.reset(root);

            // If there was an old root, clean up
            if (getRoot() != null) {
                getDatabase().checkpoint();
                getRoot().removePropertyChangeListener(ISPProgram.DATA_OBJECT_KEY, authListener);
                updateEngToolWindow(null);
            }

            // Track VCS changes [only] in the current root
            _vcsStateTracker.setProgram(root);

            // clear the current editor when the root node is assigned since
            // the editor caches a node which will be from the previous root
            if (_currentEditor != null) {
                getContentPresentation().remove(_currentEditor.getWindow());
                _currentEditor = null;
            }

            // catch prog id changes
            _eventManager.setRootNode(root);

            // Tell the tree what's up
            final ISPProgram oldRoot = getTree().getRoot();
            if ((oldRoot != null) && (_history.findOrNull(oldRoot) != null)) {
                treeSnapshots.put(oldRoot.getNodeKey(), getTree().snapshot());
            }
            final SPTree.StateSnapshot ss = (root == null) ? SPTree.StateSnapshot.empty() : treeSnapshots.get(root.getNodeKey());
            getTree().setRoot(root, ss);

            if (root == null) {
                // Clear everything out. TODO: this is pretty terrible
                treeSnapshots.clear();
                getTitledBorder().setTitle("No current science program");
                getDescriptionBox().setText("");
                getContentPresentation().revalidate();
                _problemViewer.setNodeData(null);
                _conflictPanel.setNode(null);
                updateConflictToolWindow();
                repaint();
            } else {
                // Watch for changes to the program data object
                root.addPropertyChangeListener(ISPProgram.DATA_OBJECT_KEY, authListener);

                if (getRoot() != null && OTOptions.isCheckingEngineEnabled()) {
                    _checker.check(getRoot(), getTree(), OT.getMagnitudeTable());
                }
                BagsManager.watch(getRoot());
            }

            // Finally, update title and actions
            _updateFrameTitle();
            _actions._updateEnabledStates();

            // Tree selection (soon!) will cause an editor to be created, and will update our nav history.
        } catch (final Exception e) {
            DialogUtil.error(e);
        }
    }

    /** Replace the root node with a new version of the same program. */
    public void replaceRoot(final ISPProgram newRoot) {
        final SPTreeState s = SPTreeState.apply(getTree().getTree());
        setRootNode(newRoot);
        s.restore(getTree().getTree());
    }

    private void updateEngToolWindow(final Component comp) {
        SwingUtilities.invokeLater(() -> {
            final SPViewerFrame f = getParentFrame();
            final ToolWindowManager twm = f.getToolWindowManager();
            final ToolWindow win = twm.getToolWindow(EngToolWindow.ENG_TOOL_WINDOW_KEY);
            if (win == null) return;

            boolean engVisible = false;
            if (win.isAvailable()) engVisible = win.isVisible();
            win.setAvailable(comp != null);

            if (comp != null) {
                _engToolWindow.setComponent(comp);

                final DockedTypeDescriptor dtd;
                dtd = (DockedTypeDescriptor) win.getTypeDescriptor(ToolWindowType.DOCKED);
                dtd.setDockLength(comp.getPreferredSize().width + 30);
                win.setVisible(engVisible);
            }
        });
    }

    private boolean visible = false; //to have control of the last state selected by the user for the problem window

    //Update the ToolWindow that contains the problems details, if possible
    public void updateProblemToolWindow(final IP2Problems problems) {
        SwingUtilities.invokeLater(() -> {
            final SPViewerFrame f = getParentFrame();
            final ToolWindowManager toolWindowManager = f.getToolWindowManager();
            final ToolWindow problemToolWindow =
                    toolWindowManager.getToolWindow(SPViewerFrame.PROBLEM_TOOLWINDOW_KEY);

            if (problemToolWindow != null) {
                if (problemToolWindow.isAvailable()) {
                    visible = problemToolWindow.isVisible();
                }

                boolean available = false;
                Icon icon = null;
                if (problems != null) {
                    final Problem.Type type = problems.getSeverity();
                    switch (type) {
                        case ERROR:
                            available = true;
                            icon = UIConstants.ERROR_ICON;
                            break;
                        case WARNING:
                            available = true;
                            icon = UIConstants.WARNING_ICON;
                            break;
                    }
                }
                problemToolWindow.setIcon(icon);
                problemToolWindow.setAvailable(available);
                if (available) problemToolWindow.setVisible(visible);
            }
        });
    }

    public void updateConflictToolWindow() {
        final SPViewerFrame f = getParentFrame();
        final ToolWindow win = f.getToolWindowManager().getToolWindow(SPViewerFrame.CONFLICT_TOOLWINDOW_KEY);
        if (win != null) {
            final boolean available = _conflictPanel.hasEditableConflicts();
            win.setAvailable(available);
            final DockedTypeDescriptor dtd;
            dtd = (DockedTypeDescriptor) win.getTypeDescriptor(ToolWindowType.DOCKED);
            dtd.setDockLength(_conflictPanel.getPreferredSize().width);
            win.setVisible(win.isAvailable());
        }
    }

    /** Update the title of the frame to include information about the file name. */
    private void _updateFrameTitle() {
        final JFrame parent = getParentFrame();
        String title = "Gemini OT";
        if (getRoot() != null) {
            title += " - " + getTitle();
        }

        parent.setTitle(title);
    }

    /** Update the editor listeners after a change in the data object */
    private void _updateListeners() {
        if (getCurrentEditor() != null) {
            // track changes in editors
            final ISPDataObject o = getCurrentEditor().getDataObject();
            if (o != null) {
                o.removePropertyChangeListener(this); // make sure we don't add it twice
                o.addPropertyChangeListener(this);
            }

            // track changes caused by manipulating image instrument features on the image
            final SPInstObsComp inst = getCurrentEditor().getContextInstrumentDataObject();
            if (inst != null && inst != o) {
                inst.removePropertyChangeListener(this); // don't add it twice
                inst.addPropertyChangeListener(this);
            }
        }
    }

    /** Called for the Save button (Used to be the Apply button) */
    public void apply() {
        try {
            if (getCurrentEditor() != null)
                getCurrentEditor().apply();

            // Update enabled states but filter out events that shouldn't
            // impact it.
            final AWTEvent e = EventQueue.getCurrentEvent();
            if (!(e instanceof KeyEvent)) {
                _actions._updateEnabledStates();
            }
        } catch (final Exception e) {
            DialogUtil.error(e);
        }
    }

    private boolean shouldClose(ISPProgram prog) {
        return shouldClose(Collections.singletonList(prog));
    }

    private boolean shouldClose(List<ISPProgram> progs) {
        if (VcsOtClient.ref().isEmpty()) return true;
        else {
            final VcsRegistrar reg = VcsOtClient.unsafeGetRegistrar();
            return SyncAllDialog.shouldClose(this, OT.getKeyChain(), reg, _db, progs);
        }
    }


    /** Closes the window, releases its resources and quits the application if it is the last main window. */
    public void closeWindow() {
        if (shouldClose(programs())) {
            closeViewer();
        }
    }

    public void closeProgram() {
        final ISPProgram p = _history.rootOrNull();
        if ((p != null) && !shouldClose(p)) {
            return;
        }
        if (p != null) {
            BagsManager.unwatch(p);
            treeSnapshots.remove(p.getNodeKey());
        }
        tryNavigate(_history.delete());
    }

    public void closeProgram(ISPProgram node) {
        if (node != null) {
            BagsManager.unwatch(node);
            treeSnapshots.remove(node.getNodeKey());
        }
        tryNavigate(_history.delete(node));
    }

    /** Close the current viewer window, but don't exit the application, even if it is the last window. */
    private void closeViewer() {
        if (getRoot() != null) {
            for (RootEntry e : _history.rootEntriesAsJava()) {
                BagsManager.unwatch(e.root());
            }
        }

        tryNavigate(_history.empty());
        treeSnapshots.clear();
        try {
            SwingUtilities.getWindowAncestor(this).dispose();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Couldn't close viewer window", ex);
        }

        // Remove this window from the list of available viewers. Note that it could get confusing to try to reuse this
        // instance at some point in the future, since _frameCount has already counted it off and the _relay listeners
        // have been cleared.
        _orderedInstances.remove(this);
        PluginRegistry.unregisterConsumer(this);

        OT.removeEditableStateListener(_editableStateListener);

        _actions.close();

        // NOTE: this will exit the application in case this is the last view (no other SPViewer or plugin etc)
        ViewerService.instance().get().unregisterView(this);
    }

    /** Exit the application. */
    public void exit() {
        if (!shouldClose(allPrograms())) {
            return;
        }
        instances().forEach(SPViewer::closeViewer);

        // TODO: Potentially there are still plugins an other parts of the application open.
        System.exit(0);
    }

    private List<ISPProgram> programs() {
        final List<ISPProgram> progs = new ArrayList<>();
        _history.rootEntriesAsJava().forEach(re -> progs.add(re.root()));
        return progs;
    }

    private static List<ISPProgram> allPrograms() {
        final List<ISPProgram> programs = new ArrayList<>();
        _orderedInstances.forEach(v -> programs.addAll(v.programs()));
        return programs;
    }

    // Enable/Disable the listeners on the science program
    public void setProgramListenersEnabled(final boolean enabled) {
        _eventManager.setRootNode(enabled ? getRoot() : null);
    }

    // Return an array of selected observations
    public ISPObservation[] getSelectedObservations() {
        final ISPProgram prog = getProgram();
        if (prog == null) {
            return null;
        }
        final ISPNode[] rnodes = getTree().getSelectedNodes();
        final Set<ISPObservation> set = new HashSet<>();
        if (rnodes == null) {
            final ISPNode n = getTree().getCurrentNode();
            final ISPObservation obs = (n != null) ? n.getContextObservation() : null;
            if (obs != null) {
                set.add(obs);
            }
        } else {
            for (final ISPNode node : rnodes) {
                if (node instanceof ISPObservation) {
                    set.add((ISPObservation) node);
                } else if (node instanceof ISPProgram) {
                    set.addAll(prog.getAllObservations());
                    break;
                } else if (node instanceof ISPGroup) {
                    set.addAll(((ISPObservationContainer) node).getObservations());
                } else {
                    final ISPObservation o = node.getContextObservation();
                    if (o != null) set.add(o);
                }
            }
        }
        final ISPObservation[] ar = new ISPObservation[set.size()];
        set.toArray(ar);
        return ar;
    }

    /** Returns the ProblemViewer where the problems found on nodes can be displayed. */
    SPProblemsViewer getProblemsViewer() {
        return _problemViewer;
    }

    public SPConflictToolWindow getConflictToolWindow() {
        return _conflictPanel;
    }

    /** Returns the {@link EngToolWindow} for this viewer. */
    public EngToolWindow getEngToolWindow() {
        return _engToolWindow;
    }

    /** Return the title of the current program */
    private String getTitle() {
        // Include the program id, if not the default "sp..."
        final SPProgramID progId = getRoot().getProgramID();
        String progIdStr = "";
        if (progId != null) {
            progIdStr = "[" + progId.stringValue() + "] ";
        }

        final ISPProgram prog = getProgram();
        if (prog != null) {
            try {
                return progIdStr + prog.getDataObject().getTitle();
            } catch (Exception e) {
                return "";
            }
        }
        return progIdStr;
    }

    /** Checks the entire program looking for potential problems. */
    public void checkCurrentProgram() {
        if (getRoot() != null) {
            _checker.check(getRoot(), getTree(), OT.getMagnitudeTable());
            getTree().repaint();
            //set the problem viewer to watch the current selected node
            _problemViewer.setNodeData(getTree().getViewable());
        }
    }

    public void clearProblemInformation() {
        getTree().repaint(); //update the UI
        _problemViewer.setNodeData(null); //deactivate the problem window...
    }

    public ISPProgram getRoot() {
        return getTree().getRoot();
    }

    @SuppressWarnings("rawtypes") // This isn't very nice but changing it implies checking the OtItemEditor hierarchy
    public OtItemEditor getCurrentEditor() {
        return _currentEditor;
    }

    public ISPObservation getContextObservation() {
        return (_currentEditor == null) ? null : _currentEditor.getContextObservation();
    }

    public ISPNode getNode() {
        return getTree().getSelectedNode();
    }

    // Return true if the array contains an observation, group, or program node
    boolean containsObservation(ISPNode[] rnodes) {
        for (ISPNode rnode : rnodes) {
            if (rnode instanceof ISPObservation
                    || rnode instanceof ISPGroup
                    || rnode instanceof ISPProgram) {
                return true;
            }
        }
        return false;
    }

    public History getHistory() {
        return _history;
    }

    /** Replace the current history (if the supplied history is non-null), causing navigation to the current focus. */
    public void tryNavigate(final History h) {
        if (h != null) {
            _history = h.preen(); // TODO: should we do this?
            final ISPProgram r = _history.rootOrNull(); // if null, returns the viewer to "closed" state

            // This causes lots of things to be reset, so only do it if necessary
            if (getRoot() != r) {
                setRootNode(r);
                _history = h.preen(); // _history reset by setRootNode :/
            }
            getTree().setSelectedNode(_history.nodeOrNull());
            getParentFrame().rebuildNavMenu();
        }
    }

    public void install(OtActionPlugin plugin) {
        getParentFrame().rebuildPluginMenu();
    }

    public void uninstall(OtActionPlugin plugin) {
        getParentFrame().rebuildPluginMenu();
    }
}
