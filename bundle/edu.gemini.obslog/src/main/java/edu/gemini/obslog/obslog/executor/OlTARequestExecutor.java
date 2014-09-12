package edu.gemini.obslog.obslog.executor;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.config.model.OlObsLogData;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.database.OlPersistenceException;
import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.obslog.instruments.TASegment;
import edu.gemini.obslog.obslog.OlDefaultObservingLog;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.obslog.functor.OlObsVisitTimeFunctor;
import edu.gemini.obslog.transfer.EChargeObslogVisit;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.skycalc.ObservingNight;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.plan.NightlyRecord;

import java.security.Principal;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

//
// Gemini Observatory/AURA
// $Id: OlTARequestExecutor.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public class OlTARequestExecutor extends OlRequestExecutorBase implements Serializable {
    public static final Logger LOG = Logger.getLogger(OlTARequestExecutor.class.getName());

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

    private SPProgramID _planID;

    /**
     * Executor will use a plan to generate a list of observations and then provide data
     * for an Observing Log.
     *
     * @param manager indicates an implementation of <tt>OlPersistenceManager</tt>
     * @param planID  this is a {@link edu.gemini.spModel.core.SPProgramID} object.
     */
    public OlTARequestExecutor(OlPersistenceManager manager, OlConfiguration config, SPProgramID planID, Set<Principal> user) {
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
    public OlTARequestExecutor(OlPersistenceManager manager, OlConfiguration config, OlLogOptions options, SPProgramID planID, Set<Principal> user) {
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
        Site site = Site.GN;
        if (planIdStr.startsWith("gs-")) {
            site = Site.GS;
        }

        try {
            options.setLimitConfigDatesByNight(ObservingNight.parseNightString(date, site));
        } catch (java.text.ParseException ex) {
            LOG.log(Level.WARNING, "Failed to set data option:" + planID);
        }
        _setObsLogOptions(options);
    }

      /**
     * This private method takes a list of {@link edu.gemini.pot.sp.ISPObservation} instances and constructs
     * their ObservationData objects.
     *
     * @param observationIDs
     * @return a new <tt>List</tt> of <tt>ObservationData</tt> objects.
     * @throws OlLogException
     */
    private List<EChargeObslogVisit> fetchObservationData(List<SPObservationID> observationIDs) throws OlLogException {
        IDBDatabaseService db = _getPersistenceManager().getDatabase();
        OlObsVisitTimeFunctor lf = new OlObsVisitTimeFunctor(_getObsLogOptions(), observationIDs);
        List<EChargeObslogVisit> result;
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

        // At this point, we have an unordered, raw list of SPObservationIDs that should be made into an observing log
        List<EChargeObslogVisit> obsDataList = fetchObservationData(_obsListHandler.getFinalList());
        if (obsDataList == null) {
            throw new OlLogException("plan returned no data");
        }

        _buildSegments(obsLog, obsDataList);
    }

    protected void _buildSegments(OlDefaultObservingLog obsLog, List<EChargeObslogVisit> transferDataList) throws OlLogException {
        int obsCount = transferDataList.size();
        if (obsCount == 0) return;

        OlSegmentType type = TASegment.SEG_TYPE;
        OlObsLogData logData = _getLogConfiguration().getDataForLogByType(type.getType());
        if (logData == null) {
            throw new OlLogException("No configuration data for instrument: " + type.toString());
        }

        TASegment segment = new TASegment(logData.getLogTableData(), _getObsLogOptions());
        for (int i = 0; i < obsCount; i++) {
            EChargeObslogVisit obsData = transferDataList.get(i);
            segment.addObservationData(obsData);
        }
        // This  is somewhat unorthadox.  Adding the gaps requires access to the complete list of visits and maps so
        // calculations can be performed over the entire list
        segment.completed(transferDataList);
        obsLog.addLogSegment(segment);
    }


}
