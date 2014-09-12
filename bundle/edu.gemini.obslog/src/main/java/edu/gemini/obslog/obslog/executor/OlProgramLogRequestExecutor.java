package edu.gemini.obslog.obslog.executor;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.obslog.transfer.EObslogVisit;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.OlDefaultObservingLog;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.SPProgramID;

import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.Serializable;
import java.util.List;

//
// Gemini Observatory/AURA
// $Id: OlProgramLogRequestExecutor.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public class OlProgramLogRequestExecutor extends OlRequestExecutorBase implements Serializable {
    public static final Logger LOG = Logger.getLogger(OlProgramLogRequestExecutor.class.getName());

    private final static OlLogOptions DEFAULT_OPTIONS = new OlLogOptions();

    static {
        DEFAULT_OPTIONS.setMultiNight(true);
        DEFAULT_OPTIONS.setShowEmpties(false);
    }

    private SPProgramID _programID;

    /**
     * Executor will use a program to generate a list of observations and then provide data
     * for an Observing Log.
     *
     * @param manager   indicates an implementation of <tt>OlPersistenceManager</tt>
     * @param programID this is a {@link edu.gemini.spModel.core.SPProgramID} object.
     */
    public OlProgramLogRequestExecutor(OlPersistenceManager manager, OlConfiguration config, SPProgramID programID, Set<Principal> user) {
        this(manager, config, DEFAULT_OPTIONS, programID, user);
    }

    /**
     * Executor will use a program to generate a list of observations and then provide data
     * for an Observing Log.
     *
     * @param manager   indicates an implementation of <tt>OlPersistenceManager</tt>
     * @param programID this is a {@link edu.gemini.spModel.core.SPProgramID} object.
     */
    public OlProgramLogRequestExecutor(OlPersistenceManager manager, OlConfiguration config, OlLogOptions options, SPProgramID programID, Set<Principal> user) {
        super(manager, config, user);

        if (programID == null) throw new NullPointerException("programID can not be null");
        _programID = programID;
        _setObsLogOptions(options);
    }

    /**
     * Construct the observing log given the set of observation ids passed with the constructor.
     *
     * @throws edu.gemini.obslog.obslog.OlLogException if an error occurs while constructing the observing log
     *                        if an improper observation ID is passed
     */
    public void execute() throws OlLogException {
        // This is neede dfirst to setup the observation list
        OlDefaultObservingLog obsLog = _getObservingLog();

        List<SPObservationID> observationIDs = _getPersistenceManager().getProgramObservations(_programID);
        if (observationIDs == null) return;

        List<EObslogVisit> obsDataList = _fetchObservationData(observationIDs);
        if (obsDataList == null) return;

        _buildSegments(obsLog, obsDataList);
    }

}
