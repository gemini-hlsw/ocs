package edu.gemini.obslog.obslog.executor;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.obslog.obslog.*;
import edu.gemini.obslog.obslog.functor.OlObsVisitFunctor;
import edu.gemini.obslog.transfer.EObslogVisit;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.IDBDatabaseService;

import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//
// Gemini Observatory/AURA
// $Id: OlRequestExecutorBase.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

/**
 * A base class for assisting in the building of <code>IObservingLog</code> instances from lists of
 * observation IDs that are <code>Strings</code>.
 */
public abstract class OlRequestExecutorBase implements Serializable {
    public static final Logger LOG = Logger.getLogger(OlRequestExecutorBase.class.getName());

    // List of Strings that are observationIDs
    private List<SPObservationID> _observationIDs;

    private OlPersistenceManager _persistenceManager;
    private OlConfiguration _configuration;

    private OlDefaultObservingLog _obsLog;
    private OlLogOptions _obsLogOptions;

    protected final Set<Principal> _user;

    /**
     * Executor will use a program to generate a list of observations and then provide data
     * for an Observing Log.
     *
     * @param manager indicates an implementation of <tt>OlPersistenceManager</tt>
     * @param config  this is a {@link OlConfiguration} object.
     */
    public OlRequestExecutorBase(OlPersistenceManager manager, OlConfiguration config, Set<Principal> user) {
        if (manager == null || config == null) throw new NullPointerException();

        _persistenceManager = manager;
        _configuration = config;
        _obsLogOptions = new OlLogOptions();
        _user = user;
    }

    protected OlDefaultObservingLog _getObservingLog() {
        if (_obsLog == null) {
            _obsLog = new OlDefaultObservingLog();
        }
        return _obsLog;
    }

    protected OlPersistenceManager _getPersistenceManager() {
        return _persistenceManager;
    }

    protected OlConfiguration _getLogConfiguration() {
        return _configuration;
    }

    protected OlLogOptions _getObsLogOptions() {
        return _obsLogOptions;
    }

    protected void _setObsLogOptions(OlLogOptions options) {
        if (options == null) {
            LOG.log(Level.INFO, "Action set null options");
            return;
        }
        _obsLogOptions.setOptions(options);
    }

    /**
     * The created <tt>IObservingLog</tt> is returned.
     *
     * @return an instance of <tt>IObservingLog</tt>.
     */
    public IObservingLog getObservingLog() {
        return _getObservingLog();
    }

    protected List<SPObservationID> _getObservationIDs() {
        if (_observationIDs == null) {
            _observationIDs = new ArrayList<>();
        }
        return _observationIDs;
    }

    protected void _buildSegments(IObservingLog obsLog, List<EObslogVisit> observationDataList) throws OlLogException {
        if (observationDataList == null || observationDataList.size() == 0 || obsLog == null) return;

        InstrumentSegmentBuilder.create(obsLog, _configuration, _obsLogOptions, observationDataList);
    }

    /**
     * This private method takes a list of {@link edu.gemini.pot.sp.ISPObservation} instances and constructs
     * their ObservationData objects.
     *
     * @param observationIDs
     * @return a new <tt>List</tt> of <tt>ObservationData</tt> objects.
     * @throws OlLogException
     */
    protected List<EObslogVisit> _fetchObservationData(List<SPObservationID> observationIDs) throws OlLogException {
        IDBDatabaseService db = _persistenceManager.getDatabase();
        OlObsVisitFunctor lf = new OlObsVisitFunctor(_obsLogOptions, observationIDs);
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
     *                        if an improper observation ID is passed
     */
    public abstract void execute() throws OlLogException;

}
