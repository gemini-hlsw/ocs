package edu.gemini.qpt.ui.view.instrument;

import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;

import edu.gemini.spModel.type.DisplayableSpType;


@SuppressWarnings("serial")
public class OutlineNode extends DefaultMutableTreeNode {

	public enum TriState {	SELECTED, UNSELECTED, INDEFINITE }

	private TriState selected = TriState.UNSELECTED;
	private boolean adjusting;

    // A flag that determines if an OutlineNode's TriState is dependent on its children or not.
    // This should be used for "grouping nodes", such as is the case with filters.
    private boolean isDependentOnChildren = true;
	
	public OutlineNode() {
		super();
	}

	public OutlineNode(Object userObject) {
		super(userObject);
	}

    public OutlineNode(Object userObject, boolean isDependentOnChildren) {
        super(userObject);
        this.isDependentOnChildren = isDependentOnChildren;
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

        // Two special cases to consider:
        // 1. If this node is selected but any of the children are not selected, make this node instead indefinite.
        // 2. If none of the children are selected / indefinite and this node is dependent on its children, mark it
        //    as unselected.
        if (selected) {
            boolean allChildrenUnselected = true;

            for (OutlineNode node : (List<OutlineNode>) children) {
                if (!TriState.UNSELECTED.equals(node.getSelected()))
                    allChildrenUnselected = false;
                if (!TriState.SELECTED.equals(node.getSelected()))
                    this.selected = TriState.INDEFINITE;
            }

            if (isDependentOnChildren && getChildCount() > 0 && allChildrenUnselected)
                this.selected = TriState.UNSELECTED;
        }

        // Previously, we propagated the change downward through the child to select / deselect everything,
        // but we don't want to do this anymore and want to preserve the settings to the children.
//		if (getChildCount() > 0) {
//			adjusting = true;
//			for (OutlineNode node: (List<OutlineNode>) children)
//				node.setSelected(selected);
//			adjusting = false;
//		}

		if (getParent() != null)
			((OutlineNode) getParent()).childChanged(this.selected);
	}


    @SuppressWarnings("unchecked")
    void childChanged(TriState childSelected) {
        if (!adjusting) {
            TriState oldSelected = selected;

            // Two cases to consider:
            // 1. If this node is fully dependent on the child nodes, then calculate it based on that.
            // 2. Otherwise, if this node is selected and a child node is not, mark it instead indefinite.
            if (isDependentOnChildren) {
                // If we only have one child, then our selected state is the child's state.
                // If the child is INDEFINITE, then this node must also be indefinite.
                if (getChildCount() == 1 || childSelected == TriState.INDEFINITE)
                    selected = childSelected;

                    // If this node is in a definite state and the child is in the opposite state, we must
                    // move to an indefinite state.
                else if ((TriState.SELECTED.equals(selected) && TriState.UNSELECTED.equals(childSelected)) ||
                        (TriState.UNSELECTED.equals(selected) && TriState.SELECTED.equals(childSelected)))
                    selected = TriState.INDEFINITE;

                    // Otherwise, if this node is indefinite, and the child is definite, we cannot know what
                    // to do unless we consider all child nodes.
                else if (TriState.INDEFINITE.equals(selected)) {
                    boolean allSelected = true;
                    boolean allUnselected = true;
                    for (OutlineNode node : (List<OutlineNode>) children) {
                        if (!node.getSelected().equals(TriState.SELECTED))
                            allSelected = false;
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
            }
            else {
                if (TriState.SELECTED.equals(selected) && !TriState.SELECTED.equals(childSelected))
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
