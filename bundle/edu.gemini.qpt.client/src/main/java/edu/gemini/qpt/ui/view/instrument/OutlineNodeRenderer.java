package edu.gemini.qpt.ui.view.instrument;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import edu.gemini.qpt.ui.util.SharedIcons;

@SuppressWarnings("serial")
public class OutlineNodeRenderer extends DefaultTreeCellRenderer {

    @SuppressWarnings("unchecked")
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		OutlineNodeRenderer ret = (OutlineNodeRenderer) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        OutlineNode node   = (OutlineNode) value;
        OutlineNode parent = (OutlineNode) node.getParent();
		switch (node.getSelected()) {
		
		case INDEFINITE:
			setIcon(SharedIcons.CHECK_INDETERMINATE);
			break;
			
		case SELECTED:
			setIcon(SharedIcons.CHECK_SELECTED);
			break;
		
		case UNSELECTED:
			setIcon(SharedIcons.CHECK_UNSELECTED);
			break;
		
		}

        // We enable this node if:
        // 1. The parent is the root (parent != null), or
        // 2. If the parent is not unselected.
        ret.setEnabled(parent == null || parent.isRoot() || !OutlineNode.TriState.UNSELECTED.equals(parent.getSelected()));
		return ret;
	}
	
}
