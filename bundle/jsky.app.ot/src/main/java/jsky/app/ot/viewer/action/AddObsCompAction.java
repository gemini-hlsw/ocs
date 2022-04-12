package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.init.NodeInitializers;
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
            final Option<ISPNodeInitializer<ISPObsComponent, ? extends ISPDataObject>> init = customInitializer(parent);

            final ISPObsComponent toAdd =
                viewer.getFactory().createObsComponent(getProgram(), componentType, init.getOrNull(), null);

            parent.addObsComponent(toAdd);
        } catch (Exception ex) {
            DialogUtil.error(ex);
        }
    }

    /**
     * Provides a hook for subclasses to return a custom initializer for the
     * component depending on context.  By default, no custom initializer is
     * assumed.
     */
    protected Option<ISPNodeInitializer<ISPObsComponent, ? extends ISPDataObject>> customInitializer(
        ISPObsComponentContainer container
    ) {
        return ImOption.empty();
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
