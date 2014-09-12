package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.*;
import jsky.app.ot.nsp.SPTreeEditUtil;
import jsky.app.ot.viewer.SPViewer;
import jsky.util.gui.DialogUtil;

import java.awt.event.ActionEvent;

public final class AddSeqCompAction extends AbstractViewerAction implements Comparable<AddSeqCompAction> {

    private final SPComponentType componentType;

    public AddSeqCompAction(SPViewer viewer, SPComponentType type) {
        super(viewer, type.readableStr);
        componentType = type;
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            final ISPSeqComponent child = viewer.getFactory().createSeqComponent(getProgram(), componentType, null);
            final ISPNode node = getContextNode();
            if (node instanceof ISPSeqComponent) {
                ((ISPSeqComponent) node).addSeqComponent(child);
            } else if (node instanceof ISPObservation) {
                ((ISPObservation) node).setSeqComponent(child);
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    @Override
    public boolean computeEnabledState() {
        try {
            if (getProgram() != null) {
                final ISPNode parent = getContextNode();
                if (parent != null) {
                    final ISPSeqComponent child = viewer.getFactory().createSeqComponent(getProgram(), componentType, BOGUS_KEY);
                    return SPTreeEditUtil.isOkayToAdd(getProgram(), child, parent, viewer.getNode());
                }
            }
        } catch (SPUnknownIDException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public int compareTo(AddSeqCompAction action) {
        return componentType.readableStr.compareToIgnoreCase(action.componentType.readableStr);
    }

    // Return the current ISPSeqComponent, if any, otherwise the ISPObservation, if any
    private ISPNode getContextNode() {
        if (componentType == SPComponentType.ITERATOR_BASE) return nodeIf(ISPObservation.class);
        else return nodeIf(ISPSeqComponent.class);
    }

}
