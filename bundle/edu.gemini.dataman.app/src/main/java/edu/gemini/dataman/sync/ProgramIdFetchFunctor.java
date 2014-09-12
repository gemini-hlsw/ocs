//
// $Id: ProgramIdFetchFunctor.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.dataman.sync;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.pot.spdb.*;

import java.security.Principal;
import java.util.*;

/**
 *
 */
final class ProgramIdFetchFunctor extends DBAbstractQueryFunctor implements IDBParallelFunctor {
    private List<SPProgramID> _progIds;

    @SuppressWarnings("unchecked")
    public synchronized List<SPProgramID> getProgIds() {
        if (_progIds == null) return Collections.EMPTY_LIST;
        return Collections.unmodifiableList(_progIds);
    }

    private synchronized List<SPProgramID> _getProgIds() {
        if (_progIds == null) {
            _progIds = new ArrayList<SPProgramID>(512);
        }
        return _progIds;
    }

    public synchronized void execute(IDBDatabaseService idbDatabase, ISPNode node, Set<Principal> principals) {
        SPProgramID progId = node.getProgramID();
        if (progId != null) {
            _getProgIds().add(node.getProgramID());
        }
    }

    public static List<SPProgramID> getProgramIds(IDBDatabaseService db, Set<Principal> user) {
        ProgramIdFetchFunctor func = new ProgramIdFetchFunctor();
        IDBQueryRunner qr = db.getQueryRunner(user);

        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        ClassLoader cl = func.getClass().getClassLoader();
        try {
            t.setContextClassLoader(cl);
            func = qr.queryPrograms(func);
            return func.getProgIds();
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    public void mergeResults(Collection<IDBFunctor> collection) {
        List<SPProgramID> progIds = new ArrayList<SPProgramID>();

        for (IDBFunctor fun : collection) {
            ProgramIdFetchFunctor piff = (ProgramIdFetchFunctor) fun;
            if (piff._progIds != null) {
                progIds.addAll(piff._progIds);
            }
        }

        _progIds = progIds;
    }
}
