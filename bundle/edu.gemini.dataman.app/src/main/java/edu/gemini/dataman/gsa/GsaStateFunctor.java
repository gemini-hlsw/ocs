//
// $Id: GsaStateFunctor.java 702 2006-12-17 14:18:56Z shane $
//

package edu.gemini.dataman.gsa;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBFunctor;
import edu.gemini.pot.spdb.IDBParallelFunctor;
import edu.gemini.spModel.dataset.DatasetExecRecord;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.GsaState;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.obsrecord.ObsExecRecord;

import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The GsaStateFunctor collects lists of datasets in significant
 * {@link GsaState}s.  Significant states are those which should trigger some
 * further action.  See {@link GsaVigilanteTask}, which executes this functor,
 * for more information.
 */
final class GsaStateFunctor extends DBAbstractQueryFunctor implements IDBParallelFunctor {
    private static final Logger LOG = Logger.getLogger(GsaStateFunctor.class.getName());

    private Map<GsaState, List<GsaDatasetInfo>> _map;
    private GsaState[] _states;

    GsaStateFunctor(GsaState[] states) {
        _states = new GsaState[states.length];
        System.arraycopy(states, 0, _states, 0, states.length);
    }

    public List<GsaDatasetInfo> getLabels(GsaState state) {
        return new ArrayList<GsaDatasetInfo>(_getMap().get(state));
    }

    private Map<GsaState, List<GsaDatasetInfo>> _getMap() {
        if (_map == null) {
            _map = new HashMap<GsaState, List<GsaDatasetInfo>>();

            for (GsaState state : _states) {
                _map.put(state, new ArrayList<GsaDatasetInfo>());
            }
        }
        return new HashMap<GsaState, List<GsaDatasetInfo>>(_map);
    }

    public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
        ISPProgram prog = (ISPProgram) node;
        for (ISPObservation obs : prog.getAllObservations()) {
            _scanObservation(obs);
        }
    }

    @SuppressWarnings("unchecked")
    private void _scanObservation(ISPObservation obs) {
        final SPObservationID obsId = obs.getObservationID();
        if (obsId == null) return;

        final ISPObsExecLog lc = obs.getObsExecLog();
        if (lc == null) return;

        final ObsExecLog log = (ObsExecLog) lc.getDataObject();
        final ObsExecRecord or = log.getRecord();
        final List<DatasetExecRecord> recs = or.getAllDatasetExecRecords();
        if (recs.size() == 0) return;

        final Map<GsaState, List<GsaDatasetInfo>> map = _getMap();

        for (DatasetExecRecord rec : recs) {
            final GsaState gsaState = rec.getGsaState();
            final List<GsaDatasetInfo> lst = map.get(gsaState);
            if (lst == null) continue;

            final DatasetLabel label = rec.getLabel();
            final String filename = rec.getDataset().getDhsFilename();
            lst.add(new GsaDatasetInfo(label, filename));
        }
    }

    public static  GsaStateFunctor execute(IDBDatabaseService db, GsaState[] states, Set<Principal> user) {
        GsaStateFunctor func = new GsaStateFunctor(states);

        Thread t = Thread.currentThread();
        ClassLoader prev = t.getContextClassLoader();
        ClassLoader cl = func.getClass().getClassLoader();
        try {
            t.setContextClassLoader(cl);
            return db.getQueryRunner(user).queryPrograms(func);
        } finally {
            t.setContextClassLoader(prev);
        }
    }

    // merge results if running in the cluster ODB
    public void mergeResults(Collection<IDBFunctor> collection) {
        Map<GsaState, List<GsaDatasetInfo>> map;
        map = new HashMap<GsaState, List<GsaDatasetInfo>>();

        for (IDBFunctor fun : collection) {
            GsaStateFunctor gsf = (GsaStateFunctor) fun;
            if (gsf._map == null) continue;

            for (Map.Entry<GsaState, List<GsaDatasetInfo>> me : gsf._map.entrySet()) {
                GsaState curState = me.getKey();
                List<GsaDatasetInfo> curInfoList = me.getValue();

                List<GsaDatasetInfo> masterInfoList = map.get(curState);
                if (masterInfoList == null) {
                    map.put(curState, new ArrayList<GsaDatasetInfo>(curInfoList));
                } else {
                    masterInfoList.addAll(curInfoList);
                }
            }
        }

        if (map.size() > 0) _map = map;
    }

    /**
     * Creates a map keyed by {@link GsaState} whose value is the list of
     * datasets with the corresponding state in the database.
     *
     * @param dbs set of databases in which to look for the information; when
     * a particular database is successfully called, any remaining databases
     * are ignored
     *
     * @param states the GsaStates of interest
     *
     * @return map of GsaState to a list of datasets in that state
     */
    public static Map<GsaState, List<GsaDatasetInfo>> getStateMap(Set<IDBDatabaseService> dbs, GsaState[] states, Set<Principal> user) {
        if ((dbs == null) || (dbs.size() == 0)) {
            LOG.warning("Could not check GsaState, the database is not available");
            return Collections.emptyMap();
        }

        if (dbs.size() > 1) {
            LOG.warning("Checking GsaState, multiple databases were found");
        }

        for (IDBDatabaseService db : dbs) {
            LOG.log(Level.FINE, "Checking GsaState info in database: " + db);
            GsaStateFunctor func;
            func = GsaStateFunctor.execute(db, states, user);

            // Check for problems running the functor.
            Exception problem = func.getException();
            if (problem != null) {
                LOG.log(Level.SEVERE, "GsaStateFunctor problem", problem);
                continue;
            }

            return func._getMap();
        }

        LOG.log(Level.WARNING, "Could not obtain GsaState information");
        return Collections.emptyMap();
    }
}
