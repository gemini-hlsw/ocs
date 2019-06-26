package edu.gemini.qpt.ui.view.instrument;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.gemini.spModel.type.DisplayableSpType;


@SuppressWarnings("serial")
public final class OutlineNode extends DefaultMutableTreeNode {

    public enum TriState { SELECTED, UNSELECTED, INDEFINITE }

    public static TriState triStateFromBoolean(boolean selected) {
        return selected ? TriState.SELECTED : TriState.UNSELECTED;
    }

    private TriState triState = TriState.UNSELECTED;
    private boolean adjusting;

    public OutlineNode() {
        super();
    }

    public OutlineNode(Object userObject) {
        super(userObject);
    }

    public TriState getSelected() {
        return triState;
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
        this.triState = triStateFromBoolean(selected);
        if (getChildCount() > 0) {
            adjusting = true;
            for (OutlineNode node: (List<OutlineNode>) children)
                node.setSelected(selected);
            adjusting = false;
        }
        if (getParent() != null)
            ((OutlineNode) getParent()).childChanged(this.triState);
    }

    @SuppressWarnings("unchecked")
    private void childChanged(TriState childSelected) {
        if (!adjusting) {
            outer: switch (triState) {

            // If we are currently in a definited select state, our new state will
            // be indefinite if there is more than one child, or the same as the one
            // and only child if there is only one.
            case SELECTED:
            case UNSELECTED:
                // REL-293: Should be able to deselect oiwfs without deselecting parent inst
                // (SW 6/26/19: NICI has only one child, NICI OIWFS which
                //  apparently is the source of problem. I think it should be
                //  solved in some other way though that doesn't destroy the
                //  plain meaning of the "tristate" selection tree. Perhaps a
                //  separate "Guiding" branch with the guiders in it?)
//                selected = (getChildCount() == 1) ? childSelected : TriState.INDEFINITE;
                triState = TriState.INDEFINITE;
                break;

            // If our current state is indefinite and the child is definite, we need
            // to look at all the children.
            case INDEFINITE:
                if (childSelected == TriState.INDEFINITE) break; // easy out
                TriState state =childSelected;
                for (OutlineNode node: (List<OutlineNode>) children) {
                    if (node.getSelected() != state)
                        break outer; // still indefinite
                }
                triState = state;

            }

        }
    }

    //
    // The following `setSelected` implementation corrects issues with the
    // previous version that keeps "Update from ICTD" from working as expected.
    // Unfortunately there seems to be some, on the surface of it, odd behavior
    // related to REL-293 that we are now relying upon?  It also doesn't
    // continue up the entire tree updating parents when a child node is
    // updated. I'm worried about replacing the previous version that is used by
    // the UI and am just adding an option to do it "correctly" for the
    // "Update from ICTD" feature.
    //

    /**
     * When a node is selected or deselected, the change is pushed down
     * recursively to all children, and pushed up through the ancestors. Setting
     * the state is a boolean operation, but reading the state may return
     * a third indefinite value.
     */
    public void setSelectedCorrectly(boolean selected) {
        setSelectedCorrectly(selected, true);
    }

    @SuppressWarnings("unchecked")
    private void setSelectedCorrectly(boolean selected, boolean updateAncestors) {
        final TriState orig = this.triState;
        this.triState       = triStateFromBoolean(selected);

        if (orig != this.triState) {
            if (!isLeaf()) {
                // There's no need in this case to update the children's ancestors
                // since we're doing it here.
                ((List<OutlineNode>) children).forEach(n -> n.setSelectedCorrectly(selected, false));
            }

            if (updateAncestors && (getParent() != null)) {
                // Walk up the tree updating ancestor nodes correctly.
                ((OutlineNode) getParent()).childChangedCorrectly(this.triState);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void childChangedCorrectly(final TriState updatedChildTriState) {
        final TriState orig = this.triState;

        switch (this.triState) {
            // Here we are only called because the child tristate changed.
            // If we are currently in a definite select state, our new state
            // will be indefinite if there is more than one child since we
            // know that the child now differs from the other siblings, or
            // the same as the one and only child if there is only one.
            case SELECTED:
            case UNSELECTED:
                this.triState = (getChildCount() == 1) ? updatedChildTriState : TriState.INDEFINITE;
                break;

            // If our current state is indefinite and the child is definite,
            // we need to look at all the children.
            case INDEFINITE:
                if (updatedChildTriState == TriState.INDEFINITE) break; // easy out, nothing changes

                if (((List<OutlineNode>) children).stream().allMatch(n -> n.getSelected() == updatedChildTriState)) {
                    this.triState = updatedChildTriState;
                }

        }

        // Keep going up the tree all the way to the root.
        if ((orig != this.triState) && (getParent() != null)) {
            ((OutlineNode) getParent()).childChangedCorrectly(this.triState);
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
