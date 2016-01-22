package edu.gemini.obslog.obslog.functor;

import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.transfer.EChargeObslogVisit;
import edu.gemini.obslog.transfer.EObslogVisit;
import edu.gemini.obslog.transfer.ObservationObsVisitTimeFactory;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPNodeNotLocalException;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBFunctor;
import edu.gemini.shared.util.GeminiRuntimeException;


import java.security.Principal;
import java.util.*;

public final class OlObsVisitTimeFunctor extends BaseTransferDataFunctor {
    private List<EChargeObslogVisit> _result;

    public OlObsVisitTimeFunctor(OlLogOptions obsLogOptions, List<SPObservationID> observationIDs) {
        super(obsLogOptions, observationIDs);
    }

    /**
     * This private method takes a list of {@link edu.gemini.pot.sp.ISPObservation} instances and constructs
     * their ObservationData objects.
     *
     * @return a new <tt>List</tt> of <tt>ObservationData</tt> objects.
     */
    private List<EChargeObslogVisit> fetchObservationData(List<ISPObservation> observations)  {
        List<EChargeObslogVisit> obsData = new ArrayList<>();

        for (ISPObservation observation : observations) {
            List<EChargeObslogVisit> od = ObservationObsVisitTimeFactory.build(observation, _getObsLogOptions());
            obsData.addAll(od);
        }

        // Sort by start time, all the list elements using the start time of the configs
        Collections.sort(obsData, EObslogVisit.CONFIG_TIME_COMPARATOR);
        return obsData;
    }

    @Override
    public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
        List<ISPObservation> observations = _fetchObservations(db);
        _result = fetchObservationData(observations);
    }

    /**
     * Gets the created <tt>IObservingLog</tt> instance.
     *
     * @return the log
     */
    public List<EChargeObslogVisit> getResult() {
        return _result;
    }


    public static List<EChargeObslogVisit> create(IDBDatabaseService db, OlLogOptions obsLogOptions, List<SPObservationID> observationIDs, Set<Principal> user)  {

        OlObsVisitTimeFunctor lf = new OlObsVisitTimeFunctor(obsLogOptions, observationIDs);
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
        List<EChargeObslogVisit> res = new ArrayList<>();
        for (IDBFunctor f : functorCollection) {
            res.addAll(((OlObsVisitTimeFunctor) f).getResult());
        }
        // Sort by start time, all the list elements using the start time of the configs
        Collections.sort(res, EObslogVisit.CONFIG_TIME_COMPARATOR);
        _result = res;
    }
}
