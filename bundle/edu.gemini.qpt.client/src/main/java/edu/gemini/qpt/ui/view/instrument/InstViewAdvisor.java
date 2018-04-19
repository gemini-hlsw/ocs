package edu.gemini.qpt.ui.view.instrument;

import java.awt.Cursor;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.shared.sp.Inst;
import edu.gemini.qpt.shared.sp.Ictd;
import edu.gemini.qpt.ui.view.instrument.OutlineNode.TriState;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.ictd.Availability;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewContext;
import edu.gemini.ui.workspace.util.ElementFactory;

public class InstViewAdvisor implements IViewAdvisor, PropertyChangeListener {

    private final JTree tree = ElementFactory.createTree();
    private Schedule model;
    private IViewContext context;

    private final TreeModelListener treeModelListener = new TreeModelListener() {
        public void treeStructureChanged(TreeModelEvent e) {
            updateSchedule();
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            updateSchedule();
        }

        public void treeNodesInserted(TreeModelEvent e) {
            updateSchedule();
        }

        public void treeNodesChanged(TreeModelEvent e) {
            updateSchedule();
        }
    };

    public void open(final IViewContext context) {
        this.context = context;

        // Set up the tree control.
        tree.setModel(new DefaultTreeModel(getRoot(null)));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new OutlineNodeRenderer());
        tree.setSelectionModel(null);
        tree.addMouseListener(new OutlineNodeMouseListener());
        tree.setToggleClickCount(Integer.MAX_VALUE); // don't expand on multi-clicks

        tree.getModel().addTreeModelListener(treeModelListener);

        // Scroll pane
        final JScrollPane scroll = new JScrollPane(tree);
        scroll.getViewport().setPreferredSize(new Dimension(tree.getPreferredSize().width, tree.getRowHeight() * 8));
        scroll.getViewport().setMinimumSize(new Dimension(tree.getPreferredSize().width,  tree.getRowHeight() * 8));
        scroll.setBorder(BorderFactory.createEmptyBorder());

        // And the context.
        context.setTitle("Instruments");
        context.setContent(scroll);

        // Listen for model changes
        context.getShell().addPropertyChangeListener(this);
    }

    public void close(IViewContext context) {

    }

    public void setFocus() {
        tree.requestFocus();
    }

    // Rebuild the entire tree for the specified schedule.
    @SuppressWarnings("unchecked")
    private OutlineNode getRoot(Schedule sched) {
        if (sched == null) return new OutlineNode();
        final OutlineNode root = new OutlineNode();
        for (final Inst i: Inst.values()) {
            if (!i.existsAtSite(sched.getSite())) continue;

            final OutlineNode inode = new OutlineNode(i);
            inode.setSelected(sched.hasFacility(i.getValue()));
            root.add(inode);

            final Map<String, OutlineNode> categoryNodes = new TreeMap<>();
            for (final Enum<?> option: i.getOptions()) {
                final OutlineNode optionNode = new OutlineNode(option);

                final String category = Inst.getCategory(option);
                if (category == null)
                    inode.add(optionNode);
                else {
                    final OutlineNode categoryNodeLookup = categoryNodes.get(category);
                    final OutlineNode categoryNode;
                    if (categoryNodeLookup == null) {
                        categoryNode = new OutlineNode(category);
                        categoryNodes.put(category, categoryNode);
                        inode.add(categoryNode);
                    } else {
                        categoryNode = categoryNodeLookup;
                    }
                    categoryNode.add(optionNode);
                }
                optionNode.setSelected(sched.hasFacility(option));
            }
        }
        return root;
    }

    private final PropertyChangeListener ictdListener = new PropertyChangeListener() {

        // Walk through the entire tree.  For each node, try to find the matching
        // item in the availability map.  If it exists, check or uncheck as
        // appropriate. If not, ignore it and keep going (we want to preserve
        // intermediate nodes like "NICI" and "Dispersers).
        private void updateFromAvailability(final Map<Enum<?>, Availability> m) {

            // Don't trigger events while updating from the availability map.
            tree.getModel().removeTreeModelListener(treeModelListener);

            try {

                final Enumeration e = ((OutlineNode) tree.getModel().getRoot()).breadthFirstEnumeration();
                while (e.hasMoreElements()) {
                    final OutlineNode n = (OutlineNode) e.nextElement();
                    final Object      o = n.getUserObject();
                    if (o != null && o instanceof Enum) {
                        ImOption.apply(m.get((Enum) o)).foreach(a ->
                            n.setSelected(a == Availability.Installed)
                        );
                    }
                }

            } finally {

                // Restore normal event handling.
                tree.getModel().addTreeModelListener(treeModelListener);

            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (Schedule.PROP_ICTD.equals(evt.getPropertyName())) {
                // Update from the feature availability map.
                ((Option<Ictd>) evt.getNewValue()).foreach(ictd ->
                    updateFromAvailability(ictd.featureAvailability)
                );
            }
        }
    };

    public void propertyChange(PropertyChangeEvent evt) {
        if (IShell.PROP_MODEL.equals(evt.getPropertyName())) {
            synchronized (this) {
                // There is a hack in RefreshAction that calls shell.setModel(null) and then sets it back to the
                // original model to force certain components to reupdate. We wish to ignore the null as this will
                // otherwise trigger a call to setRoot(null), and then when the model is set back, a call to
                // setRoot(old model), which causes tree node settings to be lost.
                final Schedule newModel = (Schedule) evt.getNewValue();
                if (newModel == null) return;

                // REL-1301: Only rebuild the entire tree when the model has just been instantiated.
                // Note that if a new plan is created, this will happen as well automatically.
                // This preserves the tree selections after refreshes.
                if (model == null) {
                    ((DefaultTreeModel) tree.getModel()).setRoot(getRoot(newModel));
                } else {
                    model.removePropertyChangeListener(Schedule.PROP_ICTD, ictdListener);
                }
                model = newModel;
                newModel.addPropertyChangeListener(Schedule.PROP_ICTD, ictdListener);
                updateSchedule();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void updateSchedule() {
        if (model != null) {
            synchronized (this) {
                // This is inefficient but it's cleaner than figuring out all the
                // corner cases for various kinds of modifications.
                final Set<Enum> facilities = new HashSet<>();
                final Enumeration e = ((OutlineNode) tree.getModel().getRoot()).breadthFirstEnumeration();
                while (e.hasMoreElements()) {
                    final OutlineNode n = (OutlineNode) e.nextElement();
                    if (n.getSelected() != TriState.UNSELECTED) {
                        final Object o = n.getUserObject();
                        if (o != null && o instanceof Enum) {
                            if (o instanceof Inst)
                                facilities.add(((Inst)o).getValue());
                            else
                                facilities.add((Enum) o);
                        }
                    }
                }

                // If facilities haven't changed, don't reset them. This will
                // happen when the model is first opened and the UI is initializing.
                if (facilities.equals(model.getFacilities()))
                    return;

                // This can take a moment, so show a busy cursor.
                try {
                    context.getShell().getPeer().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    model.setFacilities(facilities);
                } finally {
                    context.getShell().getPeer().setCursor(Cursor.getDefaultCursor());
                }
            }
        }
    }
}
