package jsky.app.ot.viewer;

import java.util.*;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * A utility class for handling JTrees.
 *
 * @author	Shane Walker
 */
public final class TreeUtil {

    /**
     * A TreeModelListener that listens for the addition of new tree nodes
     * and ensures that they show up fully expanded.  By default, new tree
     * nodes are added collapsed.
     */
    public static class NodeExpander implements TreeModelListener {
        private final JTree _jTree;

        /**
         * Constructs with the JTree that will be used to expand the
         * tree model's nodes.
         */
        public NodeExpander(final JTree jTree) {
            _jTree = jTree;
        }

        //
        // The call to expand the nodes must be placed in a separate thread
        // or else will not work.  This could be because the TreeModelListener
        // is called before the JTree itself notices the insertion of the new
        // nodes. Rather than calling expand directly, it is invoked later
        // after the JTree has had a chance to draw the new nodes.  This seems
        // to solve the problem.
        //
        private void _expand(final TreeModelEvent evt) {
            final TreePath tp = evt.getTreePath();
            final TreeNode tn = (TreeNode) tp.getLastPathComponent();
            javax.swing.SwingUtilities.invokeLater(() -> expandSubtree(_jTree, tn));
        }

        public void treeNodesInserted(final TreeModelEvent evt) {
            _expand(evt);
        }

        public void treeStructureChanged(final TreeModelEvent evt) {
            _expand(evt);
        }

        public void treeNodesChanged(final TreeModelEvent evt) { /* don't care */
        }

        public void treeNodesRemoved(final TreeModelEvent evt) { /* don't care */
        }
    }


    /**
     * Gets the TreePath cooresponding to a generic TreeNode.
     */
    public static TreePath getTreePath(final TreeNode tn) {
        final List<TreeNode> lst = new ArrayList<>();
        lst.add(tn);

        TreeNode parent = tn.getParent();
        while (parent != null) {
            lst.add(parent);
            parent = parent.getParent();
        }
        Collections.reverse(lst);

        return new TreePath(lst.toArray());
    }

    /**
     * Expands the child nodes below the given TreeNode.
     *
     * @param jTree the tree that contains the TreeNode to be expanded
     * @param tn    the tree node whose child nodes will be expanded
     */
    public static void expandChildNodes(final JTree jTree, final TreeNode tn) {
        final TreePath tp = getTreePath(tn);

        final Enumeration kids = tn.children();
        while (kids.hasMoreElements()) {
            final Object kid = kids.nextElement();
            final TreePath kidTP = tp.pathByAddingChild(kid);
            jTree.expandPath(kidTP);
        }
    }

    /**
     * Expand the entire subtree below the given TreeNode.
     */
    public static void expandSubtree(final JTree jTree, final TreeNode parent) {
        if (parent.isLeaf()) {
            return;
        }

        // Get all the leaf nodes descending from parent
        final Set<TreeNode> set = new HashSet<>();
        getLeafNodes(parent).stream()
                .map(TreeNode::getParent)
                .forEach(set::add);

        // Iterate through the parents of leafs, expanding each.
        set.forEach(n -> jTree.expandPath(getTreePath(n)));
    }

    // Helper for public getLeafNodes method.
    private static List<TreeNode> _getLeafNodes(final TreeNode parent) {
        final List<TreeNode> lst = new ArrayList<>();
        for (final Enumeration kids = parent.children(); kids.hasMoreElements();) {
            final TreeNode kid = (TreeNode) kids.nextElement();
            if (kid.isLeaf()) {
                lst.add(kid);
            } else {
                lst.addAll(_getLeafNodes(kid));
            }
        }
        return lst;
    }

    /**
     * Gets an iterator that returns all of the leaf nodes
     */
    public static List<TreeNode> getLeafNodes(final TreeNode parent) {
        return parent.isLeaf() ? Collections.singletonList(parent) : _getLeafNodes(parent);
    }

    /**
     * Adds a NodeExpander to the given tree model.  The NodeExpander
     * ensures that any new nodes added to the tree are added expanded.
     */
    public static NodeExpander addNodeExpander(final JTree jTree, final TreeModel treeModel) {
        final NodeExpander ne = new NodeExpander(jTree);
        treeModel.addTreeModelListener(ne);
        return ne;
    }

    /**
     * Return true if a tree node was selected and false if one was deselected,
     * based on the given event.
     */
    public static boolean isSelected(final TreeSelectionEvent e) {
        return Arrays.asList(e.getPaths()).stream().filter(e::isAddedPath).findFirst().isPresent();
    }
}

