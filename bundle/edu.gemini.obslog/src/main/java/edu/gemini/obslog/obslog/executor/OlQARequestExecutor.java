package edu.gemini.obslog.obslog.executor;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.config.model.OlObsLogData;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.database.OlPersistenceManager;
import edu.gemini.obslog.instruments.QASegment;
import edu.gemini.obslog.obslog.OlDefaultObservingLog;
import edu.gemini.obslog.obslog.OlLogException;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.transfer.EObslogVisit;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.skycalc.ObservingNight;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.dataset.DatasetLabel;

import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.Serializable;
import java.util.List;

//
// Gemini Observatory/AURA
// $Id: OlQARequestExecutor.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public class OlQARequestExecutor extends OlRequestExecutorBase implements Serializable {
    public static final Logger LOG = Logger.getLogger(OlQARequestExecutor.class.getName());

    private final static OlLogOptions DEFAULT_OPTIONS = new OlLogOptions();

    static {
        DEFAULT_OPTIONS.setMultiNight(false);
        DEFAULT_OPTIONS.setShowEmpties(false);
        DEFAULT_OPTIONS.setLimitConfigDatesByNight(null);
    }

    private List<SPObservationID> _observationIDs;
    // This filter is used to limit the set of included datasets, if null, all are used
    private QASegment.QASegmentFilter _filter;

    /**
     * Executor will use a plan to generate a list of observations and then provide data
     * for an Observing Log.
     *
     * @param manager indicates an implementation of <tt>OlPersistenceManager</tt>
     */
    public OlQARequestExecutor(OlPersistenceManager manager, OlConfiguration config, List<SPObservationID> obsIDs, Set<Principal> user) {
        this(manager, config, DEFAULT_OPTIONS, obsIDs, user);
    }

    /**
     * Executor will use a plan to generate a list of observations.  This one allows setting the options.
     *
     * @param manager indicates an implementation of <tt>OlPersistenceManager</tt>
     * @param config
     * @param options
     */
    public OlQARequestExecutor(OlPersistenceManager manager, OlConfiguration config, OlLogOptions options, List<SPObservationID> observationIDs, Set<Principal> user) {
        super(manager, config, user);
        _observationIDs = observationIDs;
        _setObsLogOptions(options);
    }

    public OlQARequestExecutor(OlPersistenceManager manager, OlConfiguration config, List<SPObservationID> observationIDs, List<DatasetLabel> filter, Set<Principal> user) {
        super(manager, config, user);
        if (filter == null) throw new NullPointerException("filter == null");
        _observationIDs = observationIDs;
        _setObsLogOptions(DEFAULT_OPTIONS);
        _filter = new QASegment.QASegmentFilter(filter);
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
     * Construct the observing log given the set of observation ids passed with the constructor.
     *
     * @throws edu.gemini.obslog.obslog.OlLogException
     *          if an error occurs while constructing the observing log
     *          if an improper observation ID is passed
     */
    public void execute() throws OlLogException {
        // This is needed first to setup the observation list
        OlDefaultObservingLog obsLog = _getObservingLog();

        if (_observationIDs == null) throw new NullPointerException("observation list is null");

        // At this point, we have an unordered, raw list of SPObservationIDs that should be made into an observing log
        List<EObslogVisit> obsDataList = _fetchObservationData(_observationIDs);
        if (obsDataList == null) {
            throw new OlLogException("plan returned no data");
        }

        _buildSegments(obsLog, obsDataList);
    }

    protected void _buildSegments(OlDefaultObservingLog obsLog, List<EObslogVisit> transferDataList) throws OlLogException {
        // Build one segment
        //super._buildSegments(obsLog, observationDataList);
        int obsCount = transferDataList.size();
        if (obsCount == 0) return;

        OlSegmentType type = QASegment.SEG_TYPE;
        OlObsLogData logData = _getLogConfiguration().getDataForLogByType(type.getType());
        if (logData == null) {
            throw new OlLogException("No configuration data for instrument: " + type.toString());
        }

        QASegment segment = new QASegment(logData.getLogTableData(), _getObsLogOptions(), _filter);
        for (int i = 0; i < obsCount; i++) {
            EObslogVisit obsData = transferDataList.get(i);
            segment.addObservationData(obsData);
        }
        obsLog.addLogSegment(segment);
    }

}
