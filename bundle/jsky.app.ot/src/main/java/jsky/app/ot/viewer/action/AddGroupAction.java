package jsky.app.ot.viewer.action;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.obscomp.SPGroup;
import edu.gemini.spModel.obscomp.SetDefaultGroupTitleFunctor;
import jsky.app.ot.OT;
import jsky.app.ot.nsp.SPTreeEditUtil;
import jsky.app.ot.ui.util.UIConstants;
import jsky.app.ot.viewer.SPViewer;
import jsky.util.gui.DialogUtil;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AddGroupAction extends AbstractViewerAction {

    final SPGroup.GroupType type;

    public AddGroupAction(SPViewer viewer, SPGroup.GroupType type) {
        super(viewer, type.displayValue(), (type == SPGroup.GroupType.TYPE_FOLDER ? UIConstants.FOLDER_ICON : UIConstants.GROUP_ICON));
        this.type = type;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        try {
            final List<ISPObservation> obsList = getSelectedObservations();
            final ISPGroupContainer parent = getContextNode(ISPGroupContainer.class);
            final ISPGroup group = viewer.getFactory().createGroup(getProgram(), null);
            SetDefaultGroupTitleFunctor.setDefaultGroupTitle(viewer.getDatabase(), group, OT.getUser());
            SPGroup data = (SPGroup) group.getDataObject();
            data.setGroupType(type);
            group.setDataObject(data);
            moveObservations(obsList, group);
            parent.addGroup(group);
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    private List<ISPObservation> getSelectedObservations() {
        if (viewer == null) return Collections.emptyList();
        // Evil ... gives you all observations everywhere if prog is selected!
//        final ISPObservation[] obsArray = viewer.getSelectedObservations();
        final ISPNode[] nodes = viewer.getTree().getSelectedNodes();
        if (nodes == null) return Collections.emptyList();

        final List<ISPObservation> obsList = new ArrayList<ISPObservation>();
        for (ISPNode n : nodes) {
            if (n instanceof ISPObservation) obsList.add((ISPObservation) n);
        }
        return Collections.unmodifiableList(obsList);
    }

    private void moveObservations(List<ISPObservation> obsList, ISPGroup toGroup) throws SPException {
        for (ISPObservation o : obsList) {
            final ISPContainerNode c = o.getParent();
            if (c != null) {
                ((ISPObservationContainer) c).removeObservation(o);
            }
        }
        toGroup.setObservations(obsList);
    }

    @Override
    public boolean computeEnabledState() throws Exception {
        final ISPGroupContainer parent = getContextNode(ISPGroupContainer.class);
        if (parent != null) {
            final ISPGroup group = viewer.getFactory().createGroup(getProgram(), BOGUS_KEY);
            return SPTreeEditUtil.isOkayToAdd(getProgram(), group, parent, viewer.getNode());
        }
        return false;
    }

}
