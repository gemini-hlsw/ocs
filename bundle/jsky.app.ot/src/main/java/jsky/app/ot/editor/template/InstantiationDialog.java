package jsky.app.ot.editor.template;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.*;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.template.InstantiationFunctor;
import edu.gemini.spModel.template.TemplateParameters;
import jsky.app.ot.OT;
import jsky.util.gui.Resources;
import jsky.app.ot.viewer.NodeData;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

import java.util.Arrays;
import java.util.List;

import static edu.gemini.spModel.util.DBTreeListService.Node;

public class InstantiationDialog extends NodeSelector<Void> {

    @SuppressWarnings("unchecked")
    private static final List<Class<? extends ISPProgramNode>> RELEVANT_TYPES = Arrays.asList(
            ISPTemplateFolder.class,
            ISPTemplateGroup.class,
            ISPTemplateParameters.class
    );

    public static void open(Component parent, ISPProgram program, ISPNode context) throws Exception {
        new InstantiationDialog(program.getTemplateFolder(), context).open(parent);
    }

    public InstantiationDialog(ISPContainerNode templateFolder, ISPNode contextNode)  {
        super(templateFolder, contextNode);
        tree.setCellRenderer(new InstantiationDialogRenderer());
    }

    protected String getCaption() {
        return "Select targets/conditions and templates to be applied:";
    }

    protected Void commit() throws Exception {

        // Get oriented
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        final ISPNode node = ((NodeData) root.getUserObject()).getNode();
        final InstantiationFunctor func = new InstantiationFunctor();

        // Add selected items
        for (int i = 0; i < root.getChildCount(); i++) {
            final DefaultMutableTreeNode gtn = (DefaultMutableTreeNode) root.getChildAt(i);
            final ISPTemplateGroup gn = (ISPTemplateGroup) ((NodeData) gtn.getUserObject()).getNode();
            for (int j = 0; j < gtn.getChildCount(); j++) {
                final DefaultMutableTreeNode ptn = (DefaultMutableTreeNode) gtn.getChildAt(j);
                final NodeData nodeData = (NodeData) ptn.getUserObject();
                if (nodeData.isOpen()) {
                    final ISPTemplateParameters pn = (ISPTemplateParameters) nodeData.getNode();
                    func.add(gn, pn);
                }
            }
        }

        // Exec the functor and propagate any exception
        final InstantiationFunctor f2 = SPDB.get().getQueryRunner(OT.getUser()).execute(func, node);
        //noinspection ThrowableResultOfMethodCallIgnored
        if (f2.getException() != null) throw f2.getException();

        // Nothing to return
        return null;
    }

    protected boolean isRelevant(Node n) {
        for (Class<? extends ISPNode> c: RELEVANT_TYPES) if (c.isInstance(n.getRemoteNode())) return true;
        return false;
    }

}

class InstantiationDialogRenderer extends TemplateDialogRenderer {

    private static final Icon ICON_SIDEREAL = Resources.getIcon("pit/sidereal.png");
    private static final Icon ICON_NONSIDEREAL = Resources.getIcon("pit/nonsidereal.png");
    private static final Icon ICON_CONDS = Resources.getIcon("pit/conds.png");

    InstantiationDialogRenderer() {
    }

    protected void decorate(NodeData nd) {
        // There's no UIInfo for ISPTemplateParameters so we have to handle this case ourselves
        if (nd.getNode() instanceof ISPTemplateParameters) {
            // Icon
            final TemplateParameters tps = (TemplateParameters) nd.getDataObject();
            final SPTarget spTarget = tps.getTarget();
            final Icon targetIcon = spTarget.isNonSidereal() ? ICON_NONSIDEREAL : ICON_SIDEREAL;
            setIcon(new DualIcon(targetIcon, ICON_CONDS));

            // Text
            final SPSiteQuality.Conditions conds = tps.getSiteQuality().conditions();
            setText(spTarget.getName() + " - " + conds);
        }
    }
}

