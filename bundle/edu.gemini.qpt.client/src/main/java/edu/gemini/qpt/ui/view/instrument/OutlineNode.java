package edu.gemini.qpt.ui.view.instrument;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.type.DisplayableSpType;


@SuppressWarnings("serial")
public class OutlineNode extends DefaultMutableTreeNode {

    
    public enum TriState {    SELECTED, UNSELECTED, INDEFINITE }

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
            outer: switch (selected) {

            // If we are currently in a definited select state, our new state will
            // be indefinite if there is more than one child, or the same as the one
            // and only child if there is only one.
            case SELECTED:
            case UNSELECTED:
                // REL-293: Should be able to deselect oiwfs without deselecting parent inst
//                selected = (getChildCount() == 1) ? childSelected : TriState.INDEFINITE;
                selected = TriState.INDEFINITE;
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
                selected = state;
                
            }
            
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
