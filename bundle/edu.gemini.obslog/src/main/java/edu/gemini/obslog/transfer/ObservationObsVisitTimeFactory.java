package edu.gemini.obslog.transfer;


import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.pot.sp.Instrument;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.ObservingNight;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetRecord;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obslog.ObsLog;
import edu.gemini.spModel.obsrecord.ObsVisit;
import edu.gemini.spModel.time.ObsTimeCharges;
import edu.gemini.spModel.util.SPTreeUtil;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: ObservationObsVisitTimeFactory.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

/**
 * The low-level data structure is a List of Maps, one Map for each cluster of obslog items in the observation.
 * getRows returns a List of "clusters" which are Maps of items that are the same for some datasets.
 * <p/>
 * The ObservationConfigFactory is used to extract the data from an observation sequence and obs components
 * to build the observing log.  One ObservationData/observation.
 */
public final class ObservationObsVisitTimeFactory implements Serializable {

    private static final Logger LOG = Logger.getLogger(ObservationObsVisitTimeFactory.class.getName());

    // Object created at class load
    static private final ObservationObsVisitTimeFactory _INSTANCE = new ObservationObsVisitTimeFactory();

    private OlLogOptions _obsLogOptions;

    private ObservationObsVisitTimeFactory() {
        _obsLogOptions = new OlLogOptions();
    }

    /**
     * With a {@link edu.gemini.spModel.config.IConfigBuilder} build the data for this observation.
     *
     * @param obs the {@link edu.gemini.pot.sp.ISPObservation} to be used
     * @return An instance of this class initialized for the observation
     * @throws java.rmi.RemoteException thrown when the database is not available
     */
    static public List<EChargeObslogVisit> build(ISPObservation obs, OlLogOptions obsLogOptions)  {
        if (obs == null) throw new NullPointerException("null observation");
        if (obsLogOptions == null) throw new NullPointerException("null obs log options");

        _INSTANCE._obsLogOptions.setOptions(obsLogOptions);

        ISPObsComponent inst = SPTreeUtil.findInstrument(obs);
        if (inst == null) {
            LOG.fine("Giving up: no instrument component in: " + obs.getObservationID().toString());
            return Collections.emptyList();
        }
        // Not sure what to do if there isn't an instrument.
        SPComponentType type = inst.getType();

        ObsClass obsClass = ObsClassService.lookupObsClass(obs);
        ObsLog obsLog = ObsLog.getIfExists(obs);
        if (obsLog == null) {
            LOG.fine("No Observing Log component in: " + obs.getObservationID().toString());
            // At this point, we aren't doing anything if there is no observing log/datastore
            return Collections.emptyList();
        }

        return _INSTANCE._buildSequence(type, obsClass, obsLog);
    }

    /**
     * The iterator that builds the data
     */
    private List<EChargeObslogVisit> _buildSequence(SPComponentType type, ObsClass obsClass, ObsLog obsLog) {
        List<EChargeObslogVisit> obslogVisits = new ArrayList<EChargeObslogVisit>();
        OlLogOptions options = _obsLogOptions;

        final Option<Instrument> inst = Instrument.fromComponentType(type);
        ObsVisit[] visits;
        if (options.getLimitConfigDatesByNight() == null) {
            visits = obsLog.getVisits(inst);
        } else {
            ObservingNight night = options.getLimitConfigDatesByNight();
            visits = obsLog.getVisits(inst, night.getStartTime(), night.getEndTime());
        }

        for (ObsVisit visit : visits) {
            if (visit.getAllDatasetLabels().length == 0) {
                LOG.fine("Skipping empty visit");
                continue;
            }

            EChargeObslogVisit evisit = _forOneVisit(type, obsClass, obsLog, visit);
            obslogVisits.add(evisit);
        }

        return obslogVisits;
    }

    private EChargeObslogVisit _forOneVisit(SPComponentType type, ObsClass obsClass, ObsLog obsLog, ObsVisit visit) {

        // Add one EObsLogVisit for each config
        List<DatasetRecord> dsets = new ArrayList<DatasetRecord>();
        for (DatasetLabel dlabel : visit.getAllDatasetLabels()) {
            // look record up in the obsrecord
            DatasetRecord drecord = obsLog.getDatasetRecord(dlabel);
            dsets.add(drecord);
        }

        ObsTimeCharges charges = visit.getTimeCharges(obsClass.getDefaultChargeClass());

        // Create an EObslogVisit with the obslog needed info
        return new EChargeObslogVisit(type, obsClass, visit, dsets, charges);
    }
}


