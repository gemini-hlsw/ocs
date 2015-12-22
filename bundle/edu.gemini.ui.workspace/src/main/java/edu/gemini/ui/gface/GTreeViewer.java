package edu.gemini.ui.gface;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

@SuppressWarnings("serial")
public class GTreeViewer<M, E> extends GViewer<M, E> implements TreeSelectionListener {

    private final JTree tree;
    private final Map<E, TreePath> pathMap = new HashMap<>();

    private boolean pushingSelection;

    public GTreeViewer(GTreeController<M, E> controller, JTree tree) {
        super(controller, tree);
        this.tree = tree;
        tree.setModel(new DefaultTreeModel(null));
        tree.addTreeSelectionListener(this);
        tree.setCellRenderer(renderer);
    }

    public void valueChanged(TreeSelectionEvent e) {
        if (pushingSelection) return;
        pullSelection();
    }

    public GTreeViewer(GTreeController<M, E> controller) {
        this(controller, new JTree());
    }

    @Override
    public void refresh() {
        invokeLater(refreshTask);
    }


    private void invokeLater(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    @SuppressWarnings("unchecked")
    private void pullSelection() {

        // Create a new selection from the tree.
        TreeSelectionModel tsm = tree.getSelectionModel();
        ArrayList<E> accum = new ArrayList<>();
        TreePath[] paths = tsm.getSelectionPaths();
        if (paths != null) {
            for (TreePath path: paths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                accum.add((E) node.getUserObject());
            }
        }

        // Notify superclass that we have pulled a new selection
        setPulledSelection(new GSelection(accum.toArray()));

    }

    private class SetSelectionTask implements Runnable {

        private final Set<E> set = new HashSet<>();

        public SetSelectionTask(final GSelection<E> newSelection) {

            // Only select the first item if we're in single selection mode.
            boolean single = tree.getSelectionModel().getSelectionMode() ==
                TreeSelectionModel.SINGLE_TREE_SELECTION;

            for (E o: newSelection) {
                set.add(o);
                if (single) break;
            }

        }

        public void run() {
            pushingSelection = true;
            TreeSelectionModel tsm = tree.getSelectionModel();
            tsm.clearSelection();
            for (E o: set) {
                TreePath path = pathMap.get(o);
                if (path != null) {
                    tsm.addSelectionPath(path);
                    tree.expandPath(path);
                    tree.scrollPathToVisible(path);
                }
            }
            pushingSelection = false;
            pullSelection();
        }

    }

    @Override
    public GTreeController<M, E> getController() {
        return (GTreeController<M, E>) super.getController();
    }

    @Override
    public GElementDecorator<M, E> getDecorator() {
        return (GElementDecorator<M, E>) super.getDecorator();
    }

    public void setDecorator(GElementDecorator<M, E> decorator) {
        super.setDecorator(decorator);
    }

    public JTree getTree() {
        return tree;
    }

    private final Runnable refreshTask = new Runnable() {

        @SuppressWarnings("unchecked")
        public void run() {

            // Set up to copy the tree
            DefaultTreeModel tm = (DefaultTreeModel) tree.getModel();
            GTreeController<M, E> controller = getController();
            GFilter<M, E> filter = getFilter();
            GComparator<M, E> comparator = getComparator();
            DefaultMutableTreeNode rootNode = null;

            // Initialize depth-first walk
            LinkedList<DefaultMutableTreeNode> queue = new LinkedList<>();
            E root = controller.getRoot();
            if (root != null && (filter == null || filter.accept(root)))
                queue.add(rootNode = new DefaultMutableTreeNode(root));

            // Copy all the expanded paths so we can preserve them
            final List<E> expanded = new ArrayList<>();
            for (Map.Entry<E, TreePath> e: pathMap.entrySet()) {
                if (tree.isExpanded(e.getValue()))
                    expanded.add(e.getKey());
            }

            // Build up the tree and hash the paths
            pathMap.clear();
            while (!queue.isEmpty()) {

                // Remove parent from head of list
                DefaultMutableTreeNode parentNode = queue.removeFirst();
                E parent = (E) parentNode.getUserObject();

                // Hash it
                pathMap.put(parent, new TreePath(parentNode.getPath()));

                // Copy and filter list of children
                ArrayList<E> children = new ArrayList<>();
                for (E child: controller.getChildren(parent)) {
                    if (filter == null || filter.accept(child))
                        children.add(child);
                }

                // Sort them
                if (comparator != null)
                    Collections.sort(children, comparator);

                // Add child nodes to parent node and to end of queue
                for (E child: children) {
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
                    parentNode.add(childNode);
                    queue.addLast(childNode);
                }

            }

            // Set the new root and restore the selection. Order here is important.
            GSelection<E> oldSelection = getSelection();
            tree.setIgnoreRepaint(true);
            pushingSelection = true;
            tm.setRoot(rootNode);
            new SetSelectionTask(oldSelection).run();

            // Can we do this here?
            for (E e: expanded) {
                final TreePath p = pathMap.get(e);
                if (p != null)
                    tree.expandPath(p);
            }

            tree.setIgnoreRepaint(false);

        }

    };

    private final TreeCellRenderer renderer = new DefaultTreeCellRenderer() {

        @SuppressWarnings("unchecked")
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            JLabel label = (JLabel)  super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,	row, false);

            GElementDecorator<M, E> decorator = getDecorator();
            if (decorator != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                decorator.decorate(label, (E) node.getUserObject());
            }

            return label;
        }

    };

    @Override
    protected Runnable getSelectionTask(GSelection<E> newSelection) {
        return new SetSelectionTask(newSelection);
    }

    @SuppressWarnings("unchecked")
    @Override
    public E getElementAt(Point p) {
        TreePath path = tree.getPathForLocation(p.x, p.y);
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            return (E) node.getUserObject();
        } else {
            return null;
        }
    }


}
