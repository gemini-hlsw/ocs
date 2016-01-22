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
     */
    @Override
    protected List<EObslogVisit> fetchObservationData(List<ISPObservation> observations)  {
        List<EObslogVisit> obsData = new ArrayList<>();

        for (ISPObservation observation : observations) {
            List<EObslogVisit> od = ObservationObsVisitsFactory.build(observation, _getObsLogOptions());
            obsData.addAll(od);
        }

        // Sort by start time, all the list elements using the start time of the configs
        Collections.sort(obsData, EObslogVisit.CONFIG_TIME_COMPARATOR);
        return obsData;
    }

    public static List<EObslogVisit> create(IDBDatabaseService db, OlLogOptions obsLogOptions, List<SPObservationID> observationIDs, Set<Principal> user)  {

        OlObsVisitFunctor lf = new OlObsVisitFunctor(obsLogOptions, observationIDs);
        try {
            lf = db.getQueryRunner(user).execute(lf, null);
        } catch (SPNodeNotLocalException ex) {
            // node is null so this cannot happen
            throw GeminiRuntimeException.newException(ex);
        }
        return lf.getResult();
    }

    @Override
    public void mergeResults(Collection<IDBFunctor> functorCollection) {
        List<EObslogVisit> res = new ArrayList<>();
        for (IDBFunctor f : functorCollection) {
            res.addAll(((OlObsVisitFunctor) f).getResult());
        }

        // Sort by start time, all the list elements using the start time of the configs
        Collections.sort(res, EObslogVisit.CONFIG_TIME_COMPARATOR);
        setResult(res);
    }
}
