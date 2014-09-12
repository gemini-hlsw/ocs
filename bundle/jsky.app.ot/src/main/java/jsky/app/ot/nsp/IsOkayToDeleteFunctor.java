//
// $
//

package jsky.app.ot.nsp;

import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obslog.ObsExecLog;
import jsky.app.ot.OT;

import java.security.Principal;
import java.util.Set;


/**
 * Checks whether a given node is okay to delete.
 */
final class IsOkayToDeleteFunctor extends DBAbstractFunctor {
    private boolean result;

    public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
        result = isOkayToDelete(node);
    }

    private boolean isOkayToDelete(ISPNode node)  {
        // Ugh, instead of explicitly saying what we can't delete, perhaps it
        // should default to not deletable.
        if (node instanceof ISPGroup) {
            ISPGroup group = (ISPGroup)node;
            for (ISPObservation ispObservation : group.getObservations()) {
                if (!isOkayToDelete(ispObservation)) return false;
            }
        } else if (node instanceof ISPObservation) {
            final ISPObservation obs = (ISPObservation)node;
            if (ObservationStatus.computeFor(obs) == ObservationStatus.OBSERVED) return false;
            final ISPObsExecLog log = obs.getObsExecLog();
            if (log != null) return ((ObsExecLog) log.getDataObject()).isEmpty();
        } else if (node instanceof ISPTemplateGroup) {
            ISPTemplateGroup tg = (ISPTemplateGroup) node;
            return tg.getTemplateParameters().isEmpty();
        } else if (node instanceof ISPTemplateFolder) {
            return false;
        } else if (node instanceof ISPObsQaLog) {
            return false;
        } else if (node instanceof ISPObsExecLog) {
            return false;
        } else if (node instanceof ISPProgram) {
            return false;
        } else if (node instanceof ISPSeqComponent) {
            return ((ISPSeqComponent) node).getType() != SPComponentType.ITERATOR_BASE;
        }
        return true;
    }

    public static boolean check(IDBDatabaseService db, ISPNode node) {
        final IsOkayToDeleteFunctor func = new IsOkayToDeleteFunctor();
        try {
            return db.getQueryRunner(OT.getUser()).execute(func, node).result;
        } catch (SPNodeNotLocalException ex) {
            throw new RuntimeException("Node not local", ex);
        }
    }
}
