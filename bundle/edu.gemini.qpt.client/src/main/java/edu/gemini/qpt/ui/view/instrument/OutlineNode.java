package edu.gemini.qpt.ui.view.instrument;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.type.DisplayableSpType;


@SuppressWarnings("serial")
public class OutlineNode extends DefaultMutableTreeNode {

	
	public enum TriState {	SELECTED, UNSELECTED, INDEFINITE }

	private TriState selected = TriState.UNSELECTED;
	private boolean adjusting;
	
	public OutlineNode() {
		super();
	}

	public OutlineNode(Object userObject) {
		super(userObject);
	}

	public TriState getSelected() {
		return selected;
	}
	
	/**
	 * When a node is selected or deselected, the change is pushed down
	 * recursively to all children, and pushed up to the parent. Setting
	 * the state is a boolean operation, but reading the state may return
	 * a third indefinite value.
	 * @param selected
	 */
	@SuppressWarnings("unchecked")
	public synchronized void setSelected(boolean selected) {
		this.selected = selected ? TriState.SELECTED : TriState.UNSELECTED;
		if (getChildCount() > 0) {
			adjusting = true;
			for (OutlineNode node: (List<OutlineNode>) children)
				node.setSelected(selected);
			adjusting = false;
		}
		if (getParent() != null)
			((OutlineNode) getParent()).childChanged(this.selected);
	}


    @SuppressWarnings("unchecked")
    void childChanged(TriState childSelected) {
        if (!adjusting) {
            TriState oldSelected = selected;

            // If we only have one child, then our selected state is the child's state.
            // If the child is INDEFINITE, then this node must also be indefinite.
            if (getChildCount() == 1 || childSelected == TriState.INDEFINITE)
                selected = childSelected;

            // If this node is in a definite state and the child is in the opposite state, we must
            // move to an indefinite state.
            else if ((selected == TriState.SELECTED   && childSelected == TriState.UNSELECTED) ||
                     (selected == TriState.UNSELECTED && childSelected == TriState.SELECTED))
                selected = TriState.INDEFINITE;

            // Otherwise, if this node is indefinite, and the child is definite, we cannot know what
            // to do unless we consider all child nodes.
            else if (selected == TriState.INDEFINITE) {
                boolean allSelected   = true;
                boolean allUnselected = true;
                for (OutlineNode node : (List<OutlineNode>) children) {
                    if (!node.getSelected().equals(TriState.SELECTED))
                        allSelected   = false;
                    if (!node.getSelected().equals(TriState.UNSELECTED))
                        allUnselected = false;
                    if (!allSelected && !allUnselected)
                        break;
                }

                // Four cases to consider. Ignore the case where allSelected and allUnselected are both true,
                // as this should never happen if there are children nodes since we know the child node triggering
                // this call is not INDEFINITE, and thus will trigger one of the two branches in the loop above.
                if (allSelected)
                    selected = TriState.SELECTED;
                else if (allUnselected)
                    selected = TriState.UNSELECTED;
                else
                    selected = TriState.INDEFINITE;
            }

            // If we have changed selected or we have a different value than the parent now, invoke recursively up the tree.
            // We do not need to invoke downwards.
            OutlineNode parent = (OutlineNode) getParent();
            if (parent != null && (!oldSelected.equals(selected) || !selected.equals(parent.getSelected())))
                parent.childChanged(selected);
        }
    }


	@Override
	public String toString() {
		if (userObject instanceof DisplayableSpType) {
			return ((DisplayableSpType) userObject).displayValue();
		}		
		return super.toString();
	}
	
}
