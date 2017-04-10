// Copyright 1999-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SPTree.java 47000 2012-07-26 19:15:10Z swalker $
//

package jsky.app.ot.viewer;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obscomp.ProgramNote;
import edu.gemini.spModel.obscomp.SPGroup;
import edu.gemini.spModel.obscomp.SPGroup.GroupType;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.seqcomp.SeqRepeatObserve;
import edu.gemini.spModel.util.DBTreeListService;
import jsky.app.ot.OT;
import jsky.app.ot.OTOptions;
import jsky.app.ot.StaffBean;
import jsky.app.ot.nsp.SPTreeEditUtil;
import jsky.app.ot.nsp.UIInfo;
import jsky.app.ot.ui.util.UIConstants;
import jsky.util.StringUtil;
import jsky.util.gui.AutoscrollTree;
import jsky.util.gui.DialogUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;


/**
 * This is a component that displays a Science Program in a tree view.
 * It does not manipulate the program, it only listens to and
 * displays the program.
 */
public final class SPTree extends JPanel {

    static class StateSnapshot {
        public static StateSnapshot empty() {
            return new StateSnapshot(Collections.<SPNodeKey>emptyList(), Collections.<SPNodeKey, NodeData.State>emptyMap());
        }

        public static StateSnapshot initial(ISPProgram prog) {
            final SPNodeKey key = prog.getProgramKey();
            final Map<SPNodeKey, NodeData.State> m = new HashMap<SPNodeKey, NodeData.State>();
            m.put(key, NodeData.State.apply(true, null));
            return new StateSnapshot(Collections.singletonList(key), Collections.unmodifiableMap(m));
        }

        private final List<SPNodeKey> selectionPath;
        private final Map<SPNodeKey, NodeData.State> nodeState;

        private StateSnapshot(List<SPNodeKey> selectionPath, Map<SPNodeKey, NodeData.State> nodeState) {
            this.selectionPath = selectionPath;
            this.nodeState     = nodeState;
        }

        private void restore(SPTree tree) {
            restoreExpansion(tree._tree, (DefaultMutableTreeNode) tree.getModel().getRoot());
            restoreSelection(tree);
        }

        private void restoreExpansion(JTree tree, DefaultMutableTreeNode root) {
            final NodeData.State s = nodeState.get(key(root));
            final boolean open = (s != null) && s.open;
            if (open) {
                final TreePath path = new TreePath(root.getPath());
                if (!tree.isExpanded(path)) tree.expandPath(path);
                final Enumeration children = root.children();
                while (children.hasMoreElements()) {
                    restoreExpansion(tree, (DefaultMutableTreeNode) children.nextElement());
                }
            }
        }

        private void restoreSelection(SPTree tree) {
            tree.setSelectedNode(treeNode(tree._tree, selectionPath));
        }
    }

    private static SPNodeKey key(DefaultMutableTreeNode n) {
        return ((NodeData) n.getUserObject()).getNode().getNodeKey();
    }

    private static List<SPNodeKey> selectedNodePath(JTree tree) {
        final TreePath path  = tree.getSelectionPath();
        if (path == null) return Collections.emptyList();

        final Object[] elems = path.getPath();
        final List<SPNodeKey> keyPath = new ArrayList<SPNodeKey>();
        for (Object elem : elems) {
            DefaultMutableTreeNode tn = (DefaultMutableTreeNode) elem;
            keyPath.add(key(tn));
        }

        return Collections.unmodifiableList(keyPath);
    }

    private static List<SPNodeKey> nodePath(final ISPNode node) {
        // Build a stack containing the nodes from the root down to "node".
        final Deque<ISPNode> nodePath = new LinkedList<ISPNode>();
        ISPNode tmp = node;
        while (tmp != null) {
            nodePath.addFirst(tmp);
            tmp = tmp.getParent();
        }

        // Has this node been orphaned?
        final ISPNode root = nodePath.getFirst();
        if (!(root instanceof ISPRootNode)) return Collections.emptyList();

        // Map the path to node keys.
        final List<SPNodeKey> res = new ArrayList<SPNodeKey>();
        for (ISPNode n : nodePath) {
            res.add(n.getNodeKey());
        }

        return Collections.unmodifiableList(res);
    }

    private static DefaultMutableTreeNode treeNode(DefaultMutableTreeNode root, List<SPNodeKey> nodePath) {
        if ((root == null) || nodePath.isEmpty()) return null;

        final SPNodeKey rootKey = key(root);
        final SPNodeKey curKey  = nodePath.get(0);
        if (!rootKey.equals(curKey)) return null;

        // Pop nodes off the stack finding the matching TreeNodes as we go.
        int index = 1;
        DefaultMutableTreeNode tn = root;
        while ((tn != null) && (index < nodePath.size())) {
            final SPNodeKey childKey = nodePath.get(index++);

            // Find child amongst "tn" children.
            DefaultMutableTreeNode matchingChild = null;
            final Enumeration tnChildren = tn.children();
            while (tnChildren.hasMoreElements()) {
                final DefaultMutableTreeNode tnChild = (DefaultMutableTreeNode) tnChildren.nextElement();
                final SPNodeKey key = key(tnChild);
                if (key.equals(childKey)) {
                    matchingChild = tnChild;
                    break;
                }
            }
            tn = matchingChild;
        }
        return tn;
    }

    private static DefaultMutableTreeNode treeNode(JTree tree, List<SPNodeKey> nodePath) {
        return treeNode((DefaultMutableTreeNode) tree.getModel().getRoot(), nodePath);
    }

    private static void addNodeDataState(DefaultMutableTreeNode root, Map<SPNodeKey, NodeData.State> m) {
        final NodeData nd = (NodeData) root.getUserObject();
        m.put(nd.getNode().getNodeKey(), nd.getState());
        for (int i=0; i<root.getChildCount(); ++i) {
            addNodeDataState((DefaultMutableTreeNode) root.getChildAt(i), m);
        }
    }

    private static Map<SPNodeKey, NodeData.State> stateMap(JTree tree) {
        final Map<SPNodeKey, NodeData.State> m = new HashMap<SPNodeKey, NodeData.State>();
        addNodeDataState((DefaultMutableTreeNode) tree.getModel().getRoot(), m);
        return Collections.unmodifiableMap(m);
    }

    // The tree widget used to display the science program.
    private final AutoscrollTree _tree;

    // The tree's scroll pane
    private final JScrollPane _scrollPane;

    // Panel for filtering the display by selected observation status values
    private final ObsStatusPanel _obsStatusPanel;

    // Used to search for observations by id or title
    private final SearchPanel _searchBox;

    // The root science program node being displayed
    private ISPProgram _root;

    // list of listeners for tree node selection events
    private final EventListenerList listenerList = new EventListenerList();

    // If true, ignore node selections
    private boolean _ignoreSelection = false;

    // A reference to the SPViewer instance displaying this tree
    private SPViewer _viewer;

    // Indicates how to sort the tree nodes for display. The value should be one of the
    // SORT_BY_... values defined in DBTreeListService.
    private static final String SORT_BY = DBTreeListService.SORT_BY_NONE;

    /**
     * Constructs a <code>{@link SPTree}</code> to display a science program or nightly plan.
     * This constructor constructs an empty tree with no program, call
     * <code>{@link #setRoot}</code> to attach a program or nightly plan.
     */
    public SPTree() {
        setLayout(new BorderLayout());

        _tree = new jsky.util.gui.AutoscrollTree();
        _tree.setEditable(false);
        _tree.setCellRenderer(new NodeRenderer());
        _tree.setShowsRootHandles(false);  // Don't show + on root
        _tree.putClientProperty("JTree.lineStyle", "Angled");
        _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        _tree.setScrollsOnExpand(true);
        _tree.addTreeExpansionListener(new TreeExpansionListener() {
            private void updateOpenState(TreeExpansionEvent event, boolean open) {
                final TreePath tp = event.getPath();
                DefaultMutableTreeNode tn = (DefaultMutableTreeNode) tp.getLastPathComponent();
                ((NodeData) tn.getUserObject()).setOpen(open);
            }
            @Override public void treeExpanded(TreeExpansionEvent event) {
                updateOpenState(event, true);
            }
            @Override public void treeCollapsed(TreeExpansionEvent event) {
                updateOpenState(event, false);
            }
        });
        _tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(final TreeSelectionEvent e) {
                if (!_ignoreSelection && TreeUtil.isSelected(e)) {
                    final TreePath path = e.getNewLeadSelectionPath();
                    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    _fireSelectionEvent((NodeData) node.getUserObject());
                }
            }
        });

        ToolTipManager.sharedInstance().registerComponent(_tree);

        _obsStatusPanel = new ObsStatusPanel();
        add(_obsStatusPanel, BorderLayout.NORTH);

        // Update the tree when the selected observation status changes
        _obsStatusPanel.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                _rebuildTree(saveState(_root));
            }
        });

        StaffBean.addPropertyChangeListener(new PropertyChangeListener() {
            @Override public void propertyChange(PropertyChangeEvent evt) {
                _rebuildTree(saveState(_root));
            }
        });

        _scrollPane = new JScrollPane(_tree);
        add(_scrollPane, BorderLayout.CENTER);

        _searchBox = new SearchPanel(_obsStatusPanel, this);
        add(_searchBox.peer(), BorderLayout.SOUTH);

        // Add a drop target to the FileTree
        new SPTreeDropTarget(this);

        // Add a drag source to the FileTree
        new SPTreeDragSource(this);
    }

    public StateSnapshot snapshot() {
        return new StateSnapshot(selectedNodePath(_tree), stateMap(_tree));
    }

    private StateSnapshot saveState(ISPProgram newRoot) {
        if (newRoot == null) return StateSnapshot.empty();
        else if ((_root == null) || !newRoot.getNodeKey().equals(_root.getNodeKey())) return StateSnapshot.initial(newRoot);
        else return snapshot();
    }

    /**
     * Set a reference to the SPViewer instance displaying this tree
     */
    public void setViewer(final SPViewer viewer) {
        _viewer = viewer;
        _searchBox.setViewer(viewer);
    }

    /**
     * Return a reference to the SPViewer instance displaying this tree
     */
    public SPViewer getViewer() {
        return _viewer;
    }

    /**
     * Return the tree widget used to display the science program.
     */
    public AutoscrollTree getTree() {
        return _tree;
    }

    /**
     * Set to true to ignore node selections
     */
    public void setIgnoreSelection(final boolean b) {
        _ignoreSelection = b;
    }

    /**
     * Returns the current tree root
     */
    ISPProgram getRoot() {
        return _root;
    }

    /**
     * Sets the root node to be displayed.
     */
    public void setRoot(final ISPProgram root) {
        setRoot(root, null);
    }

    public void setRoot(final ISPProgram root, StateSnapshot ss) {
        if (ss == null) ss = saveState(root);

        _root = root;
        if (root == null) {
            _tree.setModel(null);
        } else {
            _rebuildTree(ss);
        }
    }

    /**
     * Redraw the tree from the root
     */
    public void redraw() {
        setRoot(_root);
    }

    /**
     * Return the root of the tree, if it is a science program, otherwise null.
     */
    public ISPProgram getProgram() {
        final ISPNode n = getRoot();
        return n != null ? n.getProgram() : null;
    }

    public ISPObservation getContextObservation() {
        final ISPNode n = getSelectedNode();
        return n != null ? n.getContextObservation() : null;
    }

    /**
     * Returns the tree node for the given science program node, if the node should
     * be displayed in the tree, otherwise returns null.
     */
    private DefaultMutableTreeNode _modelNodeIf(final DBTreeListService.Node node, final StateSnapshot ss) {
        if (!_shouldBeVisibleToUser(node)) return null;

        final NodeData newNd = createNodeData(node, ss.nodeState.get(node.getNodeKey()));
        if (newNd == null) return null;

        return new DefaultMutableTreeNode(newNd);
    }


    // Add the given child node to the tree under the given parent and return the
    // new tree node, if the child node should be in the tree, otherwise do nothing
    // and return null.
    private DefaultMutableTreeNode _addModelNodeIf(final DefaultMutableTreeNode parentModelNode,
                                                   final DBTreeListService.Node node,
                                                   final StateSnapshot ss) {

        final DefaultMutableTreeNode child = _modelNodeIf(node, ss);
        if (child == null) return null;

        parentModelNode.add(child);
        return child;
    }


    // Return the name of the icon to use for the given group node, based on the state of the
    // observations it contains.
    // [from #OT-63:]
    // Colour-code groups and folders using similar coding as for observations.
    // Colour of group or folder icon would be that of the 'earliest' (in lifecycle) observation
    // contained e.g. if a group contains three observations, two at Ready and one at For Review the
    // group icon would be yellow; if all observations were Ready the group icon would be green.
    //[source: kroth e-mail 20jul03, expanded definition by ppuxley]
    private static Icon _getGroupIcon(final ISPGroup group) {

        // If it's just an organizational folder, return the folder icon.
        boolean isFolder = false;
        // LORD OF DESTRUCTION: DataObjectManager get without set
        final SPGroup spg = (SPGroup) group.getDataObject();
        if (spg.getGroupType().equals(GroupType.TYPE_FOLDER)) {
            isFolder = true;
        }

        final List<ISPObservation> obsList = group.getObservations();
        if (obsList.size() == 0) {
            if (!isFolder) return UIConstants.GROUP_ICON;
            else return UIConstants.FOLDER_ICON;
        }

        ObservationStatus min = ObservationStatus.INACTIVE;
        for (final ISPObservation obs : obsList) {
            final ObservationStatus cur = ObservationStatus.computeFor(obs);
            if (cur.isLessThan(min)) {
                min = cur;
                if (min == ObservationStatus.PHASE2) break; // short-cut :/
            }
        }

        return isFolder ? UIConstants.getFolderIcon(min) : UIConstants.getGroupIcon(min);
    }

    // Update the icon for the given group after the status of the observation changed
    private void _updateGroupNodeIcon(final ISPGroup group) {
        final NodeData nd = getNodeData(group);
        if (nd != null) nd.setIcon(_getGroupIcon(group));
    }

    private void _updateContainingGroupNodeIcon(final ISPNode n) {
        if (n != null) {
            if (n instanceof ISPGroup) {
                _updateGroupNodeIcon((ISPGroup) n);
            } else {
                _updateContainingGroupNodeIcon(n.getParent());
            }
        }
    }

    // Rebuild the tree from the root node and return the new JTree root.
    private DefaultMutableTreeNode _rebuildTreeFromRoot(StateSnapshot ss) {
        if (_root != null) {
            final DBTreeListService.Node node = DBTreeListService.getNodeTree(_root, true, SORT_BY);
            if (node != null) {
                final DefaultMutableTreeNode parentNode = _modelNodeIf(node, ss);
                if (parentNode != null) {
                    _rebuildNode(node, parentNode, ss);
                    return parentNode;
                }
            }
        }
        return null;
    }


    // Rebuild the tree from the given science program node and tree model node
    private void _rebuildNode(final DBTreeListService.Node node,
                              final DefaultMutableTreeNode parentNode,
                              final StateSnapshot ss) {

        for (final Object o : node.getSubNodes()) {
            final DBTreeListService.Node subnode = (DBTreeListService.Node) o;
            final DefaultMutableTreeNode treeNode = _addModelNodeIf(parentNode, subnode, ss);
            if (treeNode != null && subnode.getRemoteNode() instanceof ISPContainerNode) {
                _rebuildNode(subnode, treeNode, ss);
            }
        }
    }


    /**
     * Rebuild the part of the tree under the given node.
     *
     * @param remoteNode the SP tree node
     * @param treeNode   the corresponding JTree node (parent node of that part of the JTree)
     */
    private void _rebuildSubnodes(final ISPNode remoteNode, final DefaultMutableTreeNode treeNode) {
        if (remoteNode instanceof ISPContainerNode) {
            final DefaultTreeModel model = getModel();
            final StateSnapshot ss = snapshot();

            treeNode.removeAllChildren();
            final DBTreeListService.Node node = DBTreeListService.getNodeTree(remoteNode, true, SORT_BY);
            if (node != null) _rebuildNode(node, treeNode, ss);

            model.nodeStructureChanged(treeNode);
            ss.restore(this);
        }
    }

    /**
     * Rebuild the tree model based on the current science program model
     */
    private void _rebuildTree(StateSnapshot ss) {
        final TreeModel oldModel = _tree.getModel();
        final DefaultMutableTreeNode root = _rebuildTreeFromRoot(ss);
        final DefaultTreeModel model = (root == null) ? null : new DefaultTreeModel(root);

        if (_root != null && oldModel != null) {
            final boolean ignore = _ignoreSelection;
            _ignoreSelection = true;
            try {
                _tree.setModel(model);
            } finally {
                _ignoreSelection = ignore;
            }
            ss.restore(this);
        } else {
            _tree.setModel(model);
        }
    }

    // Make sure that the given node is expanded and viewable.
    public void expandNode(final ISPNode node) {
        if (node != null) {
            final DefaultMutableTreeNode treeNode = getTreeNode(node);
            if (treeNode != null) {
                final TreePath path = new TreePath(treeNode.getPath());
                if (_tree.isCollapsed(path)) {
                    _tree.expandPath(path);
                    ((NodeData) treeNode.getUserObject()).setOpen(true);
                }
            }
        }
    }


    /**
     * Expand all tree nodes under the given program node.
     */
    public void expandAll(final ISPNode node) {
        final TreeModel model = _tree.getModel();
        final DefaultMutableTreeNode treeNode = getTreeNode(node);
        _expandAll(model, treeNode);
    }


    /**
     * Make sure that all nodes under the given tree node are expanded.
     */
    private void _expandAll(final TreeModel model, final DefaultMutableTreeNode treeNode) {
        final TreePath path = new TreePath(treeNode.getPath());
        if (_tree.isCollapsed(path)) {
            _tree.expandPath(path);
        }
        final int n = model.getChildCount(treeNode);
        for (int i = 0; i < n; i++) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) model.getChild(treeNode, i);
            _expandAll(model, node);
        }
    }

    /**
     * Collapse all tree nodes under the given program node.
     */
    public void collapseAll(final ISPNode node) {
        final TreeModel model = _tree.getModel();
        final DefaultMutableTreeNode treeNode = getTreeNode(node);
        _collapseAll(model, treeNode);
        if (node instanceof ISPProgram) {
            final TreePath path = new TreePath(treeNode.getPath());
            _tree.expandPath(path);
            ((NodeData) treeNode.getUserObject()).setOpen(true);
        }
    }


    /**
     * Make sure that all nodes under the given tree node are collapsed.
     */
    private void _collapseAll(final TreeModel model, final DefaultMutableTreeNode treeNode) {
        final int n = model.getChildCount(treeNode);
        for (int i = 0; i < n; i++) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) model.getChild(treeNode, i);
            _collapseAll(model, node);
        }
        final TreePath path = new TreePath(treeNode.getPath());
        if (_tree.isExpanded(path)) {
            _tree.collapsePath(path);
            ((NodeData) treeNode.getUserObject()).setOpen(false);
        }
    }

    /**
     * Checks whether a program node should be displayed by this tree.
     * To be displayed it must have a UIInfo object with the "visible"
     * property true.
     */
    private boolean _shouldBeVisibleToUser(final DBTreeListService.Node node) {
        final ISPNode rn = node.getRemoteNode();
        if ((rn instanceof ISPGroup) && !_obsStatusPanel.isStatusEnabled((ISPGroup) rn)) {
            return false;
        } else if ((rn instanceof ISPObservation) && !_obsStatusPanel.isStatusEnabled((ISPObservation) rn)) {
            return false;
        }
        final Object dataObject = node.getDataObject();
        final UIInfo uiInfo = UIInfoXML.getUIInfo(dataObject);

        // To be visible, must have a UIInfo and must be visible.
        if (uiInfo == null || !uiInfo.isVisible()) {
            return false;
        }

        final ISPNode spNode = node.getRemoteNode();
        final SPProgramID programID = (spNode == null) ? null : spNode.getProgramID();
        final boolean isStaff = OTOptions.isStaff(programID);

        if (uiInfo.getType().equals(UIInfo.TYPE_ENG_COMP) && !isStaff) {
            return false;
        }

        // Program notes are gemini-internal and should not be shown unless the OT
        // is running in onsite mode. This shouldn't happen since the OODB will
        // filter out these nodes on fetch, but just in case...
        return !((dataObject instanceof ProgramNote) && !isStaff);
    }

    /**
     * Returns a NodeData object for given sp node, uses its UIInfo.
     */
    public static NodeData createNodeData(final DBTreeListService.Node node, final NodeData.State state) {
        try {
            final ISPNode remoteNode = node.getRemoteNode();
            final UIInfo uiInfo = UIInfoXML.getUIInfo(node.getDataObject());
            final NodeData nd = new NodeData(remoteNode, uiInfo, (state == null) ? NodeData.State.EMPTY : state);
            if (remoteNode instanceof ISPGroup) {
                nd.setIcon(_getGroupIcon((ISPGroup) remoteNode));
            }
            return nd;
        } catch (Exception e) {
            DialogUtil.error(e);
            return null;
        }
    }

    /**
     * Offers access to <code>{@link DefaultTreeModel}</code> used by the
     * <code>{@link JTree}</code> component.
     * A client may want access to become a listener.
     */
    DefaultTreeModel getModel() {
        return (DefaultTreeModel) (_tree.getModel());
    }

    /**
     * Return the currently selected tree node
     */
    DefaultMutableTreeNode getSelectedTreeNode() {
        final TreePath path = _tree.getSelectionPath();
        return (path == null) ? null : (DefaultMutableTreeNode) path.getLastPathComponent();
    }


    // return an array of the currently selected tree nodes, discarding any
    // whose parents are also selected.
    private TreePath[] _getSelectionPaths() {
        TreePath paths[] = _tree.getSelectionPaths();
        if (paths == null) return null;

        final ArrayList<TreePath> l = new ArrayList<TreePath>();
        for (final TreePath path : paths) {
            final int n = l.size();
            if (n == 0) {
                l.add(path);
            } else {
                boolean isDescendant = false;
                for (final TreePath tp : l) {
                    if (tp.isDescendant(path)) {
                        isDescendant = true;
                        break;
                    }
                }
                if (!isDescendant)
                    l.add(path);
            }
        }
        paths = new TreePath[l.size()];
        l.toArray(paths);
        return paths;
    }

    /**
     * Return an array containing the currently selected (top level) tree nodes
     */
    DefaultMutableTreeNode[] getSelectedTreeNodes() {
        final TreePath[] paths = _getSelectionPaths();
        if (paths == null)
            return null;
        final DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[paths.length];
        for (int i = 0; i < paths.length; i++)
            nodes[i] = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
        return nodes;
    }

    /**
     * Return the remote SP node corresponding to the given tree node.
     */
    ISPNode getNode(final DefaultMutableTreeNode treeNode) {
        if (treeNode == null) return null;
        final Object o = treeNode.getUserObject();
        if (o == null) return null;
        return ((NodeData) o).getNode();
    }

    /**
     * Return the tree node corresponding to the given remote SP node.
     */
    public DefaultMutableTreeNode getTreeNode(final ISPNode node) {
        return treeNode(_tree, nodePath(node));
    }

    NodeData getNodeData(final ISPNode node) {
        final DefaultMutableTreeNode tn = getTreeNode(node);
        return (tn == null) ? null : (NodeData) tn.getUserObject();
    }

    /**
     * Return the tree node for the given location.
     */
    DefaultMutableTreeNode getTreeNode(final Point location) {
        final TreePath treePath = _tree.getPathForLocation(location.x, location.y);
        if (treePath == null) {
            return null;
        }
        return (DefaultMutableTreeNode) treePath.getLastPathComponent();
    }

    /**
     * Return the remote SP tree node for the given location in the tree.
     */
    ISPNode getNode(final Point location) {
        return getNode(getTreeNode(location));
    }

    /**
     * Return an object describing the currently selected tree node
     */
    public NodeData getViewable() {
        final DefaultMutableTreeNode node = getSelectedTreeNode();
        if (node != null) {
            return (NodeData) node.getUserObject();
        }
        return null;
    }

    /**
     * Return the currently selected <code>{@link edu.gemini.pot.sp.ISPNode Program Node}
     * </code>.  A client could get the selection directly from the
     * <code>JTree</code> but would then have the burden of converting to
     * a Science Program element.
     */
    public ISPNode getSelectedNode() {
        return getNode(getSelectedTreeNode());
    }

    /**
     * Set the selected tree node
     */
    public void setSelectedNode(final DefaultMutableTreeNode treeNode) {
        if (treeNode != null) _tree.setSelectionPath(new TreePath(treeNode.getPath()));
    }

    /**
     * Set the selected tree node
     */
    public void setSelectedNode(final Point location) {
        final DefaultMutableTreeNode treeNode = getTreeNode(location);
        if (treeNode != null) setSelectedNode(treeNode);
    }


    /**
     * Set the selected tree node based on the given science program node.
     */
    public void setSelectedNode(final ISPNode node) {
        if (node != null) {
            final DefaultMutableTreeNode treeNode = getTreeNode(node);
            if (treeNode != null) {
                final TreePath treePath = new TreePath(treeNode.getPath());
                _tree.setSelectionPath(treePath);
                _tree.scrollPathToVisible(treePath);

                // Try to not scroll horizontally
                final JViewport v = _scrollPane.getViewport();
                final Point p = v.getViewPosition();
                p.x = 0;
                v.setViewPosition(p);
            }
        }
    }


    /**
     * Return an array containing the currently selected science program nodes,
     * or null if none were selected.
     */
    public ISPNode[] getSelectedNodes() {
        final DefaultMutableTreeNode[] nodes = getSelectedTreeNodes();
        if (nodes == null)
            return null;

        final ArrayList<ISPNode> l = new ArrayList<ISPNode>(nodes.length);
        for (final DefaultMutableTreeNode node : nodes) {
            final ISPNode rnode = ((NodeData) node.getUserObject()).getNode();
            l.add(rnode);
        }

        final ISPNode[] rnodes = new ISPNode[l.size()];
        l.toArray(rnodes);
        return rnodes;
    }


    /**
     * Implemenents CurrentNodeSupplier
     *
     * @return Returns the currently selected node.
     */
    public ISPNode getCurrentNode() {
        return getSelectedNode();
    }

    /**
     * Move the given node from its current location to the given parent node.
     * Exception: Moving an observation to another observation (or a subnode of it)
     * just moves the observation to above the "parent" observation in the list of
     * observations for the program.
     */
    public void moveNode(final ISPNode node, final ISPNode parent)
            throws SPNodeNotLocalException, SPTreeStateException {

        if (node.equals(parent))
            return;

        // Normal drag&drop move requires you to drop a node on a valid parent node.
        // To make it more intuitive, we also allow moving nodes up and down via drag&drop
        // (only within the list of sibling nodes).
        if (node instanceof ISPObsComponent && !(parent instanceof ISPObsComponentContainer)) {
            final ISPObsComponent obsComp = (ISPObsComponent) node;
            if (parent instanceof ISPObsComponent) {
                final ISPObsComponent targetObsComp = (ISPObsComponent) parent;
                final ISPObsComponentContainer c = (ISPObsComponentContainer) targetObsComp.getParent();
                final List<ISPObsComponent> l = c.getObsComponents();
                if (l.remove(obsComp)) {
                    l.add(l.indexOf(targetObsComp), obsComp);
                    c.setObsComponents(l);
                    return;
                }
            }
        }

        if (node instanceof ISPSeqComponent) {
            final ISPSeqComponent seqComp = (ISPSeqComponent) node;
            if (parent instanceof ISPSeqComponent) {
                final ISPSeqComponent targetSeqComp = (ISPSeqComponent) parent;
                final SPComponentBroadType broadType = targetSeqComp.getType().broadType;
                // only move up/down if target can't contain subnodes
                if (broadType.equals(SeqRepeatObserve.SP_TYPE.broadType)) {
                    final ISPNode targetSeqCompParent = targetSeqComp.getParent();
                    if (targetSeqCompParent instanceof ISPSeqComponent
                            && targetSeqCompParent.equals(seqComp.getParent())) {
                        final ISPSeqComponent sc = (ISPSeqComponent) targetSeqCompParent;
                        final List<ISPSeqComponent> l = sc.getSeqComponents();
                        if (l.remove(seqComp)) {
                            l.add(l.indexOf(targetSeqComp), seqComp);
                            sc.setSeqComponents(l);
                            return;
                        }
                    }
                }
            }
        }

        final ISPNode nodeParent = node.getParent();
        if (removeNode(node)) {
            if (!addNode(node, parent)) {
                addNode(node, nodeParent); // if add actually fails, add the node again
            }
        }
    }

    /**
     * Remove the given node from the tree and return true if done.
     */
    boolean removeNode(final ISPNode node) {
        return SPTreeEditUtil.removeNode(node);
    }


    /**
     * Add the given node to the given parent node.
     *
     * @return true if the node could be added
     */
    public boolean addNode(final ISPNode node, final ISPNode parent)
            throws SPNodeNotLocalException, SPTreeStateException {

        final ISPProgram prog = getProgram();
        return prog != null && SPTreeEditUtil.addNode(prog, parent, node);

    }

    /**
     * Adds a <code>{@link SPTreeListener}</code>; it will be informed
     * when program nodes are selected.
     */
    public void addSPTreeListener(final SPTreeListener spl) {
        listenerList.add(SPTreeListener.class, spl);
    }

    /**
     * Called when a tree node is selected to notify any listeners.
     */
    private void _fireSelectionEvent(final NodeData viewable) {
        final SPTreeEvent spe = new SPTreeEvent(this, viewable);
        final Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SPTreeListener.class) {
                ((SPTreeListener) listeners[i + 1]).nodeSelected(spe);
            }
        }
    }


    /**
     * Special renderer that knows how to display sp tree nodes.
     */
    public static class NodeRenderer extends DefaultTreeCellRenderer {
        private static final Border _spacing = BorderFactory.createEmptyBorder(2, 2, 2, 2);

        /*
        private enum NodeState {
            conflict {
                public void style(JComponent comp) {
                    comp.setForeground(Color.red.darker());
                    comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                }
            },
            created {
                private final Color c = new Color(128, 64, 0);
                public void style(JComponent comp) {
                    comp.setForeground(c);
                    comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                }
            },
            modified {
                public void style(JComponent comp) {
                    comp.setForeground(Color.blue.darker());
                    comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                }
            },
            normal {
                public void style(JComponent comp) {
                    comp.setForeground(Color.black);
                    comp.setFont(comp.getFont().deriveFont(Font.PLAIN));
                }
            };

            public abstract void style(JComponent comp);
        }

        private static final NodeState nodeState(final ISPNode node) {
            if (node.hasConflicts()) return NodeState.conflict;
            else {
                final SPProgramID pid = node.getProgramID();
                if (pid == null) return NodeState.normal;

                final scala.collection.immutable.Map<SPNodeKey, VersionVector<LifespanId, Integer>> local, remote;
                remote = VersionMapStore.getOrNull(node.getProgramID());
                if (remote == null) return NodeState.normal;

                final SPNodeKey key = node.getNodeKey();
                local  = node.getProgram().getVersions();

                if (JavaVersionMapOps.isNewLocally(key, local, remote)) return NodeState.created;
                else if (JavaVersionMapOps.isModifiedLocally(key, local, remote)) return NodeState.modified;
                else return NodeState.normal;
            }
        }
        */


        public Component getTreeCellRendererComponent(final JTree tree,
                                                      final Object value,
                                                      final boolean selected,
                                                      final boolean expanded,
                                                      final boolean leaf,
                                                      final int row,
                                                      final boolean hasFocus) {

            super.getTreeCellRendererComponent(tree, value, selected,
                    expanded, leaf, row,
                    hasFocus);

            final DefaultMutableTreeNode tn = (DefaultMutableTreeNode) value;
            final Object obj = tn.getUserObject();
            if (!(obj instanceof NodeData)) {
                return this;
            }

            final NodeData viewable = (NodeData) obj;
            try {
                setText(viewable.getTitle());

                if (viewable.hasConflict()) {
                    setForeground(Color.red.darker());
                } else {
                    setForeground(Color.black);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            setIcon(viewable.getIcon());
            setBorder(_spacing);

            return this;
        }
    }

    /**
     * Called whenever a node's data object is modified
     */
    public void dataObjectChanged(final ISPNode sourceNode, final ISPDataObject oldValue, final ISPDataObject newValue) {
        // check if the node's title changed
        String oldTitle = null, newTitle = null;
        if (newValue != null) newTitle = newValue.getTitle();
        if (oldValue != null) oldTitle = oldValue.getTitle();

        if (oldValue == null || !StringUtil.equals(oldTitle, newTitle)) {
            // the node's title changed
            if (sourceNode != null) {
                final DefaultMutableTreeNode tn = getTreeNode(sourceNode);
                if (tn != null) getModel().nodeChanged(tn);
            }
        }

        if (newValue instanceof SPObservation) {
            // if the observation status changed, redraw the tree to reapply any filters
            final SPObservation d1 = (SPObservation) newValue;
            final SPObservation d2 = (SPObservation) oldValue;
            if ((d2 != null) && //d1 is always not null
                    isObsStatusChange(d1, d2)) {
                handlePotentialObsStatusUpdate(sourceNode);
            } else {
                _tree.repaint();
            }

        } else if (newValue instanceof ObsExecLog) {
            final int stepCount = getStepCount(sourceNode);
            final ObsExecLog newLog = (ObsExecLog) newValue;
            final ObsExecLog oldLog = (ObsExecLog) oldValue;
            if ((oldLog != null) && (oldLog.getRecord().getExecStatus(stepCount) != newLog.getRecord().getExecStatus(stepCount))) {
                handlePotentialObsStatusUpdate(sourceNode);
            } else {
                _tree.repaint();
            }

        } else if (newValue instanceof ISPSeqObject) {
            final ISPSeqObject newSeq = (ISPSeqObject) newValue;
            final ISPSeqObject oldSeq = (ISPSeqObject) oldValue;

            if ((oldSeq == null) || newSeq.getStepCount() != oldSeq.getStepCount()) {
                handlePotentialObsStatusUpdate(sourceNode);
            }

        } else if (sourceNode instanceof ISPGroup) {
            // Also need to update the group icon if it changed from a scheduling group
            // to a folder or vice-versa.
            _updateGroupNodeIcon((ISPGroup) sourceNode);
        }
    }

    private void handlePotentialObsStatusUpdate(ISPNode src) {
        _updateContainingGroupNodeIcon(src);
        final boolean ignore = _ignoreSelection;
        _ignoreSelection = true;
        try {
            redraw();
        } finally {
            _ignoreSelection = ignore;
        }
        OT.updateEditableState(src);
    }

    private static int getStepCount(ISPNode node) {
        if (node == null) {
            return 0;
        } else if (node instanceof ISPObservation) {
            final ISPSeqComponent root = ((ISPObservation) node).getSeqComponent();
            return (root == null) ? 0 : root.getStepCount();
        } else {
            return getStepCount(node.getParent());
        }
    }

    private static boolean isObsStatusChange(final SPObservation o1, final SPObservation o2) {
        return (o1.getPhase2Status() != o2.getPhase2Status()) ||
                (o1.getExecStatusOverride() != o2.getExecStatusOverride());
    }

    /**
     * Called whenever the SP tree structure is modified
     */
    public void treeStructureChanged(final ISPNode modifiedNode) {
        // For simplicity, just redraw the part of the tree under the modified node.
        if (modifiedNode instanceof ISPProgram) {
            // If the top level of the tree is involved, redraw the whole tree
            redraw();
        } else if (modifiedNode instanceof ISPGroup) {
            _updateGroupNodeIcon((ISPGroup) modifiedNode);
            redraw();
        } else if (modifiedNode != null) {
            // otherwise redraw only the subtree under the modified node
            final DefaultMutableTreeNode treeNode = getTreeNode(modifiedNode);
            if (treeNode != null) {
                _rebuildSubnodes(modifiedNode, treeNode);
            }
        }
    }


}
