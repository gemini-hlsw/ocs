package jsky.app.ot.editor.template;

import edu.gemini.pot.sp.ISPContainerNode;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.spModel.gemini.security.UserRolePrivileges;
import edu.gemini.spModel.util.DBTreeListService;
import jsky.util.gui.Resources;
import jsky.app.ot.viewer.NodeData;
import jsky.app.ot.viewer.SPTree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static edu.gemini.spModel.util.DBTreeListService.SORT_BY_TITLE;
import static edu.gemini.spModel.util.DBTreeListService.getNodeTree;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;

/**
 * A dialog that displays a subtree of a science program and allows selection of nodes, then performs an arbitrary
 * action on these selected nodes, returning a value of type T.
 */
public abstract class NodeSelector<T> extends JDialog {

    private final UserRolePrivileges urps;
    private static final KeyStroke ESC = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    private Exception exception = null;// What happens when you click cancel or hit esc?
    private T result;

    protected abstract String getCaption();
    protected abstract T commit() throws Exception;

    public T open(Component parent) throws Exception {
        setModal(true);
        setLocationRelativeTo(parent);
        setVisible(true);
        if (exception != null)
            throw exception;
        return result;
    }

    protected UserRolePrivileges getUserRolePrivileges() {
        return urps;
    }

    public boolean isEmpty() {
        return tree.getModel().getRoot() == null;
    }

    protected NodeSelector(ISPContainerNode rootNode)  {
        this(rootNode, null);
    }

    protected NodeSelector(ISPContainerNode rootNode, ISPNode contextNode)  {
        this(rootNode, contextNode, null);
    }

    protected NodeSelector(ISPContainerNode rootNode, ISPNode contextNode, UserRolePrivileges urps)  {

        this.urps = urps;

        // Build our nodes (the same structure used by SPViewer)
        final DBTreeListService.Node root = getNodeTree(rootNode, true, SORT_BY_TITLE);
        final DBTreeListService.Node context = findNode(root, contextNode);

        // Build our tree model
        final DefaultMutableTreeNode tn = treeNode(root, context, false);
        final DefaultTreeModel tm = new DefaultTreeModel(tn);

        // Configure the tree
        final int visibleRowCount = 20;
        tree.setModel(tm);
        tree.setCellRenderer(new TemplateDialogRenderer());
        tree.addMouseListener(listener);
        tree.setVisibleRowCount(visibleRowCount);
        tree.setToggleClickCount(0); // disallow click to expand
        expandSelected(tn);

        // Construct the dialog
        setContentPane(new JPanel() {{
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            setLayout(new BorderLayout());
            add(new JLabel(getCaption()) {{
                setForeground(Color.BLACK);
                setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
            }}, BorderLayout.NORTH);
            add(new JScrollPane(tree) {{
                getViewport().setPreferredSize(new Dimension(600, tree.getRowHeight() * visibleRowCount));
            }}, BorderLayout.CENTER);
            add(buttonPanel(), BorderLayout.SOUTH);
        }});

        // Set default ok and cancel actions, and finish up
        getRootPane().setDefaultButton(ok);
        getRootPane().registerKeyboardAction(cancelAction, ESC, WHEN_IN_FOCUSED_WINDOW);
        pack();

    }

    // Creates a panel to contain the button controls at the bottom.
    private JPanel buttonPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        final Insets r = new Insets(0, 0, 0, 5);
        p.add(new JLabel("Select"), new GridBagConstraints()      {{ gridx=0; insets=r; }});
        p.add(all, new GridBagConstraints()                       {{ gridx=1; insets=r; }});
        p.add(none, new GridBagConstraints()                      {{ gridx=2; }});
        p.add(new JPanel(), new GridBagConstraints()              {{ gridx=3; fill=HORIZONTAL; weightx=1.0; }});
        p.add(new JButton(cancelAction), new GridBagConstraints() {{ gridx=4; insets=r; }});
        p.add(ok, new GridBagConstraints()                        {{ gridx=5; }});
        return p;
    }

    /*
    private static int rowCount(DefaultMutableTreeNode tn) {
        final Enumeration e = tn.breadthFirstEnumeration();
        int count = 0;
        while (e.hasMoreElements()) {
            e.nextElement();
            ++count;
        }
        return count;
    }
    */

    private final Action cancelAction = new AbstractAction("Cancel") {
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    };

    // What happens when you click ok or hit enter?
    private final Action okAction = new AbstractAction("Ok") {
        public void actionPerformed(ActionEvent e) {
            try {
                result = commit();
            } catch (Exception ex) {
                exception = ex;
            } finally {
                setVisible(false);
            }
        }
    };

    public List<NodeData> getSelectedNodes() {

        // Breadth-first
        final LinkedList<DefaultMutableTreeNode> queue = new LinkedList<DefaultMutableTreeNode>();
        queue.addLast((DefaultMutableTreeNode) tree.getModel().getRoot());

        // Accumulate NodeDatas
        final ArrayList<NodeData> accum = new ArrayList<NodeData>();
        while (!queue.isEmpty()) {
            final DefaultMutableTreeNode n = queue.removeFirst();
            final NodeData nd = (NodeData) n.getUserObject();
            if (nd.isOpen())
                accum.add(nd);
            for (int i = 0; i < n.getChildCount(); i++)
                queue.addLast((DefaultMutableTreeNode) n.getChildAt(i));
        }

        // Done
        return accum;

    }

    // We need references to a few controls
    protected final JTree tree = new JTree() {
        public void expandPath(TreePath path) {
            // override to expand to leaves
            setExpandedState(path, true);
        }
    };

    // Select all
    private final Action allAction = new AbstractAction("All") {
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getModel().getRoot();
            setSelected(node, true);
            tree.repaint();
        }
    };
    // Select none
    private final Action noneAction = new AbstractAction("None") {
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getModel().getRoot();
            setSelected(node, false);
            tree.repaint();
        }
    };
    private static void setSelected(DefaultMutableTreeNode node, boolean selected) {
        ((NodeData) node.getUserObject()).setOpen(selected);
        for (int i=0; i<node.getChildCount(); ++i) {
            setSelected((DefaultMutableTreeNode) node.getChildAt(i), selected);
        }
    }

    protected final JButton all  = new JButton(allAction);
    protected final JButton none = new JButton(noneAction);

    protected final JButton ok = new JButton(okAction);

    // MouseListener that handles clicking on checkboxes. It's kind of ugly, sorry.
    protected final MouseListener listener = new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
            final TreePath tp = tree.getPathForLocation(e.getX(), e.getY());
            if (tp != null) {
                final Rectangle bounds = tree.getPathBounds(tp);
                if (e.getX() < bounds.getX() + 16) { // width of checkbox
                    final DefaultMutableTreeNode tn = (DefaultMutableTreeNode) tp.getLastPathComponent();
                    final NodeData nd = (NodeData) tn.getUserObject();
                    final boolean open = !nd.isOpen();
                    setOpen1(tn, open);
                    setOpen2(tn, open);
                    tree.repaint();
                }
            }
        }

        // Push selection state down
        private void setOpen1(DefaultMutableTreeNode tn, boolean open) {
            final NodeData nd = (NodeData) tn.getUserObject();
            nd.setOpen(open);
            for (int i = 0; i < tn.getChildCount(); i++) {
                final DefaultMutableTreeNode tn0 = (DefaultMutableTreeNode) tn.getChildAt(i);
                setOpen1(tn0, open);
            }
        }

        // Push selecton state up
        private void setOpen2(DefaultMutableTreeNode tn, boolean open) {
            if (open) {
                final DefaultMutableTreeNode p = (DefaultMutableTreeNode) tn.getParent();
                if (p != null) {
                    for (int i = 0; i < p.getChildCount(); i++) {
                        final DefaultMutableTreeNode tn0 = (DefaultMutableTreeNode) p.getChildAt(i);
                        final NodeData nd = (NodeData) tn0.getUserObject();
                        if (!nd.isOpen())
                            return; // done, nothing to do
                    }
                    ((NodeData) p.getUserObject()).setOpen(open);
                    setOpen2(p, open);
                }
            } else {
                for (Object o : tn.getPath()) {
                    final DefaultMutableTreeNode tn0 = (DefaultMutableTreeNode) o;
                    final NodeData nd = (NodeData) tn0.getUserObject();
                    nd.setOpen(open);
                }
            }
        }

    };// Find the Node associated with the given remoteNode, if any.

    protected static DBTreeListService.Node findNode(DBTreeListService.Node n, ISPNode rn) {
        if (rn == null) return null;
        if (n.getRemoteNode().equals(rn))
            return n;
        for (final DBTreeListService.Node c : n.getSubNodes()) {
            final DBTreeListService.Node ret = findNode(c, rn);
            if (ret != null)
                return ret;
        }
        return null;
    }// Construct a subtree from the given node, recursing on nodes of the types we're interested in

    protected DefaultMutableTreeNode treeNode(DBTreeListService.Node n, DBTreeListService.Node context, boolean select) {
        if (isRelevant(n)) {
            select |= n == context;
            final NodeData nd = SPTree.createNodeData(n, NodeData.State.EMPTY.setOpen(select));
            final DefaultMutableTreeNode tn = new DefaultMutableTreeNode(nd);
            for (DBTreeListService.Node c : n.getSubNodes())
                if (isRelevant(c))
                    tn.add(treeNode(c, context, select));
            return tn;
        }
        return null;
    }

    protected abstract boolean isRelevant(DBTreeListService.Node n);

    protected void expandSelected(DefaultMutableTreeNode tn) {
        if (tn != null) {
            final NodeData nd = (NodeData) tn.getUserObject();
            if (nd.isOpen())
                tree.expandPath(new TreePath(tn.getPath()));
            for (int i = 0; i < tn.getChildCount(); i++) {
                final DefaultMutableTreeNode tn0 = (DefaultMutableTreeNode) tn.getChildAt(i);
                expandSelected(tn0);
            }
        }
    }
}

class TemplateDialogRenderer extends SPTree.NodeRenderer {

    private static final Icon CHECK_SELECTED = Resources.getIcon("pit/check_selected.gif");
    private static final Icon CHECK_UNSELECTED = Resources.getIcon("pit/check_unselected.gif");
    private static final Icon CHECK_INDETERMINATE = Resources.getIcon("pit/check_indefinite.gif");

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        // Superclass will make everything look like SPTree by default, but we never show a node
        // as selected or focused because it's kind of distracting.
        super.getTreeCellRendererComponent(tree, value, false, expanded, leaf, row, false);

        // Get the NodeData we're looking at
        final DefaultMutableTreeNode tn = (DefaultMutableTreeNode) value;
        final NodeData nd = (NodeData) tn.getUserObject();

        // Icon
        decorate(nd);

        // Find the right checkbox icon. HACK: We're tracking "checked" with unused the "open" bit
        Icon check = CHECK_UNSELECTED;
        if (nd.isOpen()) {
            check = CHECK_SELECTED;
        } else if (isAnyDescendantSelected(tn)) {
            check = CHECK_INDETERMINATE;
        }

        // Done, yay
        if (getIcon() != null)
            setIcon(new DualIcon(check, getIcon()));
        return this;

    }

    protected void decorate(NodeData nd) {
        // nop
    }

    protected boolean isAnyDescendantSelected(DefaultMutableTreeNode tn) {
        final NodeData nd = (NodeData) tn.getUserObject();
        if (nd.isOpen()) return true;
        for (int i = 0; i < tn.getChildCount(); i++) {
            final DefaultMutableTreeNode tn0 = (DefaultMutableTreeNode) tn.getChildAt(i);
            if (isAnyDescendantSelected(tn0))
                return true;
        }
        return false;
    }

    /**
     * A side-by-side icon.
     */
    protected static class DualIcon implements Icon {

        private final Icon left;
        private final Icon right;

        private final int SPACING = 2; // px

        public DualIcon(Icon left, Icon right) {
            if (left == null || right == null)
                throw new IllegalArgumentException("Icons can't be null");
            this.left = left;
            this.right = right;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            left.paintIcon(c, g, x, y);
            right.paintIcon(c, g, x + left.getIconWidth() + SPACING, y);
        }

        public int getIconWidth() {
            return left.getIconWidth() + SPACING + right.getIconWidth();
        }

        public int getIconHeight() {
            return Math.max(left.getIconHeight(), right.getIconHeight());
        }

    }

}

