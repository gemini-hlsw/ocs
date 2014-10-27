package edu.gemini.qpt.ui.view.instrument;

import java.awt.*;
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
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.shared.sp.Inst;
import edu.gemini.qpt.ui.view.instrument.OutlineNode.TriState;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewContext;
import edu.gemini.ui.workspace.util.ElementFactory;

public class InstViewAdvisor implements IViewAdvisor, PropertyChangeListener {

	private final JTree tree = ElementFactory.createTree();
	private Schedule model;
    private IViewContext context;
	
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
		
		tree.getModel().addTreeModelListener(new TreeModelListener() {
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
		});
		
		// Scroll pane
		JScrollPane scroll = new JScrollPane(tree);
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
		OutlineNode root = new OutlineNode();
		for (final Inst i: Inst.values()) {
			if (!i.existsAtSite(sched.getSite())) continue;

			OutlineNode inode = new OutlineNode(i, i.getDependentOnChildren());
			inode.setSelected(sched.hasFacility(i.getValue()));
			root.add(inode);

			Map<String, OutlineNode> categoryNodes = new TreeMap<String, OutlineNode>();			
			for (Enum option: i.getOptions()) {
				OutlineNode optionNode = new OutlineNode(option);

				String category = Inst.getCategory(option);
				if (category == null)
					inode.add(optionNode);
				else {
					OutlineNode categoryNode = categoryNodes.get(category);
					if (categoryNode == null) {
						categoryNode = new OutlineNode(category);
						categoryNodes.put(category, categoryNode);
						inode.add(categoryNode);
					}
					categoryNode.add(optionNode);
				}
				optionNode.setSelected(sched.hasFacility(option));
			}
		}
		return root;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (IShell.PROP_MODEL.equals(evt.getPropertyName())) {
			synchronized (this) {
				Schedule newModel = (Schedule) evt.getNewValue();

                // REL-1301: Only rebuild the entire tree when the model has just been instantiated.
                // Note that if a new plan is created, this will happen as well automatically.
                // This preserves the tree selections after refreshes.
                if (model == null)
                    ((DefaultTreeModel) tree.getModel()).setRoot(getRoot(newModel));
                model = newModel;
                updateSchedule();
			}
		}
	}

    private void updateSchedule() {
        if (model != null) {
            synchronized (this) {
                // This is inefficient but it's cleaner than figuring out all the
                // corner cases for various kinds of modifications.
                Set<Enum> facilities = new HashSet<Enum>();
                Enumeration e = ((OutlineNode) tree.getModel().getRoot()).breadthFirstEnumeration();
                while (e.hasMoreElements()) {
                    OutlineNode n = (OutlineNode) e.nextElement();
                    if (n.getSelected() != TriState.UNSELECTED) {
                        Object o = n.getUserObject();
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
