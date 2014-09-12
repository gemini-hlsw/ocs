//
// $Id: QueuedObservationFunctor.java 3253 2007-01-29 19:22:36Z gillies $
//
package edu.gemini.wdba.glue;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.*;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.wdba.shared.QueuedObservation;


import java.security.Principal;
import java.util.*;

/**
 * A functor for extracting QueuedObservation objects for each (String)
 * observation id returned by the WDBA.
 */
public class QueuedObservationFunctor extends DBAbstractFunctor implements IDBParallelFunctor {
    private List<SPObservationID> _obsIds;
    private List<QueuedObservation> _results;

    private QueuedObservationFunctor(List<String> obsIdStrs) throws SPBadIDException {
        _obsIds = new ArrayList<SPObservationID>(obsIdStrs.size());
        for (String obsIdStr : obsIdStrs) {
            _obsIds.add(new SPObservationID(obsIdStr));
        }
    }

    public List<QueuedObservation> getResults() {
        if (_results == null) return Collections.emptyList();
        return Collections.unmodifiableList(_results);
    }

    public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
        if (_obsIds == null) return;

        _results = new ArrayList<QueuedObservation>();
//        try {
            for (SPObservationID obsId : _obsIds) {
                ISPObservation obs = db.lookupObservationByID(obsId);
                // Can be null if running in a slave and the obs isn't in this
                // particular slave database.
                if (obs == null) continue;

                SPObservation dataObj = (SPObservation) obs.getDataObject();
                _results.add(new QueuedObservation(obsId, dataObj.getTitle()));
            }
//        } catch (RemoteException ex) {
//            throw new RuntimeException("RemoteException thrown within functor");
//        }
    }

    public static List<QueuedObservation> getQueuedObservations(IDBDatabaseService db, List<String> obsIdStrs, Set<Principal> user)
            throws SPBadIDException {
        QueuedObservationFunctor func = new QueuedObservationFunctor(obsIdStrs);
        try {
            func = db.getQueryRunner(user).execute(func, null);
        } catch (SPNodeNotLocalException ex) {
            // shouldn't happen ...
            throw new RuntimeException("passed in a null node, but wasn't local!");
        }
        return func.getResults();
    }

    public void mergeResults(Collection<IDBFunctor> functorCollection) {
        List<QueuedObservation> res = new ArrayList<QueuedObservation>();
        for (IDBFunctor f : functorCollection) {
            res.addAll(((QueuedObservationFunctor) f).getResults());
        }
        _results = res;
    }
}
