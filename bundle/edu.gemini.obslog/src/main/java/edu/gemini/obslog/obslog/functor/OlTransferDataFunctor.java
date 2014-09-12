package edu.gemini.obslog.obslog.functor;

import edu.gemini.obslog.transfer.EObslogVisit;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.IDBDatabaseService;


import java.security.Principal;
import java.util.List;
import java.util.Set;

//
// Gemini Observatory/AURA
// $Id: OlTransferDataFunctor.java,v 1.2 2006/10/17 21:37:11 shane Exp $
//

public abstract class OlTransferDataFunctor extends BaseTransferDataFunctor {
    //private static final Logger LOG = LogUtil.getLogger(OlTransferDataFunctor.class);

    private List<EObslogVisit> _result;

    public OlTransferDataFunctor(OlLogOptions obsLogOptions, List<SPObservationID> observationIDs) {
        super(obsLogOptions, observationIDs);
    }

    public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
//        try {
            List<ISPObservation> observations = _fetchObservations(db);
            _result = fetchObservationData(observations);

//        } catch (RemoteException ex) {
            //LOG.error("Remote exception in local code!", ex);
//            throw GeminiRuntimeException.newException(ex);
//        }
    }

    /**
     * Gets the created <tt>IObservingLog</tt> instance.
     *
     * @return the log
     */
    public List<EObslogVisit> getResult() {
        return _result;
    }

    protected void setResult(List<EObslogVisit> res) {
        _result = res;
    }

    /**
     * This private method takes a list of {@link edu.gemini.pot.sp.ISPObservation} instances and constructs
     * their ObservationData objects.
     *
     * @param observations
     * @return a new <tt>List</tt> of <tt>ObservationData</tt> objects.
     * @throws java.rmi.RemoteException
     */
    abstract protected List<EObslogVisit> fetchObservationData(List<ISPObservation> observations) ;

}
