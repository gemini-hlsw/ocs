package jsky.app.ot.viewer;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPNodeKey;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * Records the open/close and selected node state of a Science Program tree and
 * provides support for restoring it to a new JTree.
 */
public final class SPTreeState {
    private final SPNodeKey selected;
    private final Set<SPNodeKey> open;

    public SPTreeState(SPNodeKey selected, Set<SPNodeKey> open) {
        this.selected = selected;
        this.open     = new HashSet<SPNodeKey>(open);
    }

    /**
     * Restores this state information in the given JTree.
     */
    public void restore(JTree tree) {
        final TreeModel tm = tree.getModel();
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) tm.getRoot();
        restore(tree, root);
    }

    private void restore(JTree tree, DefaultMutableTreeNode treeNode) {
        final NodeStuff nd = NodeStuff.apply(treeNode);
        if (nd == null) return;

        if (nd.key.equals(selected)) {
            tree.setSelectionPath(new TreePath(treeNode.getPath()));
        }

        if (open.contains(nd.key)) {
            nd.viewable.setOpen(true);
            tree.expandPath(new TreePath(treeNode.getPath()));

            Enumeration<DefaultMutableTreeNode> c = nd.children();
            while (c.hasMoreElements()) restore(tree, c.nextElement());
        } else {
            nd.viewable.setOpen(false);
        }
    }

    /**
     * Obtains a snapshot of the open/close and selection state for the given
     * JTree.
     */
    public static SPTreeState apply(JTree tree) {
        final TreePath p = tree.getSelectionPath();
        final SPNodeKey selected = (p == null) ? null :
                NodeStuff.apply((DefaultMutableTreeNode) p.getLastPathComponent()).key;

        final TreeModel tm = tree.getModel();
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) tm.getRoot();
        final Set<SPNodeKey> open = new HashSet<SPNodeKey>();
        apply0(tree, root, open);

        return new SPTreeState(selected, open);
    }

    private static void apply0(JTree tree, DefaultMutableTreeNode treeNode, Set<SPNodeKey> open) {
        final NodeStuff nd = NodeStuff.apply(treeNode);
        if (nd == null) return;

        if (nd.isOpen(tree)) {
            open.add(nd.key);
            final Enumeration<DefaultMutableTreeNode> c = nd.children();
            while (c.hasMoreElements()) apply0(tree, c.nextElement(), open);
        }
    }

    // A tuple of related information for a node in the JTree/Science Program:
    // (DefaultMutableTreeNode, NodeData, ISPNode, SPNodeKey)
    private static final class NodeStuff {
        final DefaultMutableTreeNode treeNode;
        final NodeData viewable;
        final ISPNode programNode;
        final SPNodeKey key;
        final TreePath path;

        private NodeStuff(DefaultMutableTreeNode tNode, NodeData v,
                         ISPNode pNode, SPNodeKey k) {
            this.treeNode    = tNode;
            this.viewable    = v;
            this.programNode = pNode;
            this.key         = k;
            this.path        = new TreePath(tNode.getPath());
        }

        @SuppressWarnings("unchecked")
        Enumeration<DefaultMutableTreeNode> children() {
            return (Enumeration<DefaultMutableTreeNode>) treeNode.children();
        }

        public boolean isOpen(JTree tree) {
            return tree.isExpanded(path);
        }

        static NodeStuff apply(DefaultMutableTreeNode tNode) {
            final NodeData v = (NodeData) tNode.getUserObject();
            if (v == null) return null;

            final ISPNode pNode = v.getNode();
            if (pNode == null) return null;

            final SPNodeKey key = pNode.getNodeKey();
            if (key == null) return null;

            return new NodeStuff(tNode, v, pNode, key);
        }
    }
}
