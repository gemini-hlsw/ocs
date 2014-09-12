//
// $Id: DsetRecordFactory.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.datasetrecord.impl.store;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.spModel.config.ConfigBridge;
import edu.gemini.spModel.config.map.ConfigValMapInstances;
import edu.gemini.spModel.config.map.ConfigValMapUtil;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.DefaultConfig;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.dataset.Dataset;
import edu.gemini.spModel.dataset.DatasetExecRecord;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.obsrecord.ObsExecRecord;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility used to create dataset records and add them to the corresponding
 * obs record.
 */
final class DsetRecordFactory {
    private static final Logger LOG = Logger.getLogger(DsetRecordFactory.class.getName());

    /**
     * The key that identifies the data label item of a
     * {@link edu.gemini.spModel.config2.Config}.
     */
    public static final ItemKey DATALABEL_KEY = new ItemKey("observe:dataLabel");

    private ISPObservation _obs;
    private ObsExecRecord _obsExecRecord;
    private ConfigSequence _sequence;

    DsetRecordFactory(ISPObservation obs, ObsExecRecord obsRec) {
        _obs       = obs;
        _obsExecRecord = obsRec;
    }

    ConfigSequence getConfigSequence() {
        if (_sequence == null) _sequence = ConfigBridge.extractSequence(_obs, null, ConfigValMapInstances.IDENTITY_MAP);
        return _sequence;
    }

    private Config _deriveConfig(DatasetLabel label) {
        long start = System.currentTimeMillis();
        Config tmpl = new DefaultConfig();
        tmpl.putItem(DATALABEL_KEY, label.toString());
        Config config = getConfigSequence().match(tmpl);
        long end = System.currentTimeMillis();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Derived config for "+ label +" in "+ (end-start) +" ms");
        }

        // Sometimes there are dataset labels that don't correspond to real
        // steps in the sequence, in which case there is no matching config
        // and config is null.  In that case we still can add a dataset
        // record but it just won't have a valid config.
        return (config == null) ? null : ConfigValMapUtil.mapValues(config, ConfigValMapInstances.TO_DISPLAY_VALUE);
    }

    /**
     * Creates the DatasetRecord based upon the update request and adds it to
     * the ObsRecord for the corresponding observation.  This method is intended
     * to be called from a functor (particularly the
     * {@link ObsRecordUpdateFunctor}, so RemoteExceptions are not anticipated
     * but are required by the ODB API.
     */
    DatasetExecRecord createRecord(DsetRecordUpdateRequest dsetReq) {

        DatasetLabel label = dsetReq.getLabel();

        Dataset dset = dsetReq.getDataset();
        if (dset == null) {
            LOG.warning("Cannot create record for label " + label +
                        ", missing dataset information");
            return null; // not enough info to create record
        }

        Config config = _deriveConfig(label);
        DatasetExecRecord rec = new DatasetExecRecord(dset);
        _obsExecRecord.putDatasetExecRecord(rec, config);
        return rec;
    }
}
