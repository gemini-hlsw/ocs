package edu.gemini.obslog.obslog.executor;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.config.model.OlObsLogData;
import edu.gemini.obslog.database.OlPersistenceException;
import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.obslog.instruments.WeatherSegment;
import edu.gemini.obslog.obslog.OlDefaultObservingLog;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.OlLogInformation;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.transfer.EObslogVisit;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.skycalc.ObservingNight;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.plan.NightlyRecord;
import edu.gemini.spModel.gemini.plan.WeatherInfo;
import edu.gemini.spModel.prog.GemPlanId;

import java.security.Principal;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.Serializable;
import java.util.*;

//
// Gemini Observatory/AURA
// $Id: OlPlanLogRequestExecutor.java,v 1.2 2006/12/05 14:56:16 gillies Exp $
//

public class OlPlanLogRequestExecutor extends OlRequestExecutorBase implements Serializable {
    public static final Logger LOG = Logger.getLogger(OlPlanLogRequestExecutor.class.getName());

    private final static OlLogOptions DEFAULT_OPTIONS = new OlLogOptions();

    static {
        DEFAULT_OPTIONS.setMultiNight(false);
        DEFAULT_OPTIONS.setShowEmpties(false);
        DEFAULT_OPTIONS.setLimitConfigDatesByNight(null);
    }

    /**
     * This private class encapsulates the algorithm for handling the pre-processing of the observation list.
     * It allows the executor to see the original list, the final list, and the most recent observation.
     */
    private static class ObservationListHandler {
        private List<SPObservationID> _originalList;
        private List<SPObservationID> _finalList;
        private SPObservationID _current;

        ObservationListHandler(List<SPObservationID> observationIDs) {
            if (observationIDs == null) throw new NullPointerException("Original observation list is null");
            _originalList = new ArrayList<SPObservationID>(observationIDs);
            int size = _originalList.size();
            _current = (size > 0) ? _originalList.get(size - 1) : null;
        }

        List<SPObservationID> getOriginalList() {
            return _originalList;
        }

        int getOriginalListSize() {
            return getOriginalList().size();
        }

        List<SPObservationID> getFinalList() {
            if (_finalList == null) {
                _finalList = new ArrayList<SPObservationID>();
            }
            return _finalList;
        }

        int getFinalListSize() {
            return getFinalList().size();
        }

        SPObservationID getCurrent() {
            return _current;
        }

        /**
         * This method is a temporary patch to remove adjacent duplicates in the observation list in the nightly plan
         * caused by observers pausing and continuing which causes multiple startSequence entries in the nightly plan.
         * So only the first should be kept.
         *
         * @return <String> list of observations with adjacent duplicates removed
         */
        public List<SPObservationID> apply() {
            int size = _originalList.size();
            if (size == 0) {
                return getFinalList();
            }

            // Copy it, sort it, remove duplicates
            Set<SPObservationID> idSet = new LinkedHashSet<SPObservationID>(_originalList);
            // Simply returns the ordered set as a list
            _finalList = new ArrayList<SPObservationID>(idSet);
            return _finalList;
        }

    }

    private List<WeatherInfo> _weatherInfo;

    private SPProgramID _planID;

    /**
     * Executor will use a plan to generate a list of observations and then provide data
     * for an Observing Log.
     *
     * @param manager indicates an implementation of <tt>OlPersistenceManager</tt>
     * @param config the log configuration file structure
     * @param planID  this is a {@link edu.gemini.spModel.core.SPProgramID} object.
     */
    public OlPlanLogRequestExecutor(OlPersistenceManager manager, OlConfiguration config, SPProgramID planID, Set<Principal> user) {
        this(manager, config, DEFAULT_OPTIONS, planID, user);
    }

    /**
     * Executor will use a plan to generate a list of observations.  This one allows setting the options.
     *
     * @param manager indicates an implementation of <tt>OlPersistenceManager</tt>
     * @param config the log configuration structure
     * @param options options set by the caller
     * @param planID  this is a {@link edu.gemini.spModel.core.SPProgramID} object.
     */
    public OlPlanLogRequestExecutor(OlPersistenceManager manager, OlConfiguration config, OlLogOptions options, SPProgramID planID, Set<Principal> user) {
        super(manager, config, user);
        _planID = planID;
        _setObsLogOptions(options);
    }

    private void _setOptions(SPProgramID planID) {
        OlLogOptions options = new OlLogOptions();
        options.setOptions(DEFAULT_OPTIONS);

        Option<GemPlanId> opt = GemPlanId.parse(planID);
        if (!None.instance().equals(opt)) {
            GemPlanId gemPlanId = opt.getValue();

            Site desc = gemPlanId.getSite().getSiteDesc();
            Date date = gemPlanId.getDate();

            ObservingNight on = new ObservingNight(desc, date.getTime());
            options.setLimitConfigDatesByNight(on);
        }

        _setObsLogOptions(options);
    }


    /**
     * Construct the observing log given the set of observation ids passed with the constructor.
     *
     * @throws edu.gemini.obslog.obslog.OlLogException if an error occurs while constructing the observing log
     *                        if an improper observation ID is passed
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

        ObservationListHandler _obsListHandler = new ObservationListHandler(observationIDs);
        _obsListHandler.apply();

        _setOptions(planID);

        // SPObservationID current = _obsListHandler.getCurrent();
        //LOG.log(Level.INFO, "Start list size: " + _obsListHandler.getOriginalListSize());
        //LOG.log(Level.INFO, "Final list size: " + _obsListHandler.getFinalListSize());

        _weatherInfo = obsLogObj.getWeatherLog();

        OlLogInformation logInformation = obsLog.getLogInformation();
        logInformation.setNightObservers(obsLogObj.getNightObservers());
        logInformation.setSsas(obsLogObj.getSSA());
        logInformation.setDataproc(obsLogObj.getDataProc());
        logInformation.setDayobserver(obsLogObj.getDayObservers());
        logInformation.setFilePrefix(obsLogObj.getFilePrefix());
        logInformation.setNightComment(obsLogObj.getNightComment());
        logInformation.setCCVersion(obsLogObj.getCCSoftwareVersion());
        logInformation.setDCVersion(obsLogObj.getDCSoftwareVersion());
        logInformation.setSoftwareComment(obsLogObj.getSoftwareVersionNote());
        // Redundant at this time
        obsLog.setLogInformation(logInformation);

        // At this point, we have an unordered, raw list of SPObservationIDs that should be made into an observing log
        List<EObslogVisit> obsDataList = _fetchObservationData(_obsListHandler.getFinalList());
        if (obsDataList == null) {
            throw new OlLogException("plan returned no data");
        }

        _buildSegments(obsLog, obsDataList);
    }

    protected void _buildSegments(OlDefaultObservingLog obsLog, List<EObslogVisit> observationDataList) throws OlLogException {
        super._buildSegments(obsLog, observationDataList);

        if (_weatherInfo != null) {
            OlObsLogData weatherLogData = _getLogConfiguration().getDataForLogByType(WeatherSegment.SEG_TYPE.getType());
            if (weatherLogData != null) {
                WeatherSegment weather = new WeatherSegment(weatherLogData.getLogTableData(), _weatherInfo);
                obsLog.addWeatherSegment(weather);
            }
        }
    }


}
