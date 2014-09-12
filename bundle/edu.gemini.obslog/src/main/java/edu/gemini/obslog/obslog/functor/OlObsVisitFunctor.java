package edu.gemini.obslog.obslog.functor;

import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.transfer.EObslogVisit;
import edu.gemini.obslog.transfer.ObservationObsVisitsFactory;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPNodeNotLocalException;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBFunctor;
import edu.gemini.shared.util.GeminiRuntimeException;


import java.security.Principal;
import java.util.*;

//
// Gemini Observatory/AURA
// $Id: OlObsVisitFunctor.java,v 1.3 2006/12/05 14:56:16 gillies Exp $
//

public final class OlObsVisitFunctor extends OlTransferDataFunctor {

    public OlObsVisitFunctor(OlLogOptions obsLogOptions, List<SPObservationID> observationIDs) {
        super(obsLogOptions, observationIDs);
    }

    /**
     * This private method takes a list of {@link edu.gemini.pot.sp.ISPObservation} instances and constructs
     * their ObservationData objects.
     *
     * @param observations the list of observations that should be fetched from the database
     * @return a new <tt>List</tt> of <tt>ObservationData</tt> objects.
     * @throws java.rmi.RemoteException
     */
    protected List<EObslogVisit> fetchObservationData(List<ISPObservation> observations)  {
        List<EObslogVisit> obsData = new ArrayList<EObslogVisit>();

        for (int i = 0, size = observations.size(); i < size; i++) {
            List<EObslogVisit> od = ObservationObsVisitsFactory.build(observations.get(i), _getObsLogOptions());
            obsData.addAll(od);
        }

        // Sort by start time, all the list elements using the start time of the configs
        Collections.sort(obsData, EObslogVisit.CONFIG_TIME_COMPARATOR);
        return obsData;
    }

    public static List create(IDBDatabaseService db, OlLogOptions obsLogOptions, List<SPObservationID> observationIDs, Set<Principal> user)  {

        OlObsVisitFunctor lf = new OlObsVisitFunctor(obsLogOptions, observationIDs);
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
            res.addAll(((OlObsVisitFunctor) f).getResult());
        }

        // Sort by start time, all the list elements using the start time of the configs
        Collections.sort(res, EObslogVisit.CONFIG_TIME_COMPARATOR);
        setResult(res);
    }
}
