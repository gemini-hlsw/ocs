package jsky.app.ot.editor.template;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPTemplateFolder;
import edu.gemini.pot.sp.ISPTemplateGroup;
import edu.gemini.pot.sp.ISPTemplateParameters;
import edu.gemini.spModel.template.SplitFunctor;
import edu.gemini.spModel.template.TemplateFolder;
import edu.gemini.spModel.util.DBTreeListService;
import jsky.app.ot.OT;
import jsky.app.ot.viewer.NodeData;

import java.awt.Component;


public class SplitDialog extends NodeSelector<Void> {

    private final ISPTemplateGroup groupNode;

    public SplitDialog(ISPTemplateGroup groupNode)  {
        super(groupNode, null);
        this.groupNode = groupNode;

        // TODO: make this faster; it's two remote calls
        final ISPTemplateFolder templateFolder = (ISPTemplateFolder) groupNode.getParent();
        final TemplateFolder templateFolderData = (TemplateFolder) templateFolder.getDataObject();
        tree.setCellRenderer(new InstantiationDialogRenderer(templateFolderData));

    }

    public static void open(Component parent, ISPTemplateGroup group) throws Exception {
        new SplitDialog(group).open(parent);
    }

    protected String getCaption() {
        return "Select targets/conditions you wish to move to the new template group:";
    }

    protected Void commit() throws Exception {
        final SplitFunctor functor = new SplitFunctor(groupNode);
        for (NodeData nd: getSelectedNodes()) {
            final ISPNode node = nd.getNode();
            if (node instanceof ISPTemplateParameters)
                functor.add((ISPTemplateParameters) node);
        }
        SPDB.get().getQueryRunner(OT.getUser()).execute(functor, null);
        return null;
    }

    protected boolean isRelevant(DBTreeListService.Node n) {
        final ISPNode rn = n.getRemoteNode();
        return rn instanceof ISPTemplateGroup ||
               rn instanceof ISPTemplateParameters;
    }

}
