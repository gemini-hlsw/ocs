package edu.gemini.obslog.transfer;


import edu.gemini.pot.sp.Instrument;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetRecord;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obslog.ObsLog;
import edu.gemini.spModel.obsrecord.ObsVisit;
import edu.gemini.spModel.obsrecord.UniqueConfig;
import edu.gemini.spModel.util.SPTreeUtil;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: ObsRecordDatasetFactory.java,v 1.3 2005/12/11 15:54:15 gillies Exp $
//

/**
 * The low-level data structure is a List of Maps, one Map for each cluster of obslog items in the observation.
 * getRows returns a List of "clusters" which are Maps of items that are the same for some datasets.
 * <p/>
 * This factory is used to produce the information for an observation and all of its datasets.  There is one
 * <code>UniqueConfig</code> for each step in the sequence..
 */
public final class ObsRecordDatasetFactory implements Serializable {

    private static final Logger LOG = Logger.getLogger(ObsRecordDatasetFactory.class.getName());

    // Object created at class load
    static private final ObsRecordDatasetFactory _INSTANCE = new ObsRecordDatasetFactory();

    private ObsRecordDatasetFactory() {
    }

    /**
     * With a {@link edu.gemini.spModel.config.IConfigBuilder} build the data for this observation.
     *
     * @param obs the {@link edu.gemini.pot.sp.ISPObservation} to be used
     * @return An instance of this class initialized for the observation
     * @throws java.rmi.RemoteException thrown when the database is not available
     */
    static public List<EObslogVisit> build(ISPObservation obs)  {
        if (obs == null) throw new NullPointerException("ObservationData: Null observation");

        ISPObsComponent inst = SPTreeUtil.findInstrument(obs);
        if (inst == null) {
            LOG.info("Giving up: no instrument component in: " + obs.getObservationID().toString());
            return Collections.emptyList();
        }

        // Not sure what to do if there isn't an instrument.
        SPComponentType type = inst.getType();

        // Get the obsClass
        ObsClass obsClass = ObsClassService.lookupObsClass(obs);

        // Check for an observing log component
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
    private List<EObslogVisit> _buildSequence(SPComponentType type, ObsClass obsClass, ObsLog obsLog) {

        List<EObslogVisit> eObslogVisits = new ArrayList<EObslogVisit>();

        for (ObsVisit visit : obsLog.getVisits(Instrument.fromComponentType(type), obsClass)) {
            if (visit.getAllDatasetLabels().length == 0) {
                LOG.fine("Skipping  empty visit");
                continue;
            }
            List<EObslogVisit> rows = _buildOneVisit(type, obsClass, obsLog, visit);
            eObslogVisits.addAll(rows);
        }

        return eObslogVisits;
    }

    private List<EObslogVisit> _buildOneVisit(SPComponentType type, ObsClass obsClass, ObsLog obsLog, ObsVisit visit) {
        List<EObslogVisit> eObslogVisits = new ArrayList<EObslogVisit>();

        // Loop over all configs in the ObsVisit, then unroll and add one EObsLogVisit for each dataset
        for (int cindex = 0, csize = visit.getUniqueConfigs().length; cindex < csize; cindex++) {
            UniqueConfig uc = visit.getUniqueConfigs()[cindex];

            for (DatasetLabel dlabel : uc.getDatasetLabels()) {
                // look record up in the obsrecord
                List<DatasetRecord> dsets = new ArrayList<DatasetRecord>(1);
                DatasetRecord drecord = obsLog.getDatasetRecord(dlabel);
                dsets.add(drecord);

                // Create an EObslogVisit with the obslog needed info
                EObslogVisit evisit = new EObslogVisit(type, obsClass, uc, dsets);
                eObslogVisits.add(evisit);
            }
        }
        return eObslogVisits;
    }

}


