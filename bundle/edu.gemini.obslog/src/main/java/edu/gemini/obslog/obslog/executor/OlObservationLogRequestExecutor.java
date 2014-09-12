package edu.gemini.obslog.obslog.executor;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.obslog.obslog.OlDefaultObservingLog;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.obslog.functor.OlObservationDatasetFunctor;
import edu.gemini.obslog.transfer.EObslogVisit;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.IDBDatabaseService;

import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.Serializable;
import java.util.List;

//
// Gemini Observatory/AURA
// $Id: OlObservationLogRequestExecutor.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public class OlObservationLogRequestExecutor extends OlRequestExecutorBase implements Serializable {
    public static final Logger LOG = Logger.getLogger(OlObservationLogRequestExecutor.class.getName());

    private List<SPObservationID> _observationIDs;

    /**
     * Executor will look up observations and build the segments returning an <tt>IObservingLog</tt> instance.
     *
     * @param manager        indicates an implementation of <tt>OlPersistenceManager</tt>
     * @param observationIDs this is a <tt>List</tt> of {@link SPObservationID} objects.
     */
    public OlObservationLogRequestExecutor(OlPersistenceManager manager, OlConfiguration config, List<SPObservationID> observationIDs, Set<Principal> user) {
        super(manager, config, user);

        _observationIDs = observationIDs;
    }

    /**
     * This private method takes a list of {@link edu.gemini.pot.sp.ISPObservation} instances and constructs
     * their ObservationData objects.
     *
     * @param observationIDs
     * @return a new <tt>List</tt> of <tt>ObservationData</tt> objects.
     * @throws edu.gemini.obslog.obslog.OlLogException
     *
     */
    protected List<EObslogVisit> _fetchObservationData(List<SPObservationID> observationIDs) throws OlLogException {
        IDBDatabaseService db = _getPersistenceManager().getDatabase();
        OlObservationDatasetFunctor lf = new OlObservationDatasetFunctor(_getObsLogOptions(), observationIDs);
        List<EObslogVisit> result;
        try {
            lf = db.getQueryRunner(_user).execute(lf, null);
        } catch (Exception ex) {
            throw new OlLogException(ex);
        }

        result = lf.getResult();
        if (result == null) {
            throw new OlLogException(lf.getException());
        }
        return result;
    }

    /**
     * Construct the observing log given the set of observation ids passed with the constructor.
     *
     * @throws OlLogException if an error occurs while constructing the observing log
     */
    public void execute() throws OlLogException {
        OlDefaultObservingLog obsLog = _getObservingLog();

        List<EObslogVisit> obsDataList = _fetchObservationData(_observationIDs);

        OlLogOptions options = new OlLogOptions();
        options.setMultiNight(true);
        _setObsLogOptions(options);

        _buildSegments(obsLog, obsDataList);
    }


}
