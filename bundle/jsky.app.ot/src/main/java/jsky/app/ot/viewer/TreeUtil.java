package jsky.app.ot.viewer;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

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
        private JTree _jTree;

        /**
         * Constructs with the JTree that will be used to expand the
         * tree model's nodes.
         */
        public NodeExpander(JTree jTree) {
            _jTree = jTree;
        }

        //
        // The call to expand the nodes must be placed in a separate thread
        // or else will not work.  This could be because the TreeModelListener
        // is called before the JTree itself notices the insertion of the new
        // nodes. Rather than calling expand directly, it is inovked later
        // after the JTree has had a chance to draw the new nodes.  This seems
        // to solve the problem.
        //
        private void _expand(TreeModelEvent evt) {
            TreePath tp = evt.getTreePath();
            final TreeNode tn = (TreeNode) tp.getLastPathComponent();

            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    expandSubtree(_jTree, tn);
                }
            });
        }

        public void treeNodesInserted(TreeModelEvent evt) {
            _expand(evt);
        }

        public void treeStructureChanged(TreeModelEvent evt) {
            _expand(evt);
        }

        public void treeNodesChanged(TreeModelEvent evt) { /* don't care */
        }

        public void treeNodesRemoved(TreeModelEvent evt) { /* don't care */
        }
    }


    /**
     * Gets the TreePath cooresponding to a generic TreeNode.
     */
    public static TreePath getTreePath(TreeNode tn) {
        List lst = new ArrayList();
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
    public static void expandChildNodes(JTree jTree, TreeNode tn) {
        TreePath tp = getTreePath(tn);

        Enumeration kids = tn.children();
        while (kids.hasMoreElements()) {
            Object kid = kids.nextElement();
            TreePath kidTP = tp.pathByAddingChild(kid);
            jTree.expandPath(kidTP);
        }
    }

    /**
     * Expand the entire subtree below the given TreeNode.
     */
    public static void expandSubtree(JTree jTree, TreeNode parent) {
        if (parent.isLeaf()) {
            return;
        }

        // Get all the leaf nodes descending from parent
        TreeNode[] tnA = getLeafNodes(parent);

        // Create a Set containing the parent of each leaf node
        Set set = new HashSet();
        int sz = tnA.length;
        for (int i = 0; i < sz; ++i) {
            set.add(tnA[i].getParent());
        }

        // Iterate through the parents of leafs, expanding each.
        Iterator it = set.iterator();
        while (it.hasNext()) {
            jTree.expandPath(getTreePath((TreeNode) it.next()));
        }
    }

    //
    // Helper for public getLeafNodes method.
    //
    private static void _getLeafNodes(TreeNode parent, List lst) {
        Enumeration kids = parent.children();
        while (kids.hasMoreElements()) {
            TreeNode kid = (TreeNode) kids.nextElement();
            if (kid.isLeaf()) {
                lst.add(kid);
            } else {
                _getLeafNodes(kid, lst);
            }
        }
    }

    /**
     * Gets an iterator that returns all of the leaf nodes
     */
    public static TreeNode[] getLeafNodes(TreeNode parent) {
        if (parent.isLeaf()) {
            return new TreeNode[]{parent};
        }

        List lst = new ArrayList();
        _getLeafNodes(parent, lst);

        TreeNode[] tnA = new TreeNode[lst.size()];
        return (TreeNode[]) lst.toArray(tnA);
    }


    ///**
    // * Expands all the nodes in the given JTree below the given node, provided
    // * that the tree is made up of DefaultMutableTreeNodes.
    // *
    // * @param jTree the tree whose nodes will be expanded
    // * @param tn    the tree node below which all child nodes will be expanded
    // */
    //public static void
    //expandChildNodes(JTree jTree, DefaultMutableTreeNode tn)
    //{
    //   DefaultMutableTreeNode leaf = tn.getFirstLeaf();
    //   while (leaf != null) {
    //      TreePath tp = new TreePath(leaf.getPath());
    //      jTree.makeVisible(tp);
    //      leaf = leaf.getNextLeaf();
    //   }
    //}


    /**
     * Adds a NodeExpander to the given tree model.  The NodeExpander
     * ensures that any new nodes added to the tree are added expanded.
     */
    public static NodeExpander addNodeExpander(JTree jTree, TreeModel treeModel) {
        NodeExpander ne = new NodeExpander(jTree);
        treeModel.addTreeModelListener(ne);
        return ne;
    }


    /**
     * Return true if a tree node was selected and false if one was deselected,
     * based on the given event.
     */
    public static boolean isSelected(TreeSelectionEvent e) {
        TreePath[] paths = e.getPaths();
        for(int i = 0; i < paths.length; i++) {
            if (e.isAddedPath(paths[i])) {
                return true;
            }
        }
        return false;
    }
}

