package edu.gemini.obslog.obslog.functor;

import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.transfer.EObslogVisit;
import edu.gemini.obslog.transfer.ObsRecordDatasetFactory;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPNodeNotLocalException;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBFunctor;
import edu.gemini.shared.util.GeminiRuntimeException;


import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Set;

//
// Gemini Observatory/AURA
// $Id: OlObservationDatasetFunctor.java,v 1.2 2006/10/17 21:37:11 shane Exp $
//

public final class OlObservationDatasetFunctor extends OlTransferDataFunctor {

    public OlObservationDatasetFunctor(OlLogOptions obsLogOptions, List<SPObservationID> observationIDs) {
        super(obsLogOptions, observationIDs);
    }

    /**
     * This private method takes a list of {@link edu.gemini.pot.sp.ISPObservation} instances and constructs
     * their ObservationData objects.
     *
     * @param observations
     * @return a new <tt>List</tt> of <tt>ObservationData</tt> objects.
     * @throws java.rmi.RemoteException
     */
    protected List<EObslogVisit> fetchObservationData(List<ISPObservation> observations)  {
        List<EObslogVisit> obsData = new ArrayList<EObslogVisit>();

        for (int i = 0, size = observations.size(); i < size; i++) {
            List<EObslogVisit> od = ObsRecordDatasetFactory.build(observations.get(i));
            obsData.addAll(od);
        }

        return obsData;
    }

    public static List<EObslogVisit> create(IDBDatabaseService db, OlLogOptions obsLogOptions, List<SPObservationID> observationIDs, Set<Principal> user)  {

        OlObservationDatasetFunctor lf = new OlObservationDatasetFunctor(obsLogOptions, observationIDs);
        try {
            lf = db.getQueryRunner(user).execute(lf, null);
        } catch (SPNodeNotLocalException ex) {
            // node is null so this cannot happen
            throw GeminiRuntimeException.newException(ex);
        }
        return lf.getResult();
    }

    public void mergeResults(Collection<IDBFunctor> functorCollection) {
        List<EObslogVisit> res = new ArrayList<EObslogVisit>();
        for (IDBFunctor f : functorCollection) {
            res.addAll(((OlObservationDatasetFunctor) f).getResult());
        }
        setResult(res);
    }
}
