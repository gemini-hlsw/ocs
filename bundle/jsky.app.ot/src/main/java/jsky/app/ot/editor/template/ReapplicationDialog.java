package jsky.app.ot.editor.template;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.*;
import edu.gemini.spModel.gemini.security.UserRolePrivileges;
import edu.gemini.spModel.template.ReapplicationCheckFunctor;
import edu.gemini.spModel.template.ReapplicationFunctor;
import edu.gemini.spModel.util.DBTreeListService;
import jsky.app.ot.OT;
import jsky.app.ot.viewer.NodeData;
import jsky.util.gui.DialogUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Component;

import java.util.*;

import static edu.gemini.spModel.util.DBTreeListService.Node;

public class ReapplicationDialog extends NodeSelector<Void> {

    public static void open(Component parent, ISPProgram program, ISPNode context) throws Exception {
        final ReapplicationDialog rd = new ReapplicationDialog(program, context, ReapplicationHelpers.currentUserRolePrivileges());
        if (rd.isEmpty()) {
            DialogUtil.message(parent, "There are currently no observations available for template reapplication.");
        } else {
            rd.open(parent);
        }
    }

    private ReapplicationDialog(ISPContainerNode rootNode, ISPNode contextNode, UserRolePrivileges urps)  {
        super(rootNode, contextNode, urps);
    }

    protected String getCaption() {
        return "Select observations for which you wish to re-apply templates.";
    }

//    public Void getResults() {
//        return null;
//    }

    protected Void commit() throws Exception {

        // Prepare the functor
        final ReapplicationFunctor func = new ReapplicationFunctor(getUserRolePrivileges());
        for (NodeData nd : getSelectedNodes())
            if (nd.getNode() instanceof ISPObservation)
                func.add((ISPObservation) nd.getNode());

        // Exec the functor
        final ReapplicationFunctor f2 = SPDB.get().getQueryRunner(OT.getUser()).execute(func, null);
        if (f2.getException() != null)
            throw f2.getException();

        // Done
        return null;

    }

    // Some caches
    private Map<ISPObservation, Boolean> reapplyCheck = null;

    protected DefaultMutableTreeNode treeNode(final DBTreeListService.Node n, DBTreeListService.Node context, boolean select) {

        // Ugh
        if (reapplyCheck == null) {
            final ReapplicationCheckFunctor checker = new ReapplicationCheckFunctor(getUserRolePrivileges());
            final LinkedList<DBTreeListService.Node> queue = new LinkedList<Node>();
            queue.addLast(n);
            while (!queue.isEmpty()) {
                final DBTreeListService.Node a = queue.removeFirst();
                if (a.getRemoteNode() instanceof ISPObservation)
                    checker.add((ISPObservation) a.getRemoteNode());
                queue.addAll(a.getSubNodes());
            }
            try {
                reapplyCheck = SPDB.get().getQueryRunner(OT.getUser()).execute(checker, null).getResults();
            } catch (SPNodeNotLocalException e) {
                DialogUtil.error(e);
                setVisible(false);
//            } catch (RemoteException e) {
//                DialogUtil.error(e);
//                setVisible(false);
            }
        }

        return super.treeNode(n, context, select);

    }

    protected boolean isRelevant(Node n) {

        // Relevant if it's an obs that can be reapplied
        if (n.getRemoteNode() instanceof ISPObservation)
            return reapplyCheck.get(n.getRemoteNode());

        // Relevant if it has a relevant child
        for (Node c : n.getSubNodes())
            if (isRelevant(c))
                return true;

        return false;

    }


}

