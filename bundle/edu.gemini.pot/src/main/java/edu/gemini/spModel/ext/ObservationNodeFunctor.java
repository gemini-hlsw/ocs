//
// $
//

package edu.gemini.spModel.ext;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPProgramNode;
import edu.gemini.pot.sp.SPNodeNotLocalException;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;

import java.security.Principal;
import java.util.Set;


/**
 * A functor used to extract an {@link ObservationNode} from the specified
 * program node.  It figures out which observation the given node is in, and
 * creates the ObservationNode wrapper with it.
 */
public final class ObservationNodeFunctor extends DBAbstractFunctor {

    private ObservationNode result;

    // Prevent construction outside of the class.  Clients should use the
    // static public method.
    private ObservationNodeFunctor() {
    }

    // Search up the tree of program nodes until we come to the containing
    // observation or have examined all the parents.
    private ISPObservation findObservation(ISPNode node)  {
        if (node == null) return null;
        if (node instanceof ISPObservation) {
            return (ISPObservation) node;
        }
        return findObservation(node.getParent());
    }

    private ObservationNode getObservationNode() {
        return result;
    }

    /**
     * Called by the ODB when the functor executes.  Clients should use
     * {@link #getObservationNode(edu.gemini.pot.spdb.IDBDatabaseService, edu.gemini.pot.sp.ISPNode)}.
     */
    public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
        final ISPProgramNode progNode = (ISPProgramNode) node;
        ISPObservation obs = findObservation(node);
        result = new ObservationNode(obs);
    }

    /**
     * Obtains the {@link ObservationNode} wrapper corresponding to the
     * observation in which the given <code>node</code> finds itself.  If the
     * given <code>node</code> is not contained in an observation, returns
     * <code>null</code>
     *
     * @param db ODB instance in which the <code>node</code> lives
     * @param node remote program node from which to create the ObservationNode
     * wrapper
     *
     * @return {@link ObservationNode} corresponding to the observation in which
     * the given <code>node</code> lives, if any; <code>null</code> if not
     * part of an observation
     *
     * @ if there is a problem communicating with the ODB
     */
    public static ObservationNode getObservationNode(IDBDatabaseService db, ISPNode node, Set<Principal> user)  {
        ObservationNodeFunctor fun = new ObservationNodeFunctor();
        try {
            fun = db.getQueryRunner(user).execute(fun, node);
        } catch (SPNodeNotLocalException ex) {
            throw new RuntimeException(ex);
        }
        return fun.getObservationNode();
    }
}
