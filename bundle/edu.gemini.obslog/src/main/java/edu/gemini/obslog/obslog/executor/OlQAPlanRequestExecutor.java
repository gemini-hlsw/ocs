package edu.gemini.obslog.obslog.executor;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.database.OlPersistenceException;
import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.obslog.obslog.OlDefaultObservingLog;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.obslog.IObservingLog;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.skycalc.ObservingNight;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.plan.NightlyRecord;

import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.Serializable;
import java.util.List;

//
// Gemini Observatory/AURA
// $Id: OlQAPlanRequestExecutor.java,v 1.2 2006/01/19 12:32:11 gillies Exp $
//

public class OlQAPlanRequestExecutor extends OlRequestExecutorBase implements Serializable {
    public static final Logger LOG = Logger.getLogger(OlQAPlanRequestExecutor.class.getName());

    private final static OlLogOptions DEFAULT_OPTIONS = new OlLogOptions();

    static {
        DEFAULT_OPTIONS.setMultiNight(false);
        DEFAULT_OPTIONS.setShowEmpties(false);
        DEFAULT_OPTIONS.setLimitConfigDatesByNight(null);
    }

    private SPProgramID _planID;
    private IObservingLog _observingLog;

    /**
     * Executor will use a plan to generate a list of observations and then provide data
     * for an Observing Log.
     *
     * @param manager indicates an implementation of <tt>OlPersistenceManager</tt>
     * @param planID  this is a {@link edu.gemini.spModel.core.SPProgramID} object.
     */
    public OlQAPlanRequestExecutor(OlPersistenceManager manager, OlConfiguration config, SPProgramID planID, Set<Principal> user) {
        this(manager, config, DEFAULT_OPTIONS, planID, user);
    }

    /**
     * Executor will use a plan to generate a list of observations.  This one allows setting the options.
     *
     * @param manager indicates an implementation of <tt>OlPersistenceManager</tt>
     * @param config
     * @param options
     * @param planID  this is a {@link edu.gemini.spModel.core.SPProgramID} object.
     */
    public OlQAPlanRequestExecutor(OlPersistenceManager manager, OlConfiguration config, OlLogOptions options, SPProgramID planID, Set<Principal> user) {
        super(manager, config, user);
        _planID = planID;
        _setObsLogOptions(options);
    }

    private void _setOptions(SPProgramID planID) {
        OlLogOptions options = new OlLogOptions();
        options.setOptions(DEFAULT_OPTIONS);
        // Assumes a plan ID of the form "GS-PLAN20050420";
        final int START_OF_DATE_IN_PLAN = 7;

        String planIdStr = planID.stringValue();

        String date = planIdStr.substring(START_OF_DATE_IN_PLAN);

        planIdStr = planIdStr.toLowerCase();
        Site site = planIdStr.startsWith("gs-") ? Site.GS : Site.GN;
        try {
            options.setLimitConfigDatesByNight(ObservingNight.parseNightString(date, site));
        } catch (java.text.ParseException ex) {
            LOG.log(Level.WARNING, "Failed to set data option:" + planID);
        }
        _setObsLogOptions(options);
    }

     /**
     * The created <tt>IObservingLog</tt> is returned.
     *
     * @return an instance of <tt>IObservingLog</tt>.
     */
    public IObservingLog getObservingLog() {
        return _observingLog;
    }

    /**
     * Construct the observing log given the set of observation ids passed with the constructor.
     *
     * @throws edu.gemini.obslog.obslog.OlLogException
     *          if an error occurs while constructing the observing log
     *          if an improper observation ID is passed
     */
    public void execute() throws OlLogException {
        // This is needed first to setup the observation list
        OlDefaultObservingLog obsLog = _getObservingLog();
        SPProgramID planID = _planID;

        NightlyRecord obsLogObj;
        try {
            obsLogObj = _getPersistenceManager().getNightlyRecord(planID);
        } catch (OlPersistenceException ex) {
            // Indicates that plan could not be found
            LOG.log(Level.INFO, "Creating plan: " + planID.stringValue());
            obsLogObj = _getPersistenceManager().createNightlyRecordBy(planID);
        }
        if (obsLogObj == null) throw new OlPersistenceException("database returned null obs log info after creation");

        List<SPObservationID> observationIDs = obsLogObj.getObservationList();
        if (observationIDs == null) throw new NullPointerException("observation list is null");

        PlanObservationListHandler obsListHandler = new PlanObservationListHandler(observationIDs);
        obsListHandler.apply();

        // This sets to only night's data
        _setOptions(planID);

        OlQARequestExecutor exec = new OlQARequestExecutor(_getPersistenceManager(), _getLogConfiguration(),
                                                           _getObsLogOptions(), obsListHandler.getFinalList(), _user);
        exec.execute();

        _observingLog = exec.getObservingLog();
    }

}
