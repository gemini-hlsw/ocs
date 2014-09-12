package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.*;
import jsky.app.ot.nsp.SPTreeEditUtil;
import jsky.app.ot.viewer.SPViewer;
import jsky.util.gui.DialogUtil;

import javax.swing.Icon;
import java.awt.event.ActionEvent;

/**
* Created with IntelliJ IDEA.
* User: rnorris
* Date: 1/17/13
* Time: 11:06 AM
* To change this template use File | Settings | File Templates.
*/
public class AddObsCompAction extends AbstractViewerAction implements Comparable<AddObsCompAction> {

    private final SPComponentType componentType;

    // initialize with the component type
    public AddObsCompAction(SPViewer viewer, SPComponentType type) {
        super(viewer, type.readableStr);
        componentType = type;
    }

    // initialize with the component type and menu icon
    public AddObsCompAction(SPViewer viewer, SPComponentType type, Icon icon) {
        super(viewer, type.readableStr, icon);
        componentType = type;
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            final ISPObsComponentContainer parent = getContextNode(ISPObsComponentContainer.class);
            final ISPObsComponent toAdd = viewer.getFactory().createObsComponent(getProgram(), componentType, null);
            parent.addObsComponent(toAdd);
        } catch (Exception ex) {
            DialogUtil.error(ex);
        }
    }

    public int compareTo(AddObsCompAction action) {
        return componentType.readableStr.compareToIgnoreCase(action.componentType.readableStr);
    }

    @Override
    public boolean computeEnabledState() {
        try {
            final ISPObsComponentContainer parent = getContextNode(ISPObsComponentContainer.class);
            if (parent != null) {
                final ISPObsComponent toAdd = viewer.getFactory().createObsComponent(getProgram(), componentType, BOGUS_KEY);
                return SPTreeEditUtil.isOkayToAdd(getProgram(), toAdd, parent, viewer.getNode());
            }
        } catch (SPUnknownIDException ex) {
            ex.printStackTrace();
        }
        return false;
    }

}
