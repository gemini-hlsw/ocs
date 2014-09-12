package edu.gemini.obslog.obslog.functor;

import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBParallelFunctor;


import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//
// Gemini Observatory/AURA
// $Id: BaseTransferDataFunctor.java,v 1.2 2006/10/17 21:37:11 shane Exp $
//

public abstract class BaseTransferDataFunctor extends DBAbstractFunctor implements IDBParallelFunctor {
//    private static final Logger LOG = LogUtil.getLogger(BaseTransferDataFunctor.class);

    private List<SPObservationID> _observationIDs;
    private OlLogOptions _obsLogOptions;

    protected BaseTransferDataFunctor(OlLogOptions obsLogOptions, List<SPObservationID> observationIDs) {
        if (observationIDs == null) throw new NullPointerException("null observation list");
        if (obsLogOptions == null) throw new NullPointerException("null obslog options");

        _observationIDs = observationIDs;
        _obsLogOptions = obsLogOptions;
    }

    /**
     * This private method uses the list of {@link edu.gemini.pot.sp.SPObservationID} objects to fetch
     * all the observations required by the log.
     *
     * @return A new <tt>List</tt> of {@link edu.gemini.pot.sp.ISPObservation} objects.
     */
    protected List<ISPObservation> _fetchObservations(IDBDatabaseService db)  {
        ArrayList<ISPObservation> fetched = new ArrayList<ISPObservation>();

        for (SPObservationID obsID :  _observationIDs) {
            ISPObservation obs = db.lookupObservationByID(obsID);
            // Can be null if running in a master/slave configuration.
            // An observation can only be in one particular slave.
            if (obs == null) continue;
//                LOG.error("Requested observation was not in database: " + obsID.stringValue());
            fetched.add(obs);
        }
        return fetched;
    }

    /**
     * Provide the request options.
     *
     * @return an <code>{@link edu.gemini.obslog.obslog.OlLogOptions}</code> object.
     */
    protected OlLogOptions _getObsLogOptions() {
        return _obsLogOptions;
    }

    abstract public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals);

}
